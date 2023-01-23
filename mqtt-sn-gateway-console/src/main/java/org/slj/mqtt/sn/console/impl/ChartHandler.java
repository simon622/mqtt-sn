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

import be.ceau.chart.LineChart;
import be.ceau.chart.data.LineData;
import be.ceau.chart.options.LineOptions;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slj.mqtt.sn.console.chart.ChartJSUtils;
import org.slj.mqtt.sn.console.http.HttpConstants;
import org.slj.mqtt.sn.console.http.IHttpRequestResponse;
import org.slj.mqtt.sn.gateway.spi.GatewayMetrics;
import org.slj.mqtt.sn.impl.metrics.IMqttsnMetrics;
import org.slj.mqtt.sn.model.MqttsnMetricSample;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnRuntimeException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ChartHandler extends MqttsnConsoleAjaxRealmHandler {

    static final String CHART_ID = "chartId";
    static final String REQUEST_TYPE = "requestType";
    static final String REQUEST_TYPE_UPDATE = "update";
    static final String EPOCH = "epoch";

    static final String PUBLISH = "publishMetrics",
                        SESSION = "sessionMetrics",
                        NETWORK = "networkMetrics",
                        BACKEND = "backendMetrics",
                        SYSTEM = "systemMetrics";

    public ChartHandler(ObjectMapper mapper, IMqttsnRuntimeRegistry registry) {
        super(mapper, registry);
    }

    @Override
    protected void handleHttpGet(IHttpRequestResponse request) throws IOException {
        String chartId = request.getParameter(CHART_ID);
        String requestType = request.getParameter(REQUEST_TYPE);
        switch (chartId){
            case PUBLISH:
                if(!REQUEST_TYPE_UPDATE.equals(requestType)){
                    writePublish(request);
                } else {
                    String epoch = request.getParameter(EPOCH);
                    if(epoch == null){
                        sendBadRequestResponse(request, "epoch must be provided");
                    } else {
                        Long value = Long.valueOf(epoch);
                        writePublishUpdate(request, value);
                    }
                }
                break;
            case SESSION:
                if(!REQUEST_TYPE_UPDATE.equals(requestType)){
                    writeSession(request);
                } else {
                    String epoch = request.getParameter(EPOCH);
                    if(epoch == null){
                        sendBadRequestResponse(request, "epoch must be provided");
                    } else {
                        Long value = Long.valueOf(epoch);
                        writeSessionUpdate(request, value);
                    }
                }
                break;
            case NETWORK:
                if(!REQUEST_TYPE_UPDATE.equals(requestType)){
                    writeNetwork(request);
                } else {
                    String epoch = request.getParameter(EPOCH);
                    if(epoch == null){
                        sendBadRequestResponse(request, "epoch must be provided");
                    } else {
                        Long value = Long.valueOf(epoch);
                        writeNetworkUpdate(request, value);
                    }
                }
                break;
            case BACKEND:
                if(!REQUEST_TYPE_UPDATE.equals(requestType)){
                    writeBackendk(request);
                } else {
                    String epoch = request.getParameter(EPOCH);
                    if(epoch == null){
                        sendBadRequestResponse(request, "epoch must be provided");
                    } else {
                        Long value = Long.valueOf(epoch);
                        writeBackendUpdate(request, value);
                    }
                }
                break;
            case SYSTEM:
                if(!REQUEST_TYPE_UPDATE.equals(requestType)){
                    writeSystem(request);
                } else {
                    String epoch = request.getParameter(EPOCH);
                    if(epoch == null){
                        sendBadRequestResponse(request, "epoch must be provided");
                    } else {
                        Long value = Long.valueOf(epoch);
                        writeSystemUpdate(request, value);
                    }
                }
                break;
        }
    }

    private void writeSession(IHttpRequestResponse request) throws IOException {
        List<MqttsnMetricSample> active = getSamplesForMetric(IMqttsnMetrics.SESSION_ACTIVE_REGISTRY_COUNT);
        List<MqttsnMetricSample> disconnected = getSamplesForMetric(IMqttsnMetrics.SESSION_DISCONNECTED_REGISTRY_COUNT);
        List<MqttsnMetricSample> awake = getSamplesForMetric(IMqttsnMetrics.SESSION_AWAKE_REGISTRY_COUNT);
        List<MqttsnMetricSample> asleep = getSamplesForMetric(IMqttsnMetrics.SESSION_ASLEEP_REGISTRY_COUNT);
        List<MqttsnMetricSample> lost = getSamplesForMetric(IMqttsnMetrics.SESSION_LOST_REGISTRY_COUNT);

        long[] arr = ChartJSUtils.calculateLabels(active, disconnected, awake, asleep, lost);
        LineChart lineChart = new LineChart();
        LineOptions options = new LineOptions();
        lineChart.setOptions(options);
        lineChart.setData(new LineData()
                .addDataset(ChartJSUtils.createLineDataset("Active", ChartJSUtils.getColorForIndex(0), active))
                .addDataset(ChartJSUtils.createLineDataset("Disconnected", ChartJSUtils.getColorForIndex(1), disconnected))
                .addDataset(ChartJSUtils.createLineDataset("Awake", ChartJSUtils.getColorForIndex(2), awake))
                .addDataset(ChartJSUtils.createLineDataset("Asleep", ChartJSUtils.getColorForIndex(3), asleep))
                .addDataset(ChartJSUtils.createLineDataset("Lost", ChartJSUtils.getColorForIndex(4), lost))
                .addLabels(ChartJSUtils.timestampsToStr(arr)));
        writeJSONResponse(request, HttpConstants.SC_OK,
                ChartJSUtils.upgradeToV3AxisOptions(lineChart.toJson()).getBytes(StandardCharsets.UTF_8));
    }

    private void writeSessionUpdate(IHttpRequestResponse request, long since) throws IOException {
        List<MqttsnMetricSample> active = getSamplesForMetricSince(IMqttsnMetrics.SESSION_ACTIVE_REGISTRY_COUNT, since);
        List<MqttsnMetricSample> disconnected = getSamplesForMetricSince(IMqttsnMetrics.SESSION_DISCONNECTED_REGISTRY_COUNT, since);
        List<MqttsnMetricSample> awake = getSamplesForMetricSince(IMqttsnMetrics.SESSION_AWAKE_REGISTRY_COUNT, since);
        List<MqttsnMetricSample> asleep = getSamplesForMetricSince(IMqttsnMetrics.SESSION_ASLEEP_REGISTRY_COUNT, since);
        List<MqttsnMetricSample> lost = getSamplesForMetricSince(IMqttsnMetrics.SESSION_LOST_REGISTRY_COUNT, since);
        long[] arr = ChartJSUtils.calculateLabels(active, disconnected, awake, asleep, lost);
        String[] labels = ChartJSUtils.timestampsToStr(arr);
        Update u = new Update();
        u.labels = labels;
        u.data = new int [][]{
                ChartJSUtils.values(active),
                ChartJSUtils.values(disconnected),
                ChartJSUtils.values(awake),
                ChartJSUtils.values(asleep),
                ChartJSUtils.values(lost)
        };
        String json = mapper.writeValueAsString(u);
        writeJSONResponse(request, HttpConstants.SC_OK,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private void writePublish(IHttpRequestResponse request) throws IOException {
        List<MqttsnMetricSample> ingressSamples = getSamplesForMetric(IMqttsnMetrics.PUBLISH_MESSAGE_IN);
        List<MqttsnMetricSample> egressSamples = getSamplesForMetric(IMqttsnMetrics.PUBLISH_MESSAGE_OUT);
        long[] arr = ChartJSUtils.calculateLabels(ingressSamples, egressSamples);
        LineChart lineChart = new LineChart();
        LineOptions options = new LineOptions();
        lineChart.setOptions(options);
        lineChart.setData(new LineData()
                .addDataset(ChartJSUtils.createLineDataset("MQTT-SN Publish Received", ChartJSUtils.getColorForIndex(0), ingressSamples))
                .addDataset(ChartJSUtils.createLineDataset("MQTT-SN Publish Sent", ChartJSUtils.getColorForIndex(1), egressSamples))
                .addLabels(ChartJSUtils.timestampsToStr(arr)));
        writeJSONResponse(request, HttpConstants.SC_OK,
                ChartJSUtils.upgradeToV3AxisOptions(lineChart.toJson()).getBytes(StandardCharsets.UTF_8));
    }

    private void writePublishUpdate(IHttpRequestResponse request, long since) throws IOException {
        List<MqttsnMetricSample> in = getSamplesForMetricSince(IMqttsnMetrics.PUBLISH_MESSAGE_IN, since);
        List<MqttsnMetricSample> out = getSamplesForMetricSince(IMqttsnMetrics.PUBLISH_MESSAGE_OUT, since);
        List<MqttsnMetricSample> backend = getSamplesForMetricSince(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH, since);
        long[] arr = ChartJSUtils.calculateLabels(in, out, backend);
        String[] labels = ChartJSUtils.timestampsToStr(arr);
        Update u = new Update();
        u.labels = labels;
        u.data = new int [][]{
                ChartJSUtils.values(in), ChartJSUtils.values(out), ChartJSUtils.values(backend)
        };
        String json = mapper.writeValueAsString(u);
        writeJSONResponse(request, HttpConstants.SC_OK,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private void writeSystem(IHttpRequestResponse request) throws IOException {
        List<MqttsnMetricSample> in = getSamplesForMetric(IMqttsnMetrics.SYSTEM_VM_THREADS_USED);
        List<MqttsnMetricSample> out = getSamplesForMetric(IMqttsnMetrics.SYSTEM_VM_MEMORY_USED);
        List<MqttsnMetricSample> netRegSamples = getSamplesForMetric(IMqttsnMetrics.NETWORK_REGISTRY_COUNT);
        List<MqttsnMetricSample> messRegSamples = getSamplesForMetric(IMqttsnMetrics.MESSAGE_REGISTRY_COUNT);
        List<MqttsnMetricSample> dlqRegSamples = getSamplesForMetric(IMqttsnMetrics.DLQ_REGISTRY_COUNT);
        long[] arr = ChartJSUtils.calculateLabels(in, out, netRegSamples, messRegSamples, dlqRegSamples);
        LineChart lineChart = new LineChart();
        LineOptions options = new LineOptions();
        lineChart.setOptions(options);
        lineChart.setData(new LineData()
                .addDataset(ChartJSUtils.createLineDataset("VM Thread Count", ChartJSUtils.getColorForIndex(0), in))
                .addDataset(ChartJSUtils.createLineDataset("VM Memory Used (kb)", ChartJSUtils.getColorForIndex(1), out))
                .addDataset(ChartJSUtils.createLineDataset("Network Registry", ChartJSUtils.getColorForIndex(2), netRegSamples))
                .addDataset(ChartJSUtils.createLineDataset("Message Registry", ChartJSUtils.getColorForIndex(3), messRegSamples))
                .addDataset(ChartJSUtils.createLineDataset("Dead Letter Queue", ChartJSUtils.getColorForIndex(4), dlqRegSamples))
                .addLabels(ChartJSUtils.timestampsToStr(arr)));
        writeJSONResponse(request, HttpConstants.SC_OK,
                ChartJSUtils.upgradeToV3AxisOptions(lineChart.toJson()).getBytes(StandardCharsets.UTF_8));
    }

    private void writeSystemUpdate(IHttpRequestResponse request, long since) throws IOException {
        List<MqttsnMetricSample> in = getSamplesForMetricSince(IMqttsnMetrics.SYSTEM_VM_THREADS_USED, since);
        List<MqttsnMetricSample> out = getSamplesForMetricSince(IMqttsnMetrics.SYSTEM_VM_MEMORY_USED, since);
        List<MqttsnMetricSample> netRegSamples = getSamplesForMetricSince(IMqttsnMetrics.NETWORK_REGISTRY_COUNT, since);
        List<MqttsnMetricSample> messRegSamples = getSamplesForMetric(IMqttsnMetrics.MESSAGE_REGISTRY_COUNT);
        List<MqttsnMetricSample> dlqRegSamples = getSamplesForMetric(IMqttsnMetrics.DLQ_REGISTRY_COUNT);
        long[] arr = ChartJSUtils.calculateLabels(in, out, netRegSamples, messRegSamples, dlqRegSamples);
        String[] labels = ChartJSUtils.timestampsToStr(arr);
        Update u = new Update();
        u.labels = labels;
        u.data = new int [][]{
                ChartJSUtils.values(in), ChartJSUtils.values(out), ChartJSUtils.values(netRegSamples)
                , ChartJSUtils.values(messRegSamples), ChartJSUtils.values(dlqRegSamples)
        };
        String json = mapper.writeValueAsString(u);
        writeJSONResponse(request, HttpConstants.SC_OK,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private void writeNetwork(IHttpRequestResponse request) throws IOException {
        List<MqttsnMetricSample> netInSamples = getSamplesForMetric(IMqttsnMetrics.NETWORK_BYTES_IN);
        List<MqttsnMetricSample> netOutSamples = getSamplesForMetric(IMqttsnMetrics.NETWORK_BYTES_OUT);
        long[] arr = ChartJSUtils.calculateLabels(netInSamples, netOutSamples);
        LineChart lineChart = new LineChart();
        LineOptions options = new LineOptions();
        lineChart.setOptions(options);
        lineChart.setData(new LineData()
                .addDataset(ChartJSUtils.createLineDataset("Network Bytes Ingress", ChartJSUtils.getColorForIndex(0), netInSamples))
                .addDataset(ChartJSUtils.createLineDataset("Network Bytes Egress", ChartJSUtils.getColorForIndex(1), netOutSamples))
                .addLabels(ChartJSUtils.timestampsToStr(arr)));
        writeJSONResponse(request, HttpConstants.SC_OK,
                ChartJSUtils.upgradeToV3AxisOptions(lineChart.toJson()).getBytes(StandardCharsets.UTF_8));
    }

    private void writeNetworkUpdate(IHttpRequestResponse request, long since) throws IOException {
        List<MqttsnMetricSample> netInSamples = getSamplesForMetricSince(IMqttsnMetrics.NETWORK_BYTES_IN, since);
        List<MqttsnMetricSample> netOutSamples = getSamplesForMetricSince(IMqttsnMetrics.NETWORK_BYTES_OUT, since);
        long[] arr = ChartJSUtils.calculateLabels(netInSamples, netOutSamples);
        String[] labels = ChartJSUtils.timestampsToStr(arr);
        Update u = new Update();
        u.labels = labels;
        u.data = new int [][]{
                ChartJSUtils.values(netInSamples), ChartJSUtils.values(netOutSamples)
        };
        String json = mapper.writeValueAsString(u);
        writeJSONResponse(request, HttpConstants.SC_OK,
                json.getBytes(StandardCharsets.UTF_8));
    }

    private void writeBackendk(IHttpRequestResponse request) throws IOException {
        List<MqttsnMetricSample> publish = getSamplesForMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH);
        List<MqttsnMetricSample> recieve = getSamplesForMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_RECEIVE);
        List<MqttsnMetricSample> queuesize = getSamplesForMetric(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_QUEUE_SIZE);
        List<MqttsnMetricSample> expansion = getSamplesForMetric(GatewayMetrics.BACKEND_CONNECTOR_EXPANSION);
        long[] arr = ChartJSUtils.calculateLabels(publish, recieve, queuesize, expansion);
        LineChart lineChart = new LineChart();
        LineOptions options = new LineOptions();
        lineChart.setOptions(options);
        lineChart.setData(new LineData()
                .addDataset(ChartJSUtils.createLineDataset("Connector Out", ChartJSUtils.getColorForIndex(0), publish))
                .addDataset(ChartJSUtils.createLineDataset("Connector In", ChartJSUtils.getColorForIndex(1), recieve))
                .addDataset(ChartJSUtils.createLineDataset("Connector Queue", ChartJSUtils.getColorForIndex(2), queuesize))
                .addDataset(ChartJSUtils.createLineDataset("Connector Expansion", ChartJSUtils.getColorForIndex(3), expansion))
                .addLabels(ChartJSUtils.timestampsToStr(arr)));
        writeJSONResponse(request, HttpConstants.SC_OK,
                ChartJSUtils.upgradeToV3AxisOptions(lineChart.toJson()).getBytes(StandardCharsets.UTF_8));
    }

    private void writeBackendUpdate(IHttpRequestResponse request, long since) throws IOException {
        List<MqttsnMetricSample> publish = getSamplesForMetricSince(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH, since);
        List<MqttsnMetricSample> recieve = getSamplesForMetricSince(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_RECEIVE, since);
        List<MqttsnMetricSample> queuesize = getSamplesForMetricSince(GatewayMetrics.BACKEND_CONNECTOR_PUBLISH_QUEUE_SIZE, since);
        List<MqttsnMetricSample> expansion = getSamplesForMetricSince(GatewayMetrics.BACKEND_CONNECTOR_EXPANSION, since);
        long[] arr = ChartJSUtils.calculateLabels(publish, recieve, queuesize, expansion);
        String[] labels = ChartJSUtils.timestampsToStr(arr);
        Update u = new Update();
        u.labels = labels;
        u.data = new int [][]{
                ChartJSUtils.values(publish), ChartJSUtils.values(recieve), ChartJSUtils.values(queuesize), ChartJSUtils.values(expansion)
        };
        String json = mapper.writeValueAsString(u);
        writeJSONResponse(request, HttpConstants.SC_OK,
                json.getBytes(StandardCharsets.UTF_8));
    }

    protected List<MqttsnMetricSample> getSamplesForMetric(String metricName){
        return getSamplesForMetricSince(metricName, null);
    }
    protected List<MqttsnMetricSample> getSamplesForMetricSince(String metricName, Long since){
        try {
            since = since == null ? System.currentTimeMillis() - 60000 : since;
            return registry.getMetrics().getMetric(metricName).getSamples(new Date(since));
        } catch(MqttsnRuntimeException e){
            return Collections.emptyList();
        }
    }

    class Update {
        public String[] labels;
        public int[][] data;
    }
}