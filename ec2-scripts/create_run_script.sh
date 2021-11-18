#!/bin/bash

#
# Copyright (c) 2021 Simon Johnson <simon622 AT gmail DOT com>
#
# Find me on GitHub:
# https://github.com/simon622
#
# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.
#

export mqtt_broker=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/mqtt_broker --region eu-west-1 | jq -r '.Parameter.Value')
export thing_id=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/thing_id --with-decryption --region eu-west-1 | jq -r '.Parameter.Value')
export thing_user=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/thing_user --with-decryption --region eu-west-1 | jq -r '.Parameter.Value')
export thing_password=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/thing_password --with-decryption --region eu-west-1 | jq -r '.Parameter.Value')

cd "$(dirname "${BASH_SOURCE[0]}")"
envsubst '${mqtt_broker},${thing_id},${thing_user},${thing_password}' < run.sh.template > /home/ubuntu/mqtt-sn-gateway-udp/run.sh
chmod +x /home/ubuntu/mqtt-sn-gateway-udp/run.sh
