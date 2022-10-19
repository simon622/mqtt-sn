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

import org.slj.mqtt.sn.model.MqttsnClientCredentials;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.Files;

import java.io.*;
import java.util.Date;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;

public class MqttsnFilesystemStorageService extends MqttsnService implements IMqttsnStorageService {

    final String HOME_DIR = "user.dir";
    final String TMP_DIR = "java.io.tmpdir";
    static final String FIRSTRUN = "firstrun";
    private File settingsFile = null;
    private Properties properties = null;
    private File path;
    private String workspace;
    private boolean firstRun = false;
    private IMqttsnObjectReaderWriter readerWriter;

    public MqttsnFilesystemStorageService(IMqttsnObjectReaderWriter readerWriter, File path, String workspace) {
        this.path = path;
        this.workspace = workspace;
        this.readerWriter = readerWriter;
        init();
    }

    public MqttsnFilesystemStorageService(IMqttsnObjectReaderWriter readerWriter, String workspace) {
        this.workspace = workspace;
        this.readerWriter = readerWriter;
        init();
    }

    public MqttsnFilesystemStorageService(String workspace) {
        this.workspace = workspace;
        this.readerWriter = IMqttsnObjectReaderWriter.DEFAULT;
        init();
    }

    public MqttsnFilesystemStorageService(IMqttsnObjectReaderWriter readerWriter) {
        this.readerWriter = readerWriter;
        init();
    }

    public MqttsnFilesystemStorageService() {
        this(IMqttsnObjectReaderWriter.DEFAULT);
    }

    private void init(){
        initRoot();
        initWorkspace();
        initSettings();
    }

    private synchronized void initRoot(){
        path = path == null ?  new File(System.getProperty(HOME_DIR)) : path;
        path = new File(path, IMqttsnStorageService.DEFAULT_FOLDER_NAME);
        if(!path.isDirectory()) throw new MqttsnSecurityException("path location must be a directory");
        if(!path.exists()){
            path.mkdirs();
        }
    }

    private synchronized void initWorkspace(){
        path = new File(path, workspace);
        if(!path.exists()){
            path.mkdirs();
        }
    }

    public synchronized void initSettings() {
        if(settingsFile == null){
            settingsFile = new File(path, DEFAULT_SETTINGS_FILENAME);
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
                        firstRun = true;
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

    @Override
    public void saveFile(String fileName, byte[] bytes) throws MqttsnException {
        try {
            logger.log(Level.INFO, String.format("writing data to storage [%s] -> [%s]", fileName, bytes.length));
            if(fileName.contains(File.separator) || fileName.contains(".."))
                throw new MqttsnSecurityException("only able to write to child of root storage");
            File f = new File(path, fileName);
            Files.writeWithLock(f.getAbsolutePath(), bytes);
        } catch(IOException e){
            throw new MqttsnException(e);
        }
    }

    @Override
    public Optional<byte[]> loadFileIfExists(String fileName) throws MqttsnException {
        File f = new File(path, fileName);
        if(!f.exists()) return Optional.empty();
        try(InputStream fis = new FileInputStream(f)){
            return Optional.of(Files.read(fis, 1024));
        }catch(IOException e){
            throw new MqttsnException(e);
        }
    }

    @Override
    public void updateRuntimeOptionsFromFilesystem(MqttsnOptions options) throws MqttsnException {
        if(firstRun){
            if(options.getClientCredentials() != null){
                writeCredentials(options.getClientCredentials());
            }
        } else {
            //load it all
            MqttsnClientCredentials credsFromFilesystem = readCredentials();
            if(credsFromFilesystem != null){
                options.withClientCredentials(credsFromFilesystem);
            }
        }
    }

    public void writeRuntimeOptions(MqttsnOptions options) throws MqttsnException {
        writeCredentials(options.getClientCredentials());
    }

    protected void writeCredentials(MqttsnClientCredentials credentials) throws MqttsnException {
        if(readerWriter != null){
            saveFile(IMqttsnStorageService.CREDENTIALS_FILENAME,
                    readerWriter.write(credentials));
        }
    }

    protected MqttsnClientCredentials readCredentials() throws MqttsnException {
        if(readerWriter != null){
            Optional<byte[]> data = loadFileIfExists(IMqttsnStorageService.CREDENTIALS_FILENAME);
            if(data.isPresent()){
                return readerWriter.load(MqttsnClientCredentials.class, data.get());
            } else {
                return null;
            }
        }
        throw new MqttsnRuntimeException("unable to initialise filesystem with null reader");
    }
}
