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

package org.slj.mqtt.sn.impl;

import org.slj.mqtt.sn.spi.*;

import java.io.*;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;

public class MqttsnFilesystemStorageService extends MqttsnService implements IMqttsnStorageService {

    final String HOME_DIR = "user.dir";
    final String TMP_DIR = "java.io.tmpdir";

    static final String DEFAULT_FOLDER = "mqtt-sn-runtimes";
    static final String DEFAULT_SETTINGS_FILENAME = "mqtt-sn-settings.xml";
    static final String FIRSTRUN = "firstrun";
    private File settingsFile = null;
    private Properties properties = null;
    private File path;
    private String fileName = DEFAULT_SETTINGS_FILENAME;

    public MqttsnFilesystemStorageService(File path, String fileName) {
        try {
            this.path = path;
            this.fileName = fileName;
            init();
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    public MqttsnFilesystemStorageService(String fileName) {
        try {
            this.fileName = fileName;
            init();
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    public MqttsnFilesystemStorageService() {
        try {
            init();
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    public synchronized void init() throws MqttsnException {
        if(settingsFile == null){
            path = path == null ?  new File(System.getProperty(HOME_DIR)) : path;
            if(!path.isDirectory()) throw new MqttsnSecurityException("path location must be a directory");
            path = new File(path, DEFAULT_FOLDER);
            if(!path.exists()){
                path.mkdirs();
            }
            settingsFile = new File(path, fileName);
            try {
                if(!settingsFile.exists()){
                    if(!settingsFile.createNewFile()){
                        throw new MqttsnSecurityException("unable to read / write to settings location");
                    }
                    else {
                        logger.log(Level.INFO, String.format("initialising file system settings storage at [%s]",
                                settingsFile.getAbsolutePath()));
                        properties = new Properties();
                        setDatePreference(FIRSTRUN, new Date());
                    }
                } else {
                    try(InputStream fis =
                                new FileInputStream(settingsFile)){
                        properties = new Properties();
                        properties.loadFromXML(fis);
                    }
                }
            } catch(IOException e){
                throw new MqttsnSecurityException("unable to read / write to settings location; please modify file permissions to settings location", e);
            }
        }
    }

    protected synchronized void writePreferenceInternal(String key, String value) {
        boolean needsWrite = true;
        try {
            if(key == null) throw new MqttsnRuntimeException("unable to write settings value with <null> key");
            if(value == null){
                needsWrite = properties.remove(key) != null;
            } else {
                Object oldVal = properties.setProperty(key, value);
                needsWrite = oldVal == null || !oldVal.equals(value);
            }
        } finally {
            if(needsWrite){
                FileOutputStream fos = null;
                try{
                    fos = new FileOutputStream(settingsFile);
                    properties.storeToXML(fos, "Saved by the mqtt-sn runtime environment", "UTF-8");
                } catch(IOException e){
                    throw new MqttsnSecurityException("unable to write settings file", e);
                } finally {
                    try {
                        fos.close();
                    } catch(Exception e){}
                }
            }
        }
    }

    protected String readPreferenceInternal(String key, String defaultValue) {
        if(key == null || key.trim().length() == 0)
            throw new MqttsnRuntimeException("invalid settings key format <null> or empty string");
        String propertyValue = properties.getProperty(key);
        return propertyValue == null ? defaultValue : propertyValue;
    }

    @Override
    public void putAll(Properties properties) throws MqttsnException {
        this.properties.putAll(properties);
    }

    @Override
    public Properties loadAll() throws MqttsnException {
        Properties props = new Properties();
        props.putAll(properties);
        return props;
    }

    @Override
    public void setIntegerPreference(String key, Integer value) throws MqttsnException {
        writePreferenceInternal(key, value == null ? null : value.toString());
    }

    @Override
    public Integer getIntegerPreference(String key, Integer defaultValue)  {
        String val = readPreferenceInternal(key, defaultValue == null ? null : defaultValue.toString());
        return val == null ? null : Integer.valueOf(val);
    }

    @Override
    public void setDatePreference(String key, Date value) {
        setLongPreference(key, value == null ? null : value.getTime());
    }

    @Override
    public Date getDatePreference(String key, Date defaultValue) {
        String val = readPreferenceInternal(key, defaultValue == null ? null : String.valueOf(defaultValue.getTime()));
        return val == null ? null : new Date(Long.valueOf(val));
    }

    @Override
    public void setStringPreference(String key, String value) {
        writePreferenceInternal(key, value);
    }

    @Override
    public String getStringPreference(String key, String defaultValue) {
        return readPreferenceInternal(key, defaultValue);
    }

    @Override
    public void setLongPreference(String key, Long value) {
        writePreferenceInternal(key, value == null ? null : value.toString());
    }

    @Override
    public Long getLongPreference(String key, Long defaultValue) {
        String val = readPreferenceInternal(key, defaultValue == null ? null : defaultValue.toString());
        return val == null ? null : Long.valueOf(val);
    }

    @Override
    public void setBooleanPreference(String key, Boolean value) {
        writePreferenceInternal(key, value == null ? null : value.toString());
    }

    @Override
    public Boolean getBooleanPreference(String key, Boolean defaultValue) {
        String val = readPreferenceInternal(key, defaultValue == null ? null : defaultValue.toString());
        return val == null ? null : Boolean.valueOf(val);
    }
}
