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

package org.slj.mqtt.sn.gateway.impl.gateway;

import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;

public class MqttsnGatewayQueueProcessorStateService extends AbstractMqttsnService
        implements IMqttsnQueueProcessorStateService {

    protected IMqttsnGatewayRuntimeRegistry getRegistry(){
        return (IMqttsnGatewayRuntimeRegistry) super.getRegistry();
    }

    @Override
    public boolean canReceive(IMqttsnContext context) throws MqttsnException {
        IMqttsnSession session = getRegistry().getSessionRegistry().getSession(context, false);
        return session != null && MqttsnUtils.in(session.getClientState() , MqttsnClientState.ACTIVE, MqttsnClientState.AWAKE);
    }

    @Override
    public void queueEmpty(IMqttsnContext context) throws MqttsnException {

        IMqttsnSession session = getRegistry().getSessionRegistry().getSession(context, false);
        if(session != null){
            logger.debug("notified that the queue is empty, post process state is - {}", session);
            if(MqttsnUtils.in(session.getClientState() , MqttsnClientState.AWAKE)){
                logger.info("notified that the queue is empty, putting device back to sleep and sending ping-resp - {}", context);
                //-- need to transition the device back to sleep
                getRegistry().getGatewaySessionService().disconnect(session,
                        getRegistry().getMessageFactory().createDisconnect(session.getKeepAlive()));
                //-- need to send the closing ping-resp
                IMqttsnMessage pingResp = getRegistry().getMessageFactory().createPingresp();
                getRegistry().getTransport().writeToTransport(getRegistry().getNetworkRegistry().getContext(context), pingResp);
            }
        }
    }
}
