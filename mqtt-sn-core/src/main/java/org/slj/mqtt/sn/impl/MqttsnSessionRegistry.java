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

import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.impl.metrics.MqttsnSnapshotMetric;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.model.session.IMqttsnSession;
import org.slj.mqtt.sn.model.session.impl.MqttsnSessionBeanImpl;
import org.slj.mqtt.sn.spi.*;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.util.*;
import java.util.logging.Level;

/**
 * Created bean based session objects which can encapsulate the storage of session elements
 */
public class MqttsnSessionRegistry extends AbstractMqttsnSessionBeanRegistry implements IMqttsnSessionRegistry {

    protected Map<IMqttsnContext, IMqttsnSession> sessionLookup;

    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);
        sessionLookup = Collections.synchronizedMap(new HashMap());
        registerMetrics(runtime);
    }

    @Override
    public IMqttsnSession createNewSession(IMqttsnContext context) {
        if(!running()) throw new MqttsnRuntimeException("unable to create session on, service not running");
        logger.log(Level.INFO, String.format("creating new session for [%s]", context));
        IMqttsnSession session = new MqttsnSessionBeanImpl(context, MqttsnClientState.DISCONNECTED);
        sessionLookup.put(context, session);
        return session;
    }

    @Override
    public IMqttsnSession getSession(IMqttsnContext context, boolean createIfNotExists) throws MqttsnException {
        IMqttsnSession session = sessionLookup.get(context);
        if(session == null && createIfNotExists){
            synchronized (sessionLookup){
                session = sessionLookup.get(context);
                if(session == null){
                    session = createNewSession(context);
                }
            }
        }
        return session;
    }

    @Override
    public Iterator<IMqttsnSession> iterator() {
        synchronized (sessionLookup){
            return new HashSet<>(sessionLookup.values()).iterator();
        }
    }

    public boolean cleanSession(final IMqttsnContext context, final boolean deepClean) throws MqttsnException {

        IMqttsnSession session = getSession(context, false);
        if(session == null) return false;

        //clear down all prior session state
        synchronized (session){
            if(deepClean){
                //-- the queued messages
                registry.getMessageQueue().clear(session);

                //-- the subscriptions
                registry.getSubscriptionRegistry().clear(session);
            }

            //-- inflight messages & protocol messages
            registry.getMessageStateService().clear(session.getContext());

            //-- topic registrations
            registry.getTopicRegistry().clear(session);

            //-- will data
            registry.getWillRegistry().clear(session);
        }

        logger.log(Level.INFO, String.format(String.format("cleaning session state [%s], deepClean ? [%s], queueSize after clean [%s]",
                session, deepClean, registry.getMessageQueue().size(session))));
        return true;
    }

    @Override
    public void clear(IMqttsnSession session, boolean clearNetworking) throws MqttsnException {
        try {
            if(session == null) throw new NullPointerException("cannot clear <null> session");
            logger.log(Level.INFO, String.format("removing session for [%s], clear networking ? [%s]",
                    session.getContext(), clearNetworking));
            if(clearNetworking){
                getRegistry().getNetworkRegistry().
                        removeExistingClientId(session.getContext().getId());
            }
            cleanSession(session.getContext(), true);
        } finally {
            sessionLookup.remove(session.getContext());
        }
    }

    @Override
    public void clearAll() {
        Iterator<IMqttsnSession> itr = iterator();
        while(itr.hasNext()){
            clear(itr.next());
        }
    }

    @Override
    public void clear(IMqttsnSession session) {
        try {
            clear(session, true);
        } catch(MqttsnException e){
            throw new MqttsnRuntimeException(e);
        }
    }

    @Override
    public Optional<IMqttsnContext> lookupClientIdSession(String clientId){
        synchronized (sessionLookup){
            Iterator<IMqttsnContext> itr = sessionLookup.keySet().iterator();
            while(itr.hasNext()){
                IMqttsnContext c = itr.next();
                if(c != null && c.getId().equals(clientId))
                    return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    @Override
    public long countSessions(MqttsnClientState state) {
        synchronized (sessionLookup){
            return sessionLookup.values().stream().filter(s ->
                    s.getClientState() == state).count();
        }
    }

    @Override
    public long countTotalSessions() {
        synchronized (sessionLookup){
            return sessionLookup.size();
        }
    }

    @Override
    public boolean hasSession(IMqttsnContext context) {
        return sessionLookup.containsKey(context);
    }

    @Override
    public void modifyClientState(IMqttsnSession session, MqttsnClientState state){
        getSessionBean(session).setClientState(state);
        logger.log(Level.INFO, String.format("setting session-state as '%s' [%s]", state, session));
    }

    @Override
    public void modifyLastSeen(IMqttsnSession session) {
        Date lastSeen = new Date();
        getSessionBean(session).setLastSeen(lastSeen);
        logger.log(Level.INFO, String.format("setting session-lastSeen as '%s' [%s]", lastSeen, session));
    }

    @Override
    public void modifyKeepAlive(IMqttsnSession session, int keepAlive) {
        getSessionBean(session).setKeepAlive(keepAlive);
        logger.log(Level.INFO, String.format("setting session-keepAlive as '%s' [%s]", keepAlive, session));
    }

    @Override
    public void modifySessionExpiryInterval(IMqttsnSession session, long sessionExpiryInterval) {
        getSessionBean(session).setSessionExpiryInterval(sessionExpiryInterval);
        logger.log(Level.INFO, String.format("setting session-sessionExpiryInterval as '%s' [%s]", sessionExpiryInterval, session));
    }

    @Override
    public void modifyMaxPacketSize(IMqttsnSession session, int maxPacketSize) {
        getSessionBean(session).setMaxPacketSize(maxPacketSize);
        logger.log(Level.INFO, String.format("setting session-maxPacketSize as '%s' [%s]", maxPacketSize, session));
    }

    protected void registerMetrics(IMqttsnRuntimeRegistry runtime){
        if(runtime.getMetrics() != null){
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_ACTIVE_REGISTRY_COUNT, "A count of the number of sessions marked in the 'ACTIVE' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.ACTIVE)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_DISCONNECTED_REGISTRY_COUNT, "A count of the number of sessions marked in the 'DISCONNECTED' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.DISCONNECTED)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_LOST_REGISTRY_COUNT, "A count of the number of sessions marked in the 'LOST' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.LOST)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_AWAKE_REGISTRY_COUNT, "A count of the number of sessions marked in the 'AWAKE' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.AWAKE)));
            runtime.getMetrics().registerMetric(new MqttsnSnapshotMetric(IMqttsnMetrics.SESSION_ASLEEP_REGISTRY_COUNT, "A count of the number of sessions marked in the 'ASLEEP' state resident in the runtime.",
                    IMqttsnMetrics.DEFAULT_MAX_SAMPLES, IMqttsnMetrics.DEFAULT_SNAPSHOT_TIME_MILLIS, () -> countSessions(MqttsnClientState.ASLEEP)));
        }
    }
}
