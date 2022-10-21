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

package org.slj.mqtt.sn.cloud.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.slj.mqtt.sn.cloud.IMqttsnCloudService;
import org.slj.mqtt.sn.cloud.MqttsnConnectorDescriptor;
import org.slj.mqtt.sn.cloud.client.MqttsnCloudServiceException;
import org.slj.mqtt.sn.cloud.client.impl.HttpCloudServiceImpl;

import java.util.List;

public class HttpCloudClientTest {

    static final String SECURE_CLOUD_LOCATION = "https://mqtt-sn.cloud/api/test-services.json";
    static final String INSECURE_CLOUD_LOCATION = "http://mqtt-sn.cloud/api/test-services.json";

    protected ObjectMapper createMapper(){
        return new ObjectMapper();
    }

    @Test
    public void testServiceDiscovery() throws MqttsnCloudServiceException {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), INSECURE_CLOUD_LOCATION, 5000, 5000);
        Assert.assertEquals("cloud service should contain x2 service defintions", 2, cloudService.getConnectedServiceCount());
    }

    @Test
    public void testSecureServiceDiscovery() throws MqttsnCloudServiceException {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), SECURE_CLOUD_LOCATION, 5000, 5000);
        Assert.assertEquals("cloud service should contain x2 service defintions", 2, cloudService.getConnectedServiceCount());
    }

    @Test
    public void testInvalidDiscoveryUrl() {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), "ht://mqtt-sn.cloud/api/services.json", 5000, 5000);
        Assert.assertFalse("cloud connection should be unavailable", cloudService.hasCloudConnectivity());
    }

    @Test
    public void testTimeout() {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), "http://mqtt-sn.cloud/api/services.json", 1, 1);
        Assert.assertFalse("cloud connection should be unavailable", cloudService.hasCloudConnectivity());
    }

    @Test
    public void testInvalidAbsentUrl() {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), "http://mqtt-sn.cloud/api/services-bad.json", 1, 1);
        Assert.assertFalse("cloud connection should be unavailable", cloudService.hasCloudConnectivity());
    }

    @Test(expected = MqttsnCloudServiceException.class)
    public void testCallServiceOnOfflineCloud() throws MqttsnCloudServiceException {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), "http://mqtt-sn.cloud/api/services-bad.json", 1, 1);
        cloudService.getConnectedServiceCount();
    }

    @Test
    public void testConnectorListing() throws MqttsnCloudServiceException {
        IMqttsnCloudService cloudService =
                new HttpCloudServiceImpl(createMapper(), SECURE_CLOUD_LOCATION, 5000, 5000);
        List<MqttsnConnectorDescriptor> descriptors = cloudService.getAvailableConnectors();
        Assert.assertEquals("cloud service have 2 connectors available", 2, descriptors.size());
        assertConnectorDetails(descriptors.get(0));
    }

    protected void assertConnectorDetails(MqttsnConnectorDescriptor descriptor){
        System.err.println(descriptor);
        Assert.assertEquals("Loopback Gateway Connector", descriptor.getName());
        Assert.assertEquals("Connector Listing", descriptor.getDescription());
        Assert.assertEquals("https://github.com/simon622/mqtt-sn", descriptor.getDocumentationLink());
        Assert.assertEquals("org.slj.mqtt.sn.gateway.impl.connector.LoopbackMqttsnConnector", descriptor.getClassName());
        Assert.assertEquals("Simon Johnson", descriptor.getDeveloper());
        Assert.assertEquals("1.0", descriptor.getVersion());
        Assert.assertEquals("https://github.com/simon622/mqtt-sn", descriptor.getImageUrl());
    }
}
