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

package org.slj.mqtt.sn.gateway.connector.google.iotcore;

import org.slj.mqtt.sn.gateway.connector.paho.PahoMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.impl.broker.AbstractMqttsnBrokerConnection;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerException;
import org.slj.mqtt.sn.gateway.spi.broker.MqttsnBrokerOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author simonjohnson
 *
 * Uses the AWS SDK (which in turn uses PAHO) to connect to the AWS IoT Core
 */
public class GoogleIoTCoreMqttsnBrokerConnection extends PahoMqttsnBrokerConnection {

    private Logger logger = Logger.getLogger(GoogleIoTCoreMqttsnBrokerConnection.class.getName());

    public GoogleIoTCoreMqttsnBrokerConnection(MqttsnBrokerOptions options, String clientId) {
        super(options, clientId);
    }


}