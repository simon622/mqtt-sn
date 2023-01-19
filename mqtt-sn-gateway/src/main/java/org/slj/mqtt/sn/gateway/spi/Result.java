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

package org.slj.mqtt.sn.gateway.spi;

import org.slj.mqtt.sn.MqttsnConstants;
import org.slj.mqtt.sn.spi.IMqttsnMessage;

public class Result {

    protected STATUS status;
    protected String message;
    protected int returnCode;

    public enum STATUS {
        SUCCESS, ERROR, NOOP
    }

    public Result(STATUS status) {
        this.status = status;
        if(status == STATUS.SUCCESS){
            returnCode = MqttsnConstants.RETURN_CODE_ACCEPTED;
        } else if (status == STATUS.ERROR){
            returnCode = MqttsnConstants.RETURN_CODE_SERVER_UNAVAILABLE;
        }
    }

    public Result(STATUS status, int returnCode) {
        this.status = status;
        this.returnCode = returnCode;
    }

    public Result(STATUS status, int returnCode, String message) {
        this.status = status;
        this.returnCode = returnCode;
        this.message = message;
    }

    public Result(STATUS status, String message) {
        this.status = status;
        this.message = message;
    }

    public STATUS getStatus() {
        return status;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(int returnCode) {
        this.returnCode = returnCode;
    }

    public boolean isError(){
        return STATUS.ERROR == status;
    }

    @Override
    public String toString() {
        return "Result{" +
                "status=" + status +
                ", message='" + message + '\'' +
                ", returnCode=" + returnCode +
                '}';
    }
}
