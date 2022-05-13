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
import org.slj.mqtt.sn.model.IMqttsnSessionState;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnQueueProcessorStateService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.util.logging.Level;

public class MqttsnGatewayQueueProcessorStateService extends MqttsnService<IMqttsnGatewayRuntimeRegistry>
        implements IMqttsnQueueProcessorStateService {

    @Override
    public boolean canReceive(IMqttsnContext context) throws MqttsnException {
        IMqttsnSessionState state = getRegistry().getGatewaySessionService().getSessionState(context, false);
        return state != null && MqttsnUtils.in(state.getClientState() , MqttsnClientState.CONNECTED, MqttsnClientState.AWAKE);
    }

    @Override
    public void queueEmpty(IMqttsnContext context) throws MqttsnException {

        IMqttsnSessionState state = getRegistry().getGatewaySessionService().getSessionState(context, false);
        if(state != null){
            logger.log(Level.FINE, String.format("notified that the queue is empty, post process state is - [%s]", state));
            if(MqttsnUtils.in(state.getClientState() , MqttsnClientState.AWAKE)){
                logger.log(Level.INFO, String.format("notified that the queue is empty, putting device back to sleep and sending ping-resp - [%s]", context));
                //-- need to transition the device back to sleep
                getRegistry().getGatewaySessionService().disconnect(state,
                        getRegistry().getMessageFactory().createDisconnect(state.getKeepAlive()));
                //-- need to send the closing ping-resp
                IMqttsnMessage pingResp = getRegistry().getMessageFactory().createPingresp();
                getRegistry().getTransport().writeToTransport(getRegistry().getNetworkRegistry().getContext(context), pingResp);
            }
        }
    }
}
