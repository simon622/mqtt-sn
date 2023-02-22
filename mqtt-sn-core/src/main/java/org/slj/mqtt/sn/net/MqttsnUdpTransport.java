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

import org.slj.mqtt.sn.impl.AbstractMqttsnUdpTransport;
import org.slj.mqtt.sn.model.INetworkContext;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;
import org.slj.mqtt.sn.utils.StringTable;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.Future;

/**
 * Provides a transport over User Datagram Protocol (UDP). This implementation uses a receiver thread which binds
 * onto a Socket in a tight loop blocking on receive with no socket timeout set (0). The receiver thread will simply hand packets
 * off to the base receive method who will either pass to the thread pool for handling or handle blocking depending on the
 * configuration of the runtime.
 *
 * The broadcast-receiver when activated runs on it own thread, listening on the broadcast port, updating the registry
 * when new contexts are discovered.
 */
public class MqttsnUdpTransport extends AbstractMqttsnUdpTransport {

    private DatagramSocket socket;
    private DatagramSocket broadcastSocket;
    private Thread receiverThread;
    private Thread broadcastThread;

    protected volatile boolean running = false;

    public MqttsnUdpTransport(MqttsnUdpOptions udpOptions){
        super(udpOptions);
    }

    protected synchronized void bind() throws SocketException {

        running = true;
        int bufferSize = options.getReceiveBuffer();
        socket = options.getPort() > 0 ? new DatagramSocket(options.getPort()) : new DatagramSocket();
        //-- by default we do not set SoTimeout (infinite) which will block until recieve
        receiverThread = createDatagramServer("mqtt-sn-udp-receiver", bufferSize, socket);
        if(options.getBindBroadcastListener() && registry.getOptions().isEnableDiscovery()) {
            broadcastSocket = options.getBroadcastPort() > 0 ? new DatagramSocket(options.getBroadcastPort()) : new DatagramSocket();
            broadcastSocket.setBroadcast(true);
            broadcastThread = createDatagramServer("mqtt-sn-udp-broadcast", bufferSize, broadcastSocket);
        }
    }

    protected Thread createDatagramServer(final String threadName, final int bufSize, final DatagramSocket socketIn){
        Thread thread = new Thread(() -> {
            logger.info("mqtt-sn udp {} creating udp server {} bound to socket {} with buffer size {}, running ? {}",
                    registry.getOptions().getContextId(), threadName, socketIn.getLocalPort(), bufSize, running);
            byte[] buff = new byte[bufSize];
            while(running && !socketIn.isClosed() &&
                    !Thread.currentThread().isInterrupted()){
                try {
                    DatagramPacket p = new DatagramPacket(buff, buff.length);
                    socketIn.receive(p);
                    int length = p.getLength();

                    logger.debug("receiving {} byte Datagram, offset = {}, data = {}",
                            length, p.getOffset(), p.getData().length);

                    NetworkAddress address = NetworkAddress.from(p.getPort(), p.getAddress().getHostAddress());
                    INetworkContext context = registry.getNetworkRegistry().getContext(address);
                    if(context == null){
                        //-- if the network context does not exist in the registry, a new one is created by the factory -
                        //- NB: this is NOT auth, this is simply creating a context to which we can respond, auth can
                        //-- happen during the mqtt-sn context creation, at which point we can talk back to the device
                        //-- with error packets and the like
                        context = registry.getContextFactory().createInitialNetworkContext(MqttsnUdpTransport.this, address);
                    }

                    if(socketIn != null){
                        context.setReceivePort(socketIn.getLocalPort());
                        receiveDatagramInternal(context, p);
                    }
                }
                catch(SocketException e){
                    logger.warn("socket error, i/o channels closed;", e);
                }
                catch(Throwable e){
                    logger.error("uncaught exception listening for datagrams", e);
                } finally {
                    buff = new byte[bufSize];
                }
            }

            logger.info("mqtt-sn udp {} stopping udp server {} bound to socket {} with buffer size {}, running ? {}",
                    registry.getOptions().getContextId(), threadName, socketIn.getLocalPort(), bufSize, running);

        }, threadName);
        thread.setDaemon(true);
        thread.setPriority(Thread.MIN_PRIORITY + 1);
        thread.start();
        return thread;
    }

    volatile boolean stopping = false;
    @Override
    public void stop() throws MqttsnException {

        stopping = true;
        if(running && !stopping){
            super.stop();

            long socketTime = System.currentTimeMillis();
            if(socket != null &&
                    socket.isConnected()){
                socket.close();
            }
            socket = null;

            long interruptTime = System.currentTimeMillis();
            if(receiverThread != null){
                receiverThread.interrupt();
            }

            logger.info("stopped udp transport in socket.close={}ms, interrupt={}ms", System.currentTimeMillis() - socketTime,
                    System.currentTimeMillis() - interruptTime);
            broadcastThread = null;
            stopping = false;
        }
    }

    @Override
    protected void writeToTransportInternal(INetworkContext context, byte[] data) {
        try {
            DatagramPacket packet = new DatagramPacket(data, data.length);
            sendDatagramInternal(context, packet);
        } catch(Exception e){
            throw new MqttsnRuntimeException(e);
        }
    }

    protected void receiveDatagramInternal(INetworkContext context, DatagramPacket packet) {
        ByteBuffer bb = wrap(packet.getData(), packet.getLength());
        receiveFromTransport(context, drain(bb));
    }

    protected void sendDatagramInternal(INetworkContext context, DatagramPacket packet) throws IOException {
        if(!running){
            logger.warn("transport is NOT RUNNING trying to send {} byte Datagram to {}",
                    packet.getLength(), context);
            return;
        }
        NetworkAddress address = context.getNetworkAddress();
        InetAddress inetAddress = InetAddress.getByName(address.getHostAddress());
        packet.setAddress(inetAddress);
        packet.setPort(address.getPort());
        logger.debug("sending {} byte Datagram to {} -> {}",
                    packet.getLength(), address, address.getPort());
        send(packet);
    }

    protected void send(DatagramPacket packet) throws IOException {
        if(socket != null && !socket.isClosed()){
            socket.send(packet);
        }
    }

    @Override
    public void broadcast(IMqttsnMessage broadcastMessage) throws MqttsnException {
        try {
            byte[] arr = registry.getCodec().encode(broadcastMessage);
            List<InetAddress> broadcastAddresses = registry.getNetworkRegistry().getAllBroadcastAddresses();
            try (DatagramSocket socket = new DatagramSocket()){
                socket.setBroadcast(true);
                for(InetAddress address : broadcastAddresses) {
                    logger.debug("broadcasting {} message to network interface {} -> {}",
                            broadcastMessage.getMessageName(), address, options.getBroadcastPort());
                    DatagramPacket packet
                            = new DatagramPacket(arr, arr.length, address, options.getBroadcastPort());
                    socket.send(packet);
                }
            }
        } catch(Exception e){
            throw new MqttsnException(e);
        }
    }

    @Override
    public StringTable getTransportDetails() {
        StringTable st = new StringTable("Property", "Value");
        st.setTableName("UDP Transport");
        st.addRow("Host", options.getHost());
        st.addRow("Datagram port", options.getPort());
        st.addRow("Secure port", options.getSecurePort());
        st.addRow("Broadcast port", options.getBroadcastPort());
        st.addRow("MTU", options.getMtu());
        return st;
    }
}
