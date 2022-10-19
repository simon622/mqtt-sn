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

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class MqttsnClientCredentials implements Serializable {

    private static final long serialVersionUID = 7489842550387847867L;

    protected Map<String, ClientIdToken> allowedClientIds = new HashMap<>();
    protected Map<String, UserPassword> credentials = new HashMap<>();
    protected boolean allowAllClientIds = false;

    public MqttsnClientCredentials(final boolean allowAllClientIds){
        this.allowAllClientIds = allowAllClientIds;
    }

    public MqttsnClientCredentials(){
    }

    public boolean addAllowedClientId(String clientId, String token) {
        ClientIdToken userPassword = new ClientIdToken();
        userPassword.clientId = clientId;
        userPassword.token = token;
        allowedClientIds.put(clientId, userPassword);
        return true;
    }

    public boolean addCredentials(String username, String password){
        UserPassword userPassword = new UserPassword();
        userPassword.username = username;
        userPassword.password = password;
        credentials.put(username, userPassword);
        return true;
    }

    public boolean removeUsername(String username){
        return credentials.remove(username) != null;
    }

    public boolean removeClientId(String clientId){
        return allowedClientIds.remove(clientId) != null;
    }

    public Set<String> getClientIdTokens() {
        if(allowedClientIds == null || allowedClientIds.isEmpty()){
            return Collections.emptySet();
        }
        return allowedClientIds.entrySet().stream().map(stringClientIdTokenEntry ->
                stringClientIdTokenEntry.getValue().token).collect(Collectors.toSet());
    }

    public List<ClientIdToken> getAllowedClientIds() {
        if(allowedClientIds == null || allowedClientIds.isEmpty()){
            return Collections.emptyList();
        }
        return allowedClientIds.values().stream().collect(Collectors.toList());
    }

    public List<UserPassword> getCredentials() {
        if(credentials == null || credentials.isEmpty()){
            return Collections.emptyList();
        }
        return credentials.values().stream().collect(Collectors.toList());
    }

    public boolean isAllowAllClientIds() {
        return allowAllClientIds;
    }

    public void setAllowAllClientIds(boolean allowAllClientIds){
        this.allowAllClientIds = allowAllClientIds;
    }

    class ClientIdToken implements Serializable{
        public String clientId;
        public String token;
    }

    class UserPassword implements Serializable{
        public String username;
        public String password;
    }

    @Override
    public String toString() {
        return "MqttsnClientCredentials{" +
                "allowedClientIds=" + allowedClientIds +
                ", credentials=" + credentials +
                ", allowAllClientIds=" + allowAllClientIds +
                '}';
    }
}
