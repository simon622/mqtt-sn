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

package org.slj.mqtt.sn.gateway.impl.broker;

import org.slj.mqtt.sn.gateway.spi.broker.IMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.MqttsnException;

import java.util.logging.Level;

/**
 * A single broker connection is maintained and used for all connecting gateway side
 * devices
 */
public class MqttsnAggregatingBrokerService extends AbstractMqttsnBrokerService {

    volatile IMqttsnBrokerConnection connection;
    volatile boolean stopped = false;

    public MqttsnAggregatingBrokerService(MqttsnBrokerOptions options){
        super(options);
    }

    @Override
    public void stop() throws MqttsnException {
        stopped = true;
        super.stop();
        try {
            close(connection);
        } catch(MqttsnBrokerException e){
            logger.log(Level.WARNING, "error encountered shutting down broker connection;", e);
        }
    }

    @Override
    protected void initThread() {
        //-- only start deamon process if we are managing the connections
        if(options.getManagedConnections()){
            super.initThread();
        }
    }

    @Override
    public boolean isConnected(IMqttsnContext context) throws MqttsnBrokerException {
        return !stopped && connection != null && connection.isConnected();
    }

    @Override
    protected long doWork() {
        try {
            if(options.getManagedConnections()){
                logger.log(Level.FINE, "checking status of managed connection..");
                if(connection != null){
                    if(!connection.isConnected()){
                        logger.log(Level.WARNING, "detected invalid connection to broker, dropping stale connection.");
                        close(connection);
                    }
                } else {
                    initConnection();
                }
            } else {
                if(options.getConnectOnStartup() && connection == null){
                    initConnection();
                }
            }
        } catch(Exception e){
            logger.log(Level.SEVERE, "error occurred monitoring connections;", e);
        }

        return 10000;
    }

    @Override
    protected IMqttsnBrokerConnection getBrokerConnectionInternal(IMqttsnContext context) throws MqttsnBrokerException {
        if(stopped) throw new MqttsnBrokerException("broker service is in the process or shutting down");
        initConnection();
        return connection;
    }

    protected void initConnection() throws MqttsnBrokerException {
        if(connection == null){
            //-- in aggregation mode connect with the gatewayId as the clientId on the broker side
            synchronized (this){
                if(connection == null){
                    connection = registry.getBrokerConnectionFactory().createConnection(options,
                            registry.getOptions().getContextId());
                    //-- bootstrap to recieve events from the connections..
                    //-- TODO - I dont like this pattern
                    if(connection instanceof AbstractMqttsnBrokerConnection){
                        ((AbstractMqttsnBrokerConnection)connection).setBrokerService(this);
                    }
                }
            }
        }
    }

    @Override
    protected void close(IMqttsnBrokerConnection connection) throws MqttsnBrokerException {
        if(connection != null && connection.isConnected()){
            connection.close();
        }
        this.connection = null;
    }

    @Override
    protected String getDaemonName() {
        return "gateway-broker-managed-connection";
    }
}
