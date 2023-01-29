package org.slj.mqtt.sn.model;

import java.io.IOException;

public interface IAuthHandler {

  String getMechanism();

  boolean isComplete();

  byte[] handleChallenge(byte[] challenge) throws IOException;

}
