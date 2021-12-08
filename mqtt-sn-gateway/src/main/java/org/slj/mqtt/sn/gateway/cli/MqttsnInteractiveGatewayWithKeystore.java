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

package org.slj.mqtt.sn.gateway.cli;

import java.io.IOException;
import java.util.Properties;

public abstract class MqttsnInteractiveGatewayWithKeystore extends MqttsnInteractiveGateway {

    static final String KEYSTORE_LOCATION = "keyStoreLocation";
    static final String KEYSTORE_PASSWORD = "keyStorePassword";
    static final String KEY_PASSWORD = "keyPassword";
    static final String CERTIFICATE_LOCATION = "certificateLocation";
    static final String PRIVATEKEY_LOCATION = "privateKeyLocation";

    protected String keystoreLocation;
    protected String certificateLocation;
    protected String privateKeyLocation;
    protected String keyStorePassword;
    protected String keyPassword;


    @Override
    protected void configure() throws IOException {
        super.configure();
        keystoreLocation = captureFilePath(input, output, "Please enter the file location of your keystore (leave blank for certs & keys)");
        if(keystoreLocation != null){
            keyStorePassword = captureString(input, output,  "Please enter the keystore password");
        }
        else {
            certificateLocation = captureFilePath(input, output,  "Please enter the file location of your certificate");
            privateKeyLocation = captureFilePath(input, output,  "Please enter the file location of your private key");
        }
        keyPassword = captureMandatoryString(input, output,  "Please enter the private key password");
    }

    @Override
    protected void loadConfigHistory(Properties props) throws IOException {
        super.loadConfigHistory(props);
        keystoreLocation = props.getProperty(KEYSTORE_LOCATION);
        keyStorePassword = props.getProperty(KEYSTORE_PASSWORD);
        keyPassword = props.getProperty(KEY_PASSWORD);
        privateKeyLocation = props.getProperty(PRIVATEKEY_LOCATION);
        certificateLocation = props.getProperty(CERTIFICATE_LOCATION);
    }

    @Override
    protected void saveConfigHistory(Properties props) {
        super.saveConfigHistory(props);
        if(keystoreLocation != null)  props.setProperty(KEYSTORE_LOCATION, keystoreLocation);
        if(keyStorePassword != null)   props.setProperty(KEYSTORE_PASSWORD, keyStorePassword);
        if(keyPassword != null)  props.setProperty(KEY_PASSWORD, keyPassword);
        if(privateKeyLocation != null) props.setProperty(PRIVATEKEY_LOCATION, privateKeyLocation);
        if(certificateLocation != null) props.setProperty(CERTIFICATE_LOCATION, certificateLocation);
    }

    @Override
    protected String getPropertyFileName() {
        return "gateway-keystore.properties";
    }
}
