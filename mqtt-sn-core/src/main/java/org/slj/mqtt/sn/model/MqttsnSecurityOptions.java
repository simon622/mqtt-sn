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

package org.slj.mqtt.sn.model;

import org.slj.mqtt.sn.utils.Security;

/**
 * Provides security related configuration for payload encryption and publish-integrity checks
 */
public class MqttsnSecurityOptions {

    public enum INTEGRITY_TYPE {none, checksum, hmac, message};
    public enum INTEGRITY_POINT {protocol_messages, publish_data};

    /**
     * The default integrity type none
     */
    public final INTEGRITY_TYPE DEFAULT_INTEGRITY_TYPE = INTEGRITY_TYPE.none;

    /**
     * The default integrity point is protocol_messages
     */
    public final INTEGRITY_POINT DEFAULT_INTEGRITY_POINT = INTEGRITY_POINT.protocol_messages;

    /**
     * The default HMAC algorithm is SHA-256
     */
    public final Security.HMAC DEFAULT_INTEGRITY_HMAC_ALGORITHM = Security.HMAC.HMAC_SHA_256;

    /**
     * The default CHECKSUM algorithm is CRC32
     */
    public final Security.CHECKSUM DEFAULT_INTEGRITY_CHECKSUM_ALGORITHM = Security.CHECKSUM.CRC32;

    /**
     * The default integrity key. IMPORTANT - this should be changed in production runtimes
     */
    public final String DEFAULT_INTEGRITY_KEY = "u6670-sddsd-22ys8uj";

    /**
     * If present, provides SASL Auth exchange
     */
    private IAuthHandler authHandler;

    private String integrityKey = DEFAULT_INTEGRITY_KEY;
    private INTEGRITY_TYPE integrityType = DEFAULT_INTEGRITY_TYPE;
    private INTEGRITY_POINT integrityPoint = DEFAULT_INTEGRITY_POINT;
    private Security.HMAC integrityHmacAlgorithm = DEFAULT_INTEGRITY_HMAC_ALGORITHM;
    private Security.CHECKSUM integrityChecksumAlgorithm = DEFAULT_INTEGRITY_CHECKSUM_ALGORITHM;

    public String getIntegrityKey() {
        return integrityKey;
    }

    public INTEGRITY_TYPE getIntegrityType() {
        return integrityType;
    }

    public INTEGRITY_POINT getIntegrityPoint() {
        return integrityPoint;
    }

    public Security.HMAC getIntegrityHmacAlgorithm() {
        return integrityHmacAlgorithm;
    }

    public Security.CHECKSUM getIntegrityChecksumAlgorithm() {
        return integrityChecksumAlgorithm;
    }

    public IAuthHandler getAuthHandler(){
        return authHandler;
    }

    public MqttsnSecurityOptions withIntegrityKey(String integrityKey){
        this.integrityKey = integrityKey;
        return this;
    }

    public MqttsnSecurityOptions withIntegrityHMacAlgorithm(Security.HMAC integrityHmacAlgoritm){
        this.integrityHmacAlgorithm = integrityHmacAlgoritm;
        return this;
    }

    public MqttsnSecurityOptions withIntegrityChecksumAlgorithm(Security.CHECKSUM integrityChecksumAlgoritm){
        this.integrityChecksumAlgorithm = integrityChecksumAlgoritm;
        return this;
    }

    public MqttsnSecurityOptions withIntegrityPoint(INTEGRITY_POINT integrityPoint){
        this.integrityPoint = integrityPoint;
        return this;
    }

    public MqttsnSecurityOptions withIntegrityType(INTEGRITY_TYPE integrityType){
        this.integrityType = integrityType;
        return this;
    }

    public MqttsnSecurityOptions withAuthHandler(IAuthHandler authHandler){
        this.authHandler = authHandler;
        return this;
    }
}


