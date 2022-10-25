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

package org.slj.mqtt.sn.cloud;

import java.io.Serializable;
import java.util.List;

public class MqttsnConnectorDescriptor implements Serializable {

    private String name;
    private String description;
    private String signupLink;
    private String url;
    private String version;
    private String developer;
    private String imageUrl;
    private String companyName;
    private String className;
    private String protocol;
    private String ribbon;
    private List<MqttsnConnectorDescriptorProperty> properties;


    public MqttsnConnectorDescriptor(){
    }

    public List<MqttsnConnectorDescriptorProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<MqttsnConnectorDescriptorProperty> properties) {
        this.properties = properties;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSignupLink() {
        return signupLink;
    }

    public void setSignupLink(String signupLink) {
        this.signupLink = signupLink;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getRibbon() {
        return ribbon;
    }

    public void setRibbon(String ribbon) {
        this.ribbon = ribbon;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDeveloper() {
        return developer;
    }

    public void setDeveloper(String developer) {
        this.developer = developer;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    @Override
    public String toString() {
        return "MqttsnConnectorDescriptor{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", signupLink='" + signupLink + '\'' +
                ", url='" + url + '\'' +
                ", version='" + version + '\'' +
                ", developer='" + developer + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", companyName='" + companyName + '\'' +
                ", className='" + className + '\'' +
                ", protocol='" + protocol + '\'' +
                ", ribbon='" + ribbon + '\'' +
                '}';
    }

    public void copyFrom(MqttsnConnectorDescriptor impl){
        setClassName(impl.getClassName());
        setCompanyName(impl.getCompanyName());
        setDescription(impl.getDescription());
        setDeveloper(impl.getDeveloper());
        setUrl(impl.getUrl());
        setSignupLink(impl.getSignupLink());
        setImageUrl(impl.getImageUrl());
        setVersion(impl.getVersion());
        setName(impl.getName());
        setProtocol(impl.getProtocol());
        setRibbon(impl.getRibbon());
        setProperties(impl.getProperties());
    }
}