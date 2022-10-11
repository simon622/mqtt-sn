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

package org.slj.mqtt.sn.console.chart;

import be.ceau.chart.BarChart;
import be.ceau.chart.Chart;
import be.ceau.chart.color.Color;
import be.ceau.chart.data.BarData;
import be.ceau.chart.dataset.BarDataset;
import be.ceau.chart.dataset.LineDataset;
import be.ceau.chart.enums.BorderCapStyle;
import be.ceau.chart.enums.BorderJoinStyle;
import be.ceau.chart.options.elements.Fill;
import org.slj.mqtt.sn.model.MqttsnMetricSample;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.LongStream;

public class ChartJSUtils {

    public static long[] calculateLabels(List<MqttsnMetricSample>... lists){
        List<MqttsnMetricSample> all = mergeLists(lists);
        if(all.size() == 0) return new long[0];
        Collections.sort(all, MqttsnMetricSample.TIMESTAMP);
        long min = all.get(0).getTimestamp();
        long max = all.get(all.size() - 1).getTimestamp();
        long steps = countSteps(lists);
        if(steps == 0) return new long[0];
        long stepSize = (max - min) / steps;
        long[] a = LongStream.iterate(min, i -> i + stepSize).limit(steps).toArray();

        Set<Long> l = new HashSet<>();
        for (long g:
             a) {
            l.add(g);
        }
        if(l.size() != a.length){
            System.err.println(l.size() + " != "+a.length+" found clashing bag in sample!!");
            System.err.println(Arrays.toString(a));
        }
        return a;
    }
    public static List<MqttsnMetricSample> mergeLists(List<MqttsnMetricSample>[] lists){
        List<MqttsnMetricSample> l = new ArrayList<>();
        for (List<MqttsnMetricSample> sample : lists) {
            l.addAll(sample);
        }
        return l;
    }

    //NO NO NO - this isnt right... spend 5 mins thinking about thiss
    public static final int countSteps(List<MqttsnMetricSample>[] lists){
        return lists[0].size();
    }

    public static String upgradeToV3AxisOptions(String json){
        return json.replaceAll("\"options\" : \\{ \\}", "\"options\": {" +
                "    \"responsive\": \"true\", \"scales\": {\"x\": {\"display\": false}, \"y\": {\"min\": 0}}}");

//        String anno = "annotation: {" +
//                "      annotations: [{" +
//                "        type: 'line'," +
//                "        mode: 'horizontal'," +
//                "        scaleID: 'y-axis-0'," +
//                "        value: 5," +
//                "        borderColor: 'rgb(75, 192, 192)'," +
//                "        borderWidth: 4," +
//                "        label: {" +
//                "          enabled: false," +
//                "          content: 'Test label'" +
//                "        }" +
//                "      }]" +
//                "    }";
//        return json.replaceAll("", "");
    }

    public static LineDataset createLineDataset(String name, Color color, List<MqttsnMetricSample> data) {
        return new LineDataset()
                .setLabel(name)
                .setFill(new Fill(true))
                .setLineTension(0.2f)
                .setBackgroundColor(new Color(color.getR(), color.getG(), color.getB(), 0.1))
                .setBorderColor(color)
                .setBorderCapStyle(BorderCapStyle.BUTT)
                .setBorderDashOffset(0.0f)
                .setBorderJoinStyle(BorderJoinStyle.MITER)
                .addPointRadius(1)
                .addPointHitRadius(10)
                .setBorderWidth(1)
                .setSpanGaps(false)
                .setData(values(data));
    }

    public static String[] timestampsToStr(long[] timestamps){
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        String[] a = new String[timestamps.length];
        for (int i = 0; i< timestamps.length;i++){
            a[i] = sdf.format(new Date(timestamps[i]));
        }
        return a;
    }

    public static int[] values(List<MqttsnMetricSample> samples){
        int[] a = new int[samples.size()];
        for (int i = 0; i< a.length;i++){
            a[i] = (int) samples.get(i).getLongValue();
        }
        return a;
    }

    public static Chart createBarChart(String type){
        BarDataset dataset = new BarDataset()
                .setLabel("sample chart")
                .setData(65, 59, 80, 81, 56, 55, 40)
                .addBackgroundColors(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.ORANGE, Color.GRAY, Color.BLACK)
                .setBorderWidth(2);

        BarData data = new BarData()
                .addLabels("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                .addDataset(dataset);
        return new BarChart(data);
    }

    static Color[] colors = new Color[]{
            new Color(36, 73, 146), new Color(76, 129, 120), new Color(248, 53, 68), Color.YELLOW, Color.ORANGE, Color.GRAY, Color.BLACK
    };

    public static Color getColorForIndex(int idx){
        return colors[idx];
    }
}
