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

public class MqttsnCloudServiceDescriptor {

    public static final String
            BRIDGE_LISTING = "BridgeListing",
            CONNECTOR_LISTING = "ConnectorListing",
            ACCOUNT_CREATE = "AccountCreate",
            ACCOUNT_AUTHORIZE = "AccountAuthorize",
            ACCOUNT_DETAILS = "AccountDetails",
            SEND_EMAIL_SERVICE = "EmailService";

    String serviceName;
    String serviceDisplayName;
    String serviceEndpoint;
    String serviceDescription;
    String serviceVersion;
    String serviceMethod = "GET";
    boolean requiresAuthToken;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getServiceEndpoint() {
        return serviceEndpoint;
    }

    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    public String getServiceDescription() {
        return serviceDescription;
    }

    public void setServiceDescription(String serviceDescription) {
        this.serviceDescription = serviceDescription;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getServiceMethod() {
        return serviceMethod;
    }

    public void setServiceMethod(String serviceMethod) {
        this.serviceMethod = serviceMethod;
    }

    public String getServiceDisplayName() {
        return serviceDisplayName;
    }

    public void setServiceDisplayName(String serviceDisplayName) {
        this.serviceDisplayName = serviceDisplayName;
    }

    public boolean isRequiresAuthToken() {
        return requiresAuthToken;
    }

    public void setRequiresAuthToken(boolean requiresAuthToken) {
        this.requiresAuthToken = requiresAuthToken;
    }

    @Override
    public String toString() {
        return "MqttsnCloudServiceDescriptor{" +
                "serviceName='" + serviceName + '\'' +
                ", serviceDisplayName='" + serviceDisplayName + '\'' +
                ", serviceEndpoint='" + serviceEndpoint + '\'' +
                ", serviceDescription='" + serviceDescription + '\'' +
                ", serviceVersion='" + serviceVersion + '\'' +
                ", serviceMethod='" + serviceMethod + '\'' +
                ", requiresAuthToken=" + requiresAuthToken +
                '}';
    }
}
