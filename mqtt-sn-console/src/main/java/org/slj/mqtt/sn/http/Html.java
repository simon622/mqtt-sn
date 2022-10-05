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

package org.slj.mqtt.sn.http;

import java.util.Collections;

public class Html {

    public static final String STYLE_NORMAL = "normal";
    public static final String STYLE_ITALIC = "italic";
    public static final String STYLE_OBLIQUE = "oblique";

    public static final String Body = "<body>";
    public static final String Body_E = "</body>";
    public static final String Head = "<head>";
    public static final String Head_E = "</head>";
    public static final String Pre = "<pre>";
    public static final String Pre_E = "</pre>";

    public static final String DARK_GRAY = "#b0b0b0";
    public static final String LIGHT_GRAY = "#f0f0f0";
    public static final String RED = "#FF0000";
    public static final String GREEN = "#0B610B";
    public static final String ORANGE = "#FF9033";
    public static final String BLACK = "#000";
    public static final String WHITE = "#FFFFFF";

    public static String css(String url) {
        return String.format("<link rel=\"stylesheet\" type=\"text/css\" href=\"%s\">", url);
    }

    public static String inlineCss(String css) {
        return String.format("<style>%s</style>", css);
    }

    public static String heading(String title) {
        return String.format("<h2>%s</h2>", title);
    }

    public static String subheading(String title) {
        return String.format("<h4>%s</h4>", title);
    }

    public static String linebreak(int count) {
        return String.join("", Collections.nCopies(count, "<br/>"));
    }

    public static String line() {
        return "<hr/>";
    }

    public static String span(String text, String colorCode, boolean embolden) {
        return span(text, colorCode, embolden, WHITE, false);
    }

    public static String span(String text, String colorCode, boolean embolden, String backgroundColor) {
        return span(text, colorCode, embolden, backgroundColor, false);
    }

    public static String span(String text, String colorCode, boolean embolden, String backgroundColor, boolean italic) {
        String weight = embolden ? "bold" : "normal";
        return String.format("<span style=\"%s;%s;%s;%s\">%s</span>",
                String.format("color: %s", colorCode),
                String.format("font-weight: %s", weight),
                String.format("background-color: %s", backgroundColor),
                String.format("font-style: %s", italic ? STYLE_ITALIC : STYLE_NORMAL),
                text == null ? "" : text);
    }

    public static String underline(String text) {
        return String.format("<u>%s</u>", text);
    }

    public static String italic(String text) {
        return String.format("<i>%s</i>", text);
    }

    public static String strong(String text) {
        return String.format("<strong>%s</strong>", text);
    }

    public static String small(String text) {
        return String.format("<span style=\"font-size: small;\">%s</span>", text);
    }

    public static String smaller(String text) {
        return String.format("<span style=\"font-size: smaller;\">%s</span>", text);
    }

    public static String superscript(String text) {
        return superscript(text, false);
    }

    public static String superscript(String text, boolean brackets) {
        return brackets ? String.format("<sup>(%s)</sup>", text) : String.format("<sup>%s</sup>", text);
    }

    public static String div(int margin, String content) {
        return String.format("<div style=\"margin: %spx\">%s</div>", margin, content);
    }

    public static String paragraph(int margin, String content) {
        return String.format("<p style=\"margin: %spx\">%s</p>", margin, content);
    }

    public static String img(String src, String width, String height) {
        String mask = "<img src=\"%s\" width=\"%s\" height=\"%s\"/>";
        return String.format(mask, src, width, height);
    }

    public static String anchor(String link, String label, boolean targetBlank) {
        String mask = targetBlank ? "<a href=\"%s\" target=\"_blank\">%s</a>" : "<a href=\"%s\">%s</a>";
        return String.format(mask,link, label);
    }

    public static String linespace(int count) {
        return String.join("", Collections.nCopies(count, "&nbsp;"));
    }

    public static String getErrorMessage(int code, String message){
        return String.format("<h1>Http %s</h1><hr>%s", code, message);
    }
}