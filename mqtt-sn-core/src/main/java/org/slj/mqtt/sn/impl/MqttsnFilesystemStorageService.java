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

import org.slj.mqtt.sn.model.IPreferenceNamespace;
import org.slj.mqtt.sn.model.MqttsnClientCredentials;
import org.slj.mqtt.sn.model.MqttsnOptions;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.Files;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

public class MqttsnFilesystemStorageService extends AbstractMqttsnService implements IMqttsnStorageService {

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
        if(!path.exists()){
            path.mkdirs();
        }
        if(!path.isDirectory()) throw new MqttsnSecurityException("path location must be a directory");
    }

    private synchronized void initWorkspace(){
        path = new File(path, workspace);
        if(!path.exists()){
            path.mkdirs();
        }

        try {
            Files.createRuntimeLockFile(path);
        } catch(IOException e){
            throw new MqttsnSecurityException("workspace in use, cannot start");
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
                        logger.info("initialising file system settings storage at {}",
                                settingsFile.getAbsolutePath());
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
            logger.info("writing data to storage {} -> {}", fileName, bytes.length);
            if(fileName.contains(File.separator) || fileName.contains(".."))
                throw new MqttsnSecurityException("only able to write to child of root storage");
            File f = new File(path, fileName);
            Files.writeWithLock(f, bytes);
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
    public void updateRuntimeOptionsFromStorage(MqttsnOptions options) throws MqttsnException {
        if(firstRun){
            if(options.getClientCredentials() != null){
                writeCredentials(options.getClientCredentials());
            }
            if(options.getPredefinedTopics() != null && !options.getPredefinedTopics().isEmpty()){
                writePredefinedTopics(options.getPredefinedTopics());
            }
        } else {
            //load it all
            MqttsnClientCredentials credsFromFilesystem = readCredentials();
            if(credsFromFilesystem != null){
                options.withClientCredentials(credsFromFilesystem);
            }
            Map<String, Integer> alias = readPredefinedTopics();
            if(alias != null){
                options.getPredefinedTopics().clear();
                options.getPredefinedTopics().putAll(alias);
            }
        }
    }

    public void writeRuntimeOptions(MqttsnOptions options) throws MqttsnException {
        writeCredentials(options.getClientCredentials());
        writePredefinedTopics(options.getPredefinedTopics());
    }

    protected void writeCredentials(MqttsnClientCredentials credentials) throws MqttsnException {
        if(readerWriter != null){
            saveFile(IMqttsnStorageService.CREDENTIALS_FILENAME,
                    readerWriter.write(credentials));
        }
    }

    protected void writePredefinedTopics(Map<String, Integer> alias) throws MqttsnException {
        if(readerWriter != null){
            saveFile(IMqttsnStorageService.PREDEFINED_FILENAME,
                    readerWriter.write(new Predefined(alias)));
        }
    }

    protected Map<String, Integer> readPredefinedTopics() throws MqttsnException {
        if(readerWriter != null){
            Optional<byte[]> data = loadFileIfExists(IMqttsnStorageService.PREDEFINED_FILENAME);
            if(data.isPresent()){
                return readerWriter.load(Predefined.class, data.get()).alias;
            } else {
                return null;
            }
        }
        throw new MqttsnRuntimeException("unable to initialise filesystem with null reader");
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

    public <T> T getPreferenceValue(String key, Class<T> type) {
        if(String.class.isAssignableFrom(type)){
            return (T) getStringPreference(key, null);
        }
        else if(Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)){
            return (T) getIntegerPreference(key, null);
        }
        else if(Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)){
            return (T) getLongPreference(key, null);
        }
        else if(Date.class.isAssignableFrom(type)){
            return (T) getDatePreference(key, null);
        }
        else if(Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)){
            return (T) getBooleanPreference(key, null);
        }
        throw new MqttsnRuntimeException("unsupported preference type " + type);
    }

    protected String getNamespacePrefix(IPreferenceNamespace namespace, String key){
        String space = namespace.getNamespace() + "-";
        if(key != null){
            space = space += key;
        }
        return space;
    }

    protected void initializeFieldsFromStorageInternal(String prefix, Object configurableBean) throws MqttsnRuntimeException {
        try {
            Class c = configurableBean.getClass();
            Field[] fields = c.getDeclaredFields();
            for(Field field : fields){
                if(!Modifier.isStatic(field.getModifiers())){
                    field.setAccessible(true);
                    if(supportedValueType(field.getType())) {
                        Object value = null;
                        if(prefix == null){
                            value = getPreferenceValue(field.getName(), field.getType());
                        } else {
                            value = getPreferenceValue(prefix + field.getName(), field.getType());
                        }

                        //-- only set the value from storage is not null
                        if(value != null)
                            field.set(configurableBean, value);
                    }
                    field.setAccessible(false);
                }
            }
        } catch(IllegalAccessException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    protected void writeFieldsToStorageInternal(String prefix, Object configurableBean) throws MqttsnRuntimeException {
        try {
            Class c = configurableBean.getClass();
            Field[] fields = c.getDeclaredFields();
            for(Field field : fields){
                if(!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    if (supportedValueType(field.getType())) {
                        Object value = field.get(configurableBean);
                        if (value != null) {
                            if (prefix == null) {
                                setStringPreference(field.getName(), String.valueOf(value));
                            } else {
                                setStringPreference(prefix + field.getName(), String.valueOf(value));
                            }
                        }
                    }
                    field.setAccessible(false);
                }
            }
        } catch(IllegalAccessException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    protected boolean supportedValueType(Class cls){
        return cls == String.class || cls == Integer.class || cls == Long.class || cls == Date.class || cls == Boolean.class
        || cls == int.class || cls == long.class || cls == boolean.class;
    }

    public void initializeFieldsFromStorage(Object configurableBean) throws MqttsnRuntimeException {
        initializeFieldsFromStorageInternal(null, configurableBean);
    }

    public void writeFieldsToStorage(Object configurableBean) throws MqttsnRuntimeException {
        writeFieldsToStorageInternal(null, configurableBean);
    }

    @Override
    public File getWorkspaceRoot() {
        if(path == null) throw new MqttsnRuntimeException("storage not initialised");
        return path;
    }

    public void lockWorkspace(){

    }

    @Override
    public IMqttsnStorageService getPreferenceNamespace(final IPreferenceNamespace namespace) {
        return new IMqttsnStorageService() {
            @Override
            public <T> T getPreferenceValue(String key, Class<T> type) {
                return MqttsnFilesystemStorageService.this.getPreferenceValue(getNamespacePrefix(namespace, key), type);
            }

            @Override
            public void setStringPreference(String key, String value) throws MqttsnException {
                MqttsnFilesystemStorageService.this.setStringPreference(getNamespacePrefix(namespace, key), value);
            }

            @Override
            public String getStringPreference(String key, String defaultValue) {
                return MqttsnFilesystemStorageService.this.getStringPreference(getNamespacePrefix(namespace, key), defaultValue);
            }

            @Override
            public void setIntegerPreference(String key, Integer value) throws MqttsnException {
                MqttsnFilesystemStorageService.this.setIntegerPreference(getNamespacePrefix(namespace, key), value);
            }

            @Override
            public void setLongPreference(String key, Long value) throws MqttsnException {
                MqttsnFilesystemStorageService.this.setLongPreference(getNamespacePrefix(namespace, key), value);
            }

            @Override
            public Long getLongPreference(String key, Long defaultValue) {
                return MqttsnFilesystemStorageService.this.getLongPreference(getNamespacePrefix(namespace, key), defaultValue);
            }

            @Override
            public void setBooleanPreference(String key, Boolean value) throws MqttsnException {
                MqttsnFilesystemStorageService.this.setBooleanPreference(getNamespacePrefix(namespace, key), value);
            }

            @Override
            public Boolean getBooleanPreference(String key, Boolean defaultValue) {
                return MqttsnFilesystemStorageService.this.getBooleanPreference(getNamespacePrefix(namespace, key), defaultValue);
            }

            @Override
            public Integer getIntegerPreference(String key, Integer defaultValue) {
                return MqttsnFilesystemStorageService.this.getIntegerPreference(getNamespacePrefix(namespace, key), defaultValue);
            }

            @Override
            public void setDatePreference(String key, Date value) throws MqttsnException {
                MqttsnFilesystemStorageService.this.setDatePreference(getNamespacePrefix(namespace, key), value);
            }

            @Override
            public Date getDatePreference(String key, Date defaultValue) {
                return MqttsnFilesystemStorageService.this.getDatePreference(getNamespacePrefix(namespace, key), defaultValue);
            }

            @Override
            public void saveFile(String fileName, byte[] arr) throws MqttsnException {
                MqttsnFilesystemStorageService.this.saveFile(fileName, arr);
            }

            @Override
            public Optional<byte[]> loadFileIfExists(String fileName) throws MqttsnException {
                return MqttsnFilesystemStorageService.this.loadFileIfExists(fileName);
            }

            @Override
            public void updateRuntimeOptionsFromStorage(MqttsnOptions options) throws MqttsnException {
                MqttsnFilesystemStorageService.this.updateRuntimeOptionsFromStorage(options);
            }

            @Override
            public void writeRuntimeOptions(MqttsnOptions options) throws MqttsnException {
                MqttsnFilesystemStorageService.this.writeRuntimeOptions(options);
            }

            @Override
            public IMqttsnStorageService getPreferenceNamespace(final IPreferenceNamespace namespace) {
                return MqttsnFilesystemStorageService.this.getPreferenceNamespace(namespace);
            }

            @Override
            public void writeFieldsToStorage(Object configurableBean) throws MqttsnRuntimeException {
                MqttsnFilesystemStorageService.this.writeFieldsToStorageInternal(
                        getNamespacePrefix(namespace, null), configurableBean);
            }

            @Override
            public void initializeFieldsFromStorage(Object configurableBean) throws MqttsnRuntimeException {
                MqttsnFilesystemStorageService.this.initializeFieldsFromStorageInternal(
                        getNamespacePrefix(namespace, null), configurableBean);
            }

            @Override
            public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
                throw new UnsupportedOperationException("cannot start via wrapper");
            }

            @Override
            public void stop() throws MqttsnException {
                throw new UnsupportedOperationException("cannot stop via wrapper");
            }

            @Override
            public File getWorkspaceRoot() {
                return MqttsnFilesystemStorageService.this.getWorkspaceRoot();
            }

            @Override
            public boolean running() {
                return MqttsnFilesystemStorageService.this.running();
            }
        };
    }
}

class Predefined implements Serializable {
    public Map<String, Integer> alias;
    public Predefined(){}
    public Predefined(final Map<String, Integer> alias){
        this.alias = alias;
    }
}
