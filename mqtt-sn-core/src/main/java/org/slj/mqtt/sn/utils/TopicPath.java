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

package org.slj.mqtt.sn.utils;

import java.io.Serializable;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TopicPath {

//    static final String TOPIC_REGX = "^((^\\/?|\\/)([A-Za-z0-9\\.\\-\\$\\:_]+|$)?)*$";
    static final String SUBCRIPTION_REGX = "^((^\\/?|\\/)([A-Za-z0-9\\.\\-\\$\\:_]+|\\+|#$)?)*$";
    static final String WILDCARD = "#";
    static final String WILDSEG = "+";
    static final String PATHSEP = "/";

    private Topic topic;

    public TopicPath(String topicPath){
        this.topic = new Topic(topicPath);
    }

    public boolean matches(String topicPath) throws ParseException {
        Topic matchTopic = new Topic(topicPath);
        List<Token> msgTokens = topic.getTokens();
        List<Token> subscriptionTokens = matchTopic.getTokens();
        int i = 0;
        for (; i < subscriptionTokens.size(); i++) {
            Token subToken = subscriptionTokens.get(i);
            if (subToken != Token.MULTI && subToken != Token.SINGLE) {
                if (i >= msgTokens.size()) {
                    return false;
                }
                Token msgToken = msgTokens.get(i);
                if (!msgToken.equals(subToken)) {
                    return false;
                }
            } else {
                if (subToken == Token.MULTI) {
                    return true;
                }
                if (subToken == Token.SINGLE) {
                }
            }
        }
        return i == msgTokens.size();
    }

    public static boolean isWild(String topicPath){
        return topicPath != null && topicPath.contains(WILDCARD) ||topicPath.contains(WILDSEG) ;
    }

    public static boolean isValidTopic(String topicPath, int maxLength){
        return topicPath != null && topicPath.trim().length() > 0 && topicPath.trim().length() < maxLength;
//                TOPIC_REGX.matches(topicPath);
    }

    public static boolean isValidSubscription(String topicPath, int maxLength){
        return topicPath != null && topicPath.trim().length() > 0 && topicPath.trim().length() < maxLength;
//            && SUBCRIPTION_REGX.matches(topicPath);
    }

    public String toString(){
        return topic.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TopicPath topicPath = (TopicPath) o;
        return Objects.equals(topic, topicPath.topic);
    }

    @Override
    public int hashCode() {
        return Objects.hash(topic);
    }

    private static class Token {

        static final Token EMPTY = new Token("");
        static final Token MULTI = new Token(WILDCARD);
        static final Token SINGLE = new Token(WILDSEG);
        final String name;

        protected Token(String s) {
            name = s;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Token other = (Token) obj;
            if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static class Topic implements Serializable {

        private final String topicPath;
        private transient List<Token> tokens;

        public Topic(String topic) {
            this.topicPath = topic;
        }

        public List<Token> getTokens() throws ParseException {
            if (tokens == null) {
                tokens = parseTopic(topicPath);
            }
            return tokens;
        }

        private List<Token> parseTopic(String topic) throws ParseException {
            List<Token> res = new ArrayList<>();
            String[] arr = topic.split(PATHSEP);
            if (arr.length == 0) {
                res.add(Token.EMPTY);
            }
            if (topic.endsWith(PATHSEP)) {
                String[] newArr = new String[arr.length + 1];
                System.arraycopy(arr, 0, newArr, 0, arr.length);
                newArr[arr.length] = "";
                arr = newArr;
            }
            for (int i = 0; i < arr.length; i++) {
                String s = arr[i];
                if (s.isEmpty()) {
                    res.add(Token.EMPTY);
                } else if (s.equals(WILDCARD)) {
                    if (i != arr.length - 1) {
                        throw new ParseException("bad topic format - the multi symbol (#) has to be the last one after a separator", i);
                    }
                    res.add(Token.MULTI);
                } else if (s.contains(WILDCARD)) {
                    throw new ParseException("bad topic format - invalid subtopic name: " + s, i);
                } else if (s.equals(WILDSEG)) {
                    res.add(Token.SINGLE);
                } else if (s.contains(WILDSEG)) {
                    throw new ParseException("bad topic format - invalid subtopic name: " + s, i);
                } else {
                    res.add(new Token(s));
                }
            }
            return res;
        }

        @Override
        public String toString() {
            return topicPath;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Topic other = (Topic) obj;
            return Objects.equals(this.topicPath, other.topicPath);
        }

        @Override
        public int hashCode() {
            return topicPath.hashCode();
        }
    }

    public static final void main(String[] args){
    }
}