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

package org.slj.mqtt.sn.console.impl;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.console.http.*;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.IMqttsnSubscription;
import org.slj.mqtt.sn.model.session.IMqttsnTopicRegistration;
import org.slj.mqtt.sn.spi.IMqttsnOriginatingMessageSource;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class SessionHandler extends MqttsnConsoleAjaxRealmHandler {

    static final String CLIENTID = "clientId";

    public SessionHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws HttpException, IOException {

        try {
            String clientId = getMandatoryParameter(request, CLIENTID);
            SessionBean bean = populateBean(clientId);
            writeJSONBeanResponse(request, HttpConstants.SC_OK, bean);
        } catch(Exception e){
            throw new HttpInternalServerError("error populating bean;", e);
        }
    }


    protected SessionBean populateBean(String clientId) throws MqttsnException {
        SessionBean bean = new SessionBean();
        Optional<IMqttsnContext> context = registry.getSessionRegistry().lookupClientIdSession(clientId);
        if(context.isPresent()){
            IMqttsnSession session = registry.getSessionRegistry().getSession(context.get(), false);
            if(session != null){
                bean.clientId = clientId; //Html.span(clientId, null, true, null, "badge bg-primary");
                bean.generatedAt = new Date();
                bean.lastSeen = session.getLastSeen();
                bean.keepAlive = session.getKeepAlive() + " seconds";
                bean.sessionExpiryInterval = session.getSessionExpiryInterval() + " seconds";
                bean.sessionStarted = session.getSessionStarted();
                if(session.getSessionStarted() != null){
                    bean.timeSinceConnect = MqttsnUtils.getDurationString((System.currentTimeMillis() - session.getSessionStarted().getTime()));
                }
                if(session.getLastSeen() != null){
                    bean.timeSinceLastSeen = MqttsnUtils.getDurationString((System.currentTimeMillis() - session.getLastSeen().getTime()));
                }
                bean.sessionState = getColorForState(session.getClientState());
                bean.queueSize = String.valueOf(registry.getMessageQueue().size(session));
                bean.inflightEgress = String.valueOf(registry.getMessageStateService().countInflight(
                        session.getContext(), IMqttsnOriginatingMessageSource.LOCAL));
                bean.inflightIngress = String.valueOf(registry.getMessageStateService().countInflight(
                        session.getContext(), IMqttsnOriginatingMessageSource.REMOTE));
                bean.networkAddress = registry.getNetworkRegistry().getContext(session.getContext()).getNetworkAddress().toSimpleString();
                Set<IMqttsnSubscription> subs = registry.getSubscriptionRegistry().readSubscriptions(session);
                bean.subscriptions = subs.stream().map(s -> new SubscriptionBean(s)).collect(Collectors.toList());

                Set<IMqttsnTopicRegistration> regs = registry.getTopicRegistry().getRegistrations(session);
                bean.registrations = regs.stream().map(s -> new RegistrationBean(s)).collect(Collectors.toList());
            }
        }
        return bean;
    }

    class SessionBean {

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HttpConstants.DEFAULT_DATE_FORMAT)
        public Date generatedAt;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HttpConstants.DEFAULT_DATE_FORMAT)
        public Date sessionStarted;
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = HttpConstants.DEFAULT_DATE_FORMAT)
        public Date lastSeen;

        public String clientId = "";
        public String keepAlive = "";
        public String sessionExpiryInterval = "";
        public String timeSinceConnect = "";
        public String timeSinceLastSeen = "";
        public String sessionState = "";
        public String queueSize = "";
        public String inflightEgress = "";
        public String inflightIngress = "";
        public String networkAddress = "";

        public String foo = "";

        public List<SubscriptionBean> subscriptions;
        public List<RegistrationBean> registrations;
    }

    class SubscriptionBean {

        public SubscriptionBean(IMqttsnSubscription s){
            topicFilter = s.getTopicPath().toString();
            QoS = s.getGrantedQoS();
        }

        public String topicFilter;
        public int QoS;
    }

    class RegistrationBean {

        public RegistrationBean(IMqttsnTopicRegistration s){
            topicFilter = s.getTopicPath().toString();
            alias = s.getAliasId();
            confirmed = s.isConfirmed();
        }

        public String topicFilter;
        public int alias;
        public boolean confirmed;
    }

    public String getColorForState(MqttsnClientState state){
        if(state == null) return Html.span("N/A", Html.RED, true);
        switch(state){
            case AWAKE:
            case ACTIVE: return Html.span(state.toString(), null, true, null, "badge bg-success");
            case ASLEEP: return Html.span(state.toString(), null, false, null, "badge bg-warning");
            default: return Html.span(state.toString(), null, true, null, "badge bg-danger");
        }
    }
}
