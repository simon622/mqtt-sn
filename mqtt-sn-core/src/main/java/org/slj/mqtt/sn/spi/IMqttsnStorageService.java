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

package org.slj.mqtt.sn.spi;

import org.slj.mqtt.sn.model.IPreferenceNamespace;
import org.slj.mqtt.sn.model.MqttsnOptions;

import java.io.File;
import java.util.Date;
import java.util.Optional;


/**
 *
 */
@MqttsnService
public interface IMqttsnStorageService extends IMqttsnService {

    String DEFAULT_FOLDER_NAME = ".mqtt-sn-runtimes";
    String DEFAULT_SETTINGS_FILENAME = "mqtt-sn-settings.xml";
    String CREDENTIALS_FILENAME = "mqtt-sn-client-credentials";
    String PREDEFINED_FILENAME = "mqtt-sn-predefined-alias";

    static Class DEFAULT_NAMESPACE = IMqttsnStorageService.class;

    <T> T getPreferenceValue(String key, Class<T> type);

    void setStringPreference(String key, String value) throws MqttsnException;

    String getStringPreference(String key, String defaultValue) ;

    void setIntegerPreference(String key, Integer value) throws MqttsnException;

    void setLongPreference(String key, Long value) throws MqttsnException;

    Long getLongPreference(String key, Long defaultValue);

    void setBooleanPreference(String key, Boolean value) throws MqttsnException;

    Boolean getBooleanPreference(String key, Boolean defaultValue) ;
    Integer getIntegerPreference(String key, Integer defaultValue) ;

    void setDatePreference(String key, Date value) throws MqttsnException;

    Date getDatePreference(String key, Date defaultValue) ;

    void saveFile(String fileName, byte[] arr) throws MqttsnException;

    Optional<byte[]> loadFileIfExists(String fileName) throws MqttsnException ;

    void updateRuntimeOptionsFromStorage(MqttsnOptions options) throws MqttsnException ;

    void writeRuntimeOptions(MqttsnOptions options) throws MqttsnException ;

    IMqttsnStorageService getPreferenceNamespace(IPreferenceNamespace namespace);

    void writeFieldsToStorage(Object configurableBean) throws MqttsnRuntimeException ;

    void initializeFieldsFromStorage(Object configurableBean) throws MqttsnRuntimeException ;

    File getWorkspaceRoot();
}
