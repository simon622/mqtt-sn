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

package org.slj.mqtt.sn.codec;

import org.slj.mqtt.sn.spi.IMqttsnCodec;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.wire.MqttsnWireUtils;
import org.slj.mqtt.sn.wire.AbstractMqttsnMessage;
import org.slj.mqtt.sn.wire.version1_2.payload.AbstractMqttsnMessageWithFlagsField;

/**
 * Base class for simple codec implementations. This version will only support message
 * types defined by the local abstract, the marker interfaces allow support for any
 * message implementations.
 *
 * @author Simon Johnson <simon622 AT gmail DOT com>
 */
public abstract class AbstractMqttsnCodec implements IMqttsnCodec {

    @Override
    public IMqttsnMessage decode(byte[] data)
            throws MqttsnCodecException, MqttsnUnsupportedVersionException {
        IMqttsnMessage msg = createInstance(data);
        validate(msg);
        return msg;
    }

    @Override
    public byte[] encode(IMqttsnMessage msg) throws MqttsnCodecException {
        if (!AbstractMqttsnMessage.class.isAssignableFrom(msg.getClass()))
            throw new MqttsnCodecException("unsupported message formats in codec");
        return ((AbstractMqttsnMessage) msg).encode();
    }

    protected void validateLengthEquals(byte[] data, int length) throws MqttsnCodecException {
        if (data.length != length) {
            throw new MqttsnCodecException(
                    String.format("invalid data length %s, must be %s bytes", data.length, length));
        }
    }

    protected void validateLengthGreaterThanOrEquals(byte[] data, int length) throws MqttsnCodecException {
        if (data.length < length) {
            throw new MqttsnCodecException(
                    String.format("invalid data length %s, must be gt or eq to %s bytes", data.length, length));
        }
    }

    @Override
    public int getQoS(IMqttsnMessage message, boolean convertMinus1) {
        return convertMinus1 ? Math.max(getQoS(message), 0) : getQoS(message);
    }

    @Override
    public String print(IMqttsnMessage message) throws MqttsnCodecException {
        return MqttsnWireUtils.toBinary(encode(message));
    }

    protected abstract IMqttsnMessage createInstance(byte[] data) throws MqttsnCodecException, MqttsnUnsupportedVersionException;

    protected abstract int getQoS(IMqttsnMessage message);
}
