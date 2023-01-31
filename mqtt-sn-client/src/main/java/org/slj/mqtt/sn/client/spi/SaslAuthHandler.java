package org.slj.mqtt.sn.client.spi;

import java.util.Map;
import javax.security.auth.callback.CallbackHandler;
import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import org.slj.mqtt.sn.model.IAuthHandler;

public class SaslAuthHandler implements IAuthHandler {

  private final SaslClient saslClient;
  private final String mechanism;

  public SaslAuthHandler(String mechanism, String authorisationId, String serverName, Map<String,?> props, CallbackHandler callbackHandler) throws SaslException {
    this.mechanism = mechanism;
    String[] mechanisms = {mechanism};
    saslClient =  Sasl.createSaslClient(
        mechanisms,
        authorisationId,
        "MQTT-SN",
        serverName,
        props,
        callbackHandler);
  }

  @Override
  public String getMechanism() {
    return mechanism;
  }

  @Override
  public boolean isComplete() {
    return saslClient.isComplete();
  }

  @Override
  public byte[] handleChallenge(byte[] challenge) throws SaslException {
    if(!saslClient.isComplete()){
      return saslClient.evaluateChallenge(challenge);
    }
    throw new SaslException("Challenge is complete");
  }

}
