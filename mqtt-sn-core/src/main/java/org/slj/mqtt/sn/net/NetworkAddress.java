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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * A network address simply represents a host name or IP address and port combination.
 * Represents a remote ipv4 or ipv6 location. Regardless of the supplied format, the address will
 * be stored in its canonical form.
 */
public class NetworkAddress implements Serializable {

    private final String hostAddress;
    private final int port;

    /**
     * Create a new network address from the port and address supplied.
     * @param port - The port on which the remote is bound
     * @param hostAddress - A valid ipv4, ipv6 or host address. Where a name is supplied,
     *                    an attempt will be made to eagerly resolve it so unknown hosts are derived eagerly.
     * @throws UnknownHostException - no host could be found
     */
    public NetworkAddress(int port, String hostAddress) throws UnknownHostException {
        this.hostAddress = InetAddress.getByName(hostAddress).getHostAddress();
        if(port < 0 || port > 65535) throw new IllegalArgumentException("port must be in range 0 <= port <= 65535");
        this.port = port;
    }

    public String getHostAddress() {
        return hostAddress;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetworkAddress that = (NetworkAddress) o;
        return port == that.port &&
                Objects.equals(hostAddress, that.hostAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostAddress, port);
    }

    public InetSocketAddress toSocketAddress(){
        return InetSocketAddress.createUnresolved(hostAddress, port);
    }

    public static NetworkAddress from(InetSocketAddress address) throws UnknownHostException {
        return NetworkAddress.from(address.getPort(), address.getAddress().getHostAddress());
    }

    public static NetworkAddress from(int port, String hostAddress) throws UnknownHostException {
        return new NetworkAddress(port, hostAddress);
    }

    /**
     * Convenience method to obtain a network address to local host loopback (127.0.0.1) on the port specified.
     * @param port - the local port
     * @return the NetworkAddress
     */
    public static NetworkAddress localhost(int port) {
        try {
            return new NetworkAddress(port, "127.0.0.1");
        } catch(UnknownHostException e){
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("SocketAddress [");
        sb.append("address='").append(hostAddress).append('\'');
        sb.append(", port=").append(port);
        sb.append(']');
        return sb.toString();
    }
}
