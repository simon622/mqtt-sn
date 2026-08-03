/*
 * Copyright (c) 2026 Ian Craggs
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

package org.slj.mqtt.sn.wire.version2_0.payload;

import org.slj.mqtt.sn.codec.MqttsnCodecException;
import org.slj.mqtt.sn.spi.IProtectionScheme;

public class ProtectionPacketFlags {
	private static final short AUTHENTICATION_TAG_LENGTH_MULTIPLIER = 2; //bytes
    
	public static final byte NO_CRYPTO_MATERIAL = 0; 
    public static final byte SHORT_CRYPTO_MATERIAL = 2; //bytes
    public static final byte LONG_CRYPTO_MATERIAL = 4; //bytes
    public static final byte VERYLONG_CRYPTO_MATERIAL = 12; //bytes
    public static final byte NO_MONOTONIC_COUNTER = 0; 
    public static final byte SHORT_MONOTONIC_COUNTER = 2; //bytes
    public static final byte LONG_MONOTONIC_COUNTER = 4; //bytes
    
	private byte authenticationTagLength = 0x00; //Reserved value by default to force a selection
    private byte cryptoMaterialLength = 0x03; //Reserved value by default to force a selection
    private byte monotonicCounterLength = 0x03; //Reserved value by default to force a selection
    
    private byte flagsAsByte=0x00;
	protected final IProtectionScheme scheme;
    
    public static ProtectionPacketFlags decodeProtectionPacketFlags(byte flags, IProtectionScheme protectionScheme) throws MqttsnCodecException
    {
    	return new ProtectionPacketFlags((byte)((((byte)(flags & 0xF0)) >> 4) & 0x0F), (byte)(((byte)(flags & 0x0C)) >> 2), (byte)((flags & 0x03)),protectionScheme);
    }
    
    public ProtectionPacketFlags(byte authenticationTagLength, byte cryptoMaterialLength, byte monotonicCounterLength, IProtectionScheme protectionScheme) throws MqttsnCodecException
    {
    	//-- set first: validation/decoding of the Authentication Tag Length below needs to
    	//-- consult the protection scheme's nominal tag size and authentication-only-ness.
    	this.scheme = protectionScheme;

    	//-- MQTT-SN 2.0 (CSD01) 3.17.2.3: only 14 of the 16 possible values are allowed -
    	//-- 0x2 and 0x3 are reserved and MUST NOT be used.
    	if(authenticationTagLength<0x00 || authenticationTagLength>0x0F)
    	{
    		throw new MqttsnCodecException("Invalid Authentication Tag Length flag! 0x"+ String.format("%02x", authenticationTagLength&0xff).toUpperCase());
    	}
    	if(authenticationTagLength==0x02 || authenticationTagLength==0x03)
    	{
    		throw new MqttsnCodecException("Authentication Tag Length 0x2/0x3 is reserved and MUST NOT be used");
    	}
    	//-- «If the Protection Scheme is not "Authentication Only" the Authentication Tag
    	//-- Length MUST be set to 0x1»[MQTT-SN-3.17.2.3-1] - ie. AEAD schemes always use their
    	//-- own nominal tag size, never a provider-defined (0x0) or truncated (0x4-0xF) one.
    	if(!protectionScheme.isAuthenticationOnly() && authenticationTagLength!=0x01)
    	{
    		throw new MqttsnCodecException("Authentication Tag Length MUST be 0x1 for non-Authentication-Only (AEAD) protection scheme "+protectionScheme.getName());
    	}
    	this.authenticationTagLength=authenticationTagLength;

    	//-- «Authentication Tag Length values between 0x4 and 0xF inclusive MUST only be used
    	//-- for the truncation of "Authentication Only" protection schemes»[MQTT-SN-3.17.2.3-5]
    	//-- and MUST NOT define a tag size bigger than the scheme's nominal tag
    	//-- size»[MQTT-SN-3.17.2.3-8].
    	if(protectionScheme.isAuthenticationOnly() && authenticationTagLength>=0x04
    			&& getAuthenticationTagLengthDecoded()>protectionScheme.getNominalTagLengthInBytes())
    	{
    		throw new MqttsnCodecException("Authentication Tag Length truncation cannot exceed the nominal tag size of protection scheme "+protectionScheme.getName());
    	}

    	if(cryptoMaterialLength>0x03)
    	{
    		throw new MqttsnCodecException("Invalid Crypto Material Length flag! 0x"+ String.format("%02x", cryptoMaterialLength&0xff).toUpperCase());
    	}
    	if(monotonicCounterLength>=0x03)
    	{
    		throw new MqttsnCodecException("Invalid Monotonic Counter Length flag! 0x"+ String.format("%02x", monotonicCounterLength&0xff).toUpperCase());
    	}
        this.cryptoMaterialLength=cryptoMaterialLength;
    	this.monotonicCounterLength=monotonicCounterLength;
    	this.flagsAsByte |= this.authenticationTagLength << 4;
    	this.flagsAsByte |= this.cryptoMaterialLength << 2;
    	this.flagsAsByte |= this.monotonicCounterLength & 0x03;
    }
    
    protected byte getFlagsAsByte(){
        return flagsAsByte;
    }
    
    public byte getAuthenticationTagLength() {
		return authenticationTagLength;
	}
	
    public byte getCryptoMaterialLength() {
		return cryptoMaterialLength;
	}

    public byte getMonotonicCounterLength() {
		return monotonicCounterLength;
	}
    
    /**
     * MQTT-SN 2.0 (CSD01) 3.17.2.3:
     * <ul>
     *   <li>0x0 - length is provider-defined; cannot be derived generically, throws.</li>
     *   <li>0x1 - length equals the protection scheme's own nominal tag size.</li>
     *   <li>0x4-0xF - truncated ("Authentication Only" schemes only): length = value * 16 bits
     *       = value * 2 bytes.</li>
     * </ul>
     */
    public short getAuthenticationTagLengthDecoded() {
    	if(authenticationTagLength==0x00)
    	{
    		throw new MqttsnCodecException("Authentication Tag Length 0x0 is provider-defined - " +
    				"its byte length cannot be derived generically and must be supplied out of " +
    				"band by the deployment");
    	}
    	if(authenticationTagLength==0x01)
    	{
    		return scheme.getNominalTagLengthInBytes();
    	}
    	return (short) (authenticationTagLength*AUTHENTICATION_TAG_LENGTH_MULTIPLIER);
    }
    
    public byte getCryptoMaterialLengthDecoded() {
    	switch(cryptoMaterialLength)
    	{
	    	case 0x01:
	    		return SHORT_CRYPTO_MATERIAL;
	    	case 0x02:
	    		return LONG_CRYPTO_MATERIAL;
	    	case 0x03:
	    		return VERYLONG_CRYPTO_MATERIAL;
	    	default:
	    		return NO_CRYPTO_MATERIAL;
    	}
    }

    public byte getMonotonicCounterLengthDecoded() {
    	switch(monotonicCounterLength)
    	{
	    	case 0x01:
	    		return SHORT_MONOTONIC_COUNTER;
	    	case 0x02:
	    		return LONG_MONOTONIC_COUNTER;
	    	case 0x03:
	    		throw new MqttsnCodecException("Invalid Crypto Material Length! 0x"+ String.format("%02x", cryptoMaterialLength&0xff).toUpperCase()); 
	    	default:
	    		return NO_CRYPTO_MATERIAL;
    	}
    }

	public IProtectionScheme getProtectionScheme() {
		return scheme;
	}

	@Override
    public String toString()
	{
    	StringBuilder sb = new StringBuilder("Flags= ");
    	sb.append("0x").append(String.format("%02x", authenticationTagLength&0xff).toUpperCase()).append(" (");
    	if(authenticationTagLength==0x00) sb.append("provider-defined");
    	else sb.append(getAuthenticationTagLengthDecoded()).append("bytes");
    	sb.append(")");
    	sb.append(", ").append("0x").append(String.format("%02x", cryptoMaterialLength&0xff).toUpperCase()).
    		append(" (").append(getCryptoMaterialLengthDecoded()).append("bytes").append(")");
    	sb.append(", ").append("0x").append(String.format("%02x", monotonicCounterLength&0xff).toUpperCase()).
    		append(" (").append(getMonotonicCounterLengthDecoded()).append("bytes").append(")");
    	return sb.toString();
    }
}
