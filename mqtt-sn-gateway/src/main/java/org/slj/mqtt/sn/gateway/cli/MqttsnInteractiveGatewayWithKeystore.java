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

import org.slj.mqtt.sn.spi.MqttsnException;

public abstract class MqttsnInteractiveGatewayWithKeystore extends MqttsnInteractiveGateway {

    static final String KEYSTORE_LOCATION = "keyStoreLocation";
    static final String KEYSTORE_PASSWORD = "keyStorePassword";
    static final String KEY_PASSWORD = "keyPassword";
    static final String CERTIFICATE_LOCATION = "certificateLocation";
    static final String PRIVATEKEY_LOCATION = "privateKeyLocation";

//    protected String keystoreLocation;
//    protected String certificateLocation;
//    protected String privateKeyLocation;
//    protected String keyStorePassword;
//    protected String keyPassword;

    @Override
    protected void captureSettings() throws MqttsnException {
        super.captureSettings();
        storageService.setStringPreference(KEYSTORE_LOCATION,
                captureFilePath(input, output, "Please enter the file location of your keystore (leave blank for certs & keys)"));
        if(storageService.getStringPreference(KEYSTORE_LOCATION, null) == null){
            storageService.setStringPreference(KEYSTORE_PASSWORD,
                    captureString(input, output,  "Please enter the keystore password"));
        }
        else {
            storageService.setStringPreference(CERTIFICATE_LOCATION,
                    captureFilePath(input, output, "Please enter the file location of your certificate"));
            storageService.setStringPreference(PRIVATEKEY_LOCATION,
                    captureFilePath(input, output, "Please enter the file location of your private key"));
            storageService.setStringPreference(KEY_PASSWORD,
                    captureFilePath(input, output, "Please enter the private key password"));
        }
    }

//    @Override
//    protected void loadFromSettings() throws MqttsnException {
//        super.loadFromSettings();
//        keystoreLocation = getRuntimeRegistry().getStorageService().getStringPreference(KEYSTORE_LOCATION, null);
//        keyStorePassword = getRuntimeRegistry().getStorageService().getStringPreference(KEYSTORE_PASSWORD, null);
//        keyPassword = getRuntimeRegistry().getStorageService().getStringPreference(KEY_PASSWORD, null);
//        privateKeyLocation = getRuntimeRegistry().getStorageService().getStringPreference(PRIVATEKEY_LOCATION, null);
//        certificateLocation = getRuntimeRegistry().getStorageService().getStringPreference(CERTIFICATE_LOCATION, null);
//    }
//
//    @Override
//    protected void saveToSettings() throws MqttsnException {
//        super.saveToSettings();
//        getRuntimeRegistry().getStorageService().setStringPreference(KEYSTORE_LOCATION, keystoreLocation);
//        getRuntimeRegistry().getStorageService().setStringPreference(KEYSTORE_PASSWORD, keyStorePassword);
//        getRuntimeRegistry().getStorageService().setStringPreference(KEY_PASSWORD, keyPassword);
//        getRuntimeRegistry().getStorageService().setStringPreference(PRIVATEKEY_LOCATION, privateKeyLocation);
//        getRuntimeRegistry().getStorageService().setStringPreference(CERTIFICATE_LOCATION, certificateLocation);
//    }
}
