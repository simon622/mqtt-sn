#!/bin/bash

export mqtt_broker=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/mqtt_broker --region eu-west-1 | jq -r '.Parameter.Value')
export thing_id=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/thing_id --with-decryption --region eu-west-1 | jq -r '.Parameter.Value')
export thing_user=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/thing_user --with-decryption --region eu-west-1 | jq -r '.Parameter.Value')
export thing_password=$(aws ssm get-parameter --name /MQTT_SN_Gateway/iots/thing_password --with-decryption --region eu-west-1 | jq -r '.Parameter.Value')

cd "$(dirname "${BASH_SOURCE[0]}")"
envsubst '${mqtt_broker},${thing_id},${thing_user},${thing_password}' < run.sh.template > /home/ubuntu/mqtt-sn-gateway-udp/run.sh
chmod +x /home/ubuntu/mqtt-sn-gateway-udp/run.sh
