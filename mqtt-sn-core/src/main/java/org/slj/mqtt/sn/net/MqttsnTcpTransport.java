/*
 * Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
 *
 * Find me on GitHub:
 * https://github.com/simon622
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.slj.mqtt.sn.net;

import org.slj.mqtt.sn.impl.AbstractMqttsnTransport;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.StringTable;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP IP implementation support. Can be run with SSL (TLS) enabled for secure communication.
 * Supports running in both client and server mode.
 */
public class MqttsnTcpTransport
        extends AbstractMqttsnTransport {

    protected final MqttsnTcpOptions options;
    protected boolean clientMode = false;

    static AtomicInteger connectionCount = new AtomicInteger(0);
    private final Object monitor = new Object();
    protected Handler clientHandler;
    protected Server server;

    public MqttsnTcpTransport(MqttsnTcpOptions options, boolean clientMode) {
        this.options = options;
        this.clientMode = clientMode;
    }

    @Override
    public synchronized void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        try {
            super.start(runtime);
            running = false;
            if(clientMode){
                logger.info("running in client mode, establishing tcp connection...");
                connectClient();
            } else {
                logger.info("running in server mode, establishing tcp acceptors...");
                startServer();
            }
            running = true;
            synchronized (monitor){
                monitor.notifyAll();
            }
        } catch(Exception e){
            running = false;
            throw new MqttsnException(e);
        }
    }

    @Override
    public synchronized void stop() throws MqttsnException {
        super.stop();
        try {
            if(clientMode){
                logger.info("closing in client mode, closing tcp connection(s)...");
                try {
                    if(clientHandler != null){
                        clientHandler.close();
                    }
                } finally {
                    clientHandler = null;
                }
            } else {
                logger.info("closing in server mode, closing tcp connection(s)...");
                try {
                    if(server != null){
                        server.close();
                    }
                } finally {
                    server = null;
                }
            }
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    @Override
    protected void writeToTransportInternal(INetworkContext context, byte[] data) {
        try {
            if(clientMode){
                if(clientHandler == null){
                    //-- in edge cases, we may get called by other services when still connecting..
                    //-- so defend against that
                    synchronized (monitor){
                        logger.warn("waiting for TCP stack to come up...");
                        monitor.wait(options.getConnectTimeout() + 1000);
                    }
                }
                if(clientHandler != null){
                    clientHandler.write(data);
                }

            } else {
                if(server != null){
                    server.write(context, data);
                }
            }
        } catch(IOException | InterruptedException e){
            throw new MqttsnRuntimeException("error writing to connection;", e);
        }
    }

    protected SSLContext initSSLContext()
            throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException, UnrecoverableKeyException, KeyManagementException {

        String keyStorePath = options.getKeyStorePath();
        String trustStorePath = options.getTrustStorePath();
        logger.info("initting SSL context with keystore {}, truststore {}", keyStorePath, trustStorePath);

        //-- keystore
        KeyStore keyStore = null;
        KeyManager[] keyManagers = null;
        if(keyStorePath != null){
            File f = new File(keyStorePath);
            if(!f.exists() || !f.canRead())
                throw new KeyStoreException("unable to read keyStore " + keyStorePath);

            keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            KeyManagerFactory kmf = KeyManagerFactory
                    .getInstance(KeyManagerFactory.getDefaultAlgorithm());

            try(InputStream keyStoreData = new FileInputStream(f)) {
                char[] password = options.getKeyStorePassword().toCharArray();
                keyStore.load(keyStoreData, password);
                Enumeration<String> aliases = keyStore.aliases();
                while(aliases.hasMoreElements()){
                    String el = aliases.nextElement();
                    logger.info("keystore contains alias {}", el);
                }
                kmf.init(keyStore, password);
                keyManagers = kmf.getKeyManagers();
            }
        }

        TrustManager[] trustManagers = null;
        if(trustStorePath == null){
            logger.warn("!! ssl operating in trust-all mode, do not use this in production, nominate a trust store !!");
            TrustManager trustAllCerts = new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
                public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
                public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
                }
            };
            trustManagers = new TrustManager[] { trustAllCerts };
        } else {
            File f = new File(trustStorePath);
            if(!f.exists() || !f.canRead())
                throw new KeyStoreException("unable to read trustStore " + trustStorePath);

            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            try(InputStream trustStoreData = new FileInputStream(f)) {
                char[] password = options.getTrustStorePassword().toCharArray();
                trustStore.load(trustStoreData, password);
                Enumeration<String> aliases = keyStore.aliases();
                while(aliases.hasMoreElements()){
                    String el = aliases.nextElement();
                    logger.info("truststore contains alias {}", el);
                }
                TrustManagerFactory tmf = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(keyStore);
                trustManagers = tmf.getTrustManagers();
            }
        }
        SSLContext ctx = SSLContext.getInstance(options.getSslAlgorithm());
        logger.info("ssl initialised with algo {}", ctx.getProtocol());
        logger.info("ssl initialised with JCSE provider {}", ctx.getProvider());
        logger.info("ssl initialised with {} key manager(s)", keyManagers == null ? null : keyManagers.length);
        logger.info("ssl initialised with {} trust manager(s)", trustManagers == null ? null : trustManagers.length);
        ctx.init(keyManagers, trustManagers, new SecureRandom());
//                SecureRandom.getInstanceStrong());
        return ctx;

    }

    protected void connectClient() throws MqttsnException {
        try {
            //-- check we have a valid network location to connect to
            Optional<INetworkContext> remoteAddress = registry.getNetworkRegistry().first();
            if(!remoteAddress.isPresent()) throw new MqttsnException("need a remote location to connect to found <null>");
            INetworkContext remoteContext = remoteAddress.get();

            //-- create and bind local socket
            if(clientHandler == null){
                synchronized (this){
                    if(clientHandler == null){
                        Socket clientSocket;
                        if(options.isSecure()){
                            clientSocket = initSSLContext().getSocketFactory().createSocket();
                            SSLSocket ssl = (SSLSocket) clientSocket;
                            if(options.getSslProtocols() != null) {
                                logger.info("starting client-ssl with protocols {}",
                                        Arrays.toString(options.getSslProtocols()));
                                ssl.setEnabledProtocols(options.getSslProtocols());
                            }
                            if(options.getCipherSuites() != null){
                                logger.info("starting client-ssl with cipher suites {}",
                                        Arrays.toString(options.getCipherSuites()));
                                ssl.setEnabledCipherSuites(options.getCipherSuites());
                            }

                            logger.info("client-ssl enabled protocols {}",
                                    Arrays.toString(ssl.getEnabledProtocols()));
                            logger.info("client-ssl enabled cipher suites {}",
                                    Arrays.toString(ssl.getEnabledCipherSuites()));

                        } else {
                            clientSocket = options.getClientSocketFactory().createSocket();
                        }

                        //bind to the LOCAL address if specified, else bind to the OS default
                        InetSocketAddress localAddress = null;
                        if(options.getHost() != null){
                            localAddress = new InetSocketAddress(options.getHost(), options.getPort());
                        }

                        clientSocket.bind(localAddress);
                        clientSocket.setSoTimeout(options.getSoTimeout());
                        clientSocket.setKeepAlive(options.isTcpKeepAliveEnabled());

                        //-- if bound locally, connect to the remote if specified (running as a client)
                        if(clientSocket.isBound()){
                            InetSocketAddress r =
                                    new InetSocketAddress(remoteContext.getNetworkAddress().getHostAddress(),
                                            remoteContext.getNetworkAddress().getPort());
                            logger.info("connecting client socket to {} -> {}",
                                    remoteContext.getNetworkAddress().getHostAddress(), remoteContext.getNetworkAddress().getPort());
                            clientSocket.connect(r, options.getConnectTimeout());
                        }

                        //-- need to be connected before handshake
                        if(clientSocket instanceof SSLSocket &&
                                options.isSecure()){
                            ((SSLSocket) clientSocket).startHandshake();
                        }

                        if(clientSocket.isBound() && clientSocket.isConnected()){
                            clientHandler = new Handler(remoteContext, clientSocket, null);
                            clientHandler.start();
                        } else {
                            logger.error("could not bind and connect socket to host, finished.");
                        }
                    }
                }
            }
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    protected void startServer()
            throws IOException, KeyStoreException, NoSuchAlgorithmException, KeyManagementException,
            CertificateException, UnrecoverableKeyException {
        if(server == null){
            synchronized (this) {
                if (server == null) {
                    ServerSocket socket = null;
                    if(options.isSecure()){
                        logger.info("running in secure mode, tcp with TLS...");
                        socket = initSSLContext().getServerSocketFactory().createServerSocket(options.getSecurePort());

                        SSLServerSocket ssl = (SSLServerSocket) socket;
                        if(options.getSslProtocols() != null) {
                            logger.info("starting server-ssl with protocols {}", Arrays.toString(options.getSslProtocols()));
                            ssl.setEnabledProtocols(options.getSslProtocols());
                        }
                        if(options.getCipherSuites() != null){
                            logger.info("starting server-ssl with cipher suites {}", Arrays.toString(options.getCipherSuites()));
                            ssl.setEnabledCipherSuites(options.getCipherSuites());
                        }

                        logger.info("server-ssl enabled protocols {}", Arrays.toString(ssl.getEnabledProtocols()));
                        logger.info("server-ssl enabled cipher suites {}", Arrays.toString(ssl.getEnabledCipherSuites()));

                    } else {
                        socket = options.getServerSocketFactory().createServerSocket(options.getPort());
                    }
                    server = new Server(socket);
                    server.start();
                }
            }
        }
    }

    private class Server extends Thread implements ClosedListener{

        private final ServerSocket serverSocket;
        private Map<INetworkContext, Handler> connections =
                Collections.synchronizedMap(new HashMap<>());

        public Server(ServerSocket serverSocket){
            this.serverSocket = serverSocket;
            setPriority(Thread.MIN_PRIORITY + 1);
            setDaemon(false);
            setName("mqtt-sn-tcp-server");
        }

        public void write(INetworkContext context, byte[] data) throws IOException {
            Handler handler = connections.get(context);
            if(handler == null) throw new IOException("no connected handler for context");
            handler.write(data);
        }

        public void close(){
            if(connections != null && !connections.isEmpty()){
                connections.values().stream().forEach(c -> {
                    try {
                        c.close();
                    } catch(Exception e){
                        logger.error("error closing connection", e);
                    }
                });
            }
        }

        public void closed(Handler handler){
            if(connections != null && handler != null)
                connections.remove(handler.descriptor.context);
        }

        public void run(){
            logger.info("starting TCP listener, accepting {} connections on port {}",
                    options.getMaxClientConnections(), serverSocket.getLocalPort());
            while(running){
                try {
                    Socket socket = serverSocket.accept();
                    if(connections.size() >= options.getMaxClientConnections()){
                        logger.warn("max connection limit reached, disconnecting socket");
                        socket.close();

                    } else {
                        SocketAddress address = socket.getRemoteSocketAddress();
                        logger.info("new connection accepted from {}", address);
                        NetworkAddress networkAddress = NetworkAddress.from((InetSocketAddress) address);
                        INetworkContext context = registry.getNetworkRegistry().getContext(networkAddress);
                        if(context == null){
                            //-- if the network context does not exist in the registry, a new one is created by the factory -
                            //- NB: this is NOT auth, this is simply creating a context to which we can respond, auth can
                            //-- happen during the mqtt-sn context creation, at which point we can talk back to the device
                            //-- with error packets and the like
                            context = registry.getContextFactory().createInitialNetworkContext(MqttsnTcpTransport.this, networkAddress);
                        }
                        Handler handler = new Handler(context, socket, this);
                        connections.put(context, handler);
                        handler.start();
                    }

                } catch (NetworkRegistryException | IOException | MqttsnException e){
                    logger.error("error encountered accepting connection;", e);
                }
            }
        }
    }

    /**
     * When running in server mode the handler will accept each connection lazily (not pre-pooled)
     */
    class Handler extends Thread {

        private final SocketDescriptor descriptor;
        private final ClosedListener listener;

        public Handler(INetworkContext context, Socket socket, ClosedListener listener) throws IOException {
            this.descriptor = new SocketDescriptor(context, socket);
            this.listener = listener;
            setName("mqtt-sn-tcp-" + context.getNetworkAddress().toSimpleString() + "@" + System.identityHashCode(getRegistry().getRuntime()));
            connectionCount.incrementAndGet();
            setDaemon(false);
            setPriority(Thread.NORM_PRIORITY);
        }

        public void close() throws IOException {
            descriptor.close();
            if(listener != null) listener.closed(this);
        }

        public void write(byte[] data) throws IOException {
            //-- TODO this needs buffering in
            try {
                if(descriptor.isOpen()){
                    logger.debug("writing {} bytes to output stream", data.length);
                    descriptor.os.write(data);
                    descriptor.os.flush();
                }
            } catch(SocketException e){
                try {
                    if(descriptor != null) {
                        connectionLost(descriptor.context, e);
                    }
                } finally {
                    try {
                        descriptor.close();
                    } catch (IOException ex) {
                        logger.warn("error closing socket descriptor;", e);
                    }
                }
            }
        }

        public void run(){
            try {
                logger.info("starting socket handler for {}", descriptor);
                while(running && descriptor.isOpen()){
                    int count;
                    int messageLength = 0;
                    int messageLengthRemaining = 0;
                    byte[] buff = new byte[options.getReadBufferSize()];
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        while ((count = descriptor.is.read(buff)) > 0) {
                            boolean isNewMessage = baos.size() == 0;
                            baos.write(buff, 0, count);
                            if(isNewMessage){
                                messageLength = registry.getCodec().readMessageSize(baos.toByteArray());
                                messageLengthRemaining = messageLength;
                            }

                            messageLengthRemaining -= count;

                            if (messageLengthRemaining == 0 && baos.size() == messageLength) {
                                logger.debug("received {} bytes from socket for {}, reset buffer", messageLength, descriptor);
                                receiveFromTransport(descriptor.context, baos.toByteArray());
                                messageLength = 0;
                                messageLengthRemaining = 0;
                                baos.reset();
                            }
                        }
                    }

                    if(count == 0 || count == -1) {
                        logger.info("received {} bytes from socket handler (end of stream - EOF) on thread {}, descriptor {}", count, Thread.currentThread().getName(), descriptor);
                        //throw here will cascade the error up to the runtime
                        throw new SocketException("EOF received");
                    }
                }
            }
            catch(SSLProtocolException | SocketException e){
                //-- socket close from underneath during reading
                logger.debug("socket error", e);
                if(descriptor != null){
                    if(!descriptor.closed){
                        //it may have been that we asked for a stop and there was an issue closing the socket
                        //in which cast we didnt LOSE the connection.. only report lost if the
                        //service was meant to be running
                        connectionLost(descriptor.context, e);
                    }
                }
            }
            catch(IOException e){
                if(running && descriptor != null && descriptor.isOpen()){
                    connectionLost(descriptor.context, e);
                    throw new RuntimeException("error accepting data from client stream;",e);
                }
            } finally {
                try {
                    if(descriptor != null && descriptor.isOpen()) descriptor.close();
                } catch (IOException e) {
                    logger.warn("error closing socket descriptor;", e);
                } finally {
                    running = false;
                }
            }
        }
    }

    private class SocketDescriptor implements Closeable {

        private volatile boolean closed = false;
        private INetworkContext context;
        private Socket socket;
        private InputStream is;
        private OutputStream os;

        public SocketDescriptor(INetworkContext context, Socket socket) throws IOException {
            this.context = context;
            this.socket = socket;
            if(socket.isClosed()) throw new IllegalArgumentException("cannot get a descriptor onto closed socket");
            is = socket.getInputStream();
            os = socket.getOutputStream();
        }

        public boolean isOpen(){
            return socket != null && socket.isConnected() && !socket.isClosed();
        }

        @Override
        public String toString() {
            return Objects.toString(context);
        }

        @Override
        public synchronized void close() throws IOException {
            try {
                if(!closed){
                    int current = connectionCount.decrementAndGet();
                    logger.info("closing socket {}, activeConnection(s) {}", this, current);
                    closed = true;
                    try {is.close();} catch(Exception e){}
                    try {os.close();} catch(Exception e){}
                    try {socket.close();} catch(Exception e){}
                }
            } finally {
                socket = null;
                context = null;
                os = null;
                is = null;
            }
        }
    }

    @Override
    public void broadcast(IMqttsnMessage message) throws MqttsnException {
        throw new UnsupportedOperationException("broadcast not supported on TCP");
    }

    @Override
    public StringTable getTransportDetails() {
        StringTable st = new StringTable("Property", "Value");
        st.setTableName("TCP Transport");
        st.addRow("Host", options.getHost());
        st.addRow("TCP port", options.getPort());
        st.addRow("Secure port", options.getSecurePort());
        st.addRow("Max Connections", options.getMaxClientConnections());
        st.addRow("Connect Timeout", options.getConnectTimeout());
        st.addRow("So. Timeout", options.getSoTimeout());
        return st;
    }

    @Override
    public String getName() {
        return "mqtt-sn-tcp";
    }
}

interface ClosedListener {
    void closed(MqttsnTcpTransport.Handler handler);
}
