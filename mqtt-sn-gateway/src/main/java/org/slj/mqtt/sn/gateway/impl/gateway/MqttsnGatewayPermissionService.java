package org.slj.mqtt.sn.gateway.impl.gateway;

import org.slj.mqtt.sn.gateway.spi.gateway.MqttsnGatewayOptions;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.spi.IMqttsnPermissionService;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;

import java.util.Set;

public class MqttsnGatewayPermissionService <U extends IMqttsnRuntimeRegistry>
        extends MqttsnService<U>  implements IMqttsnPermissionService  {

    @Override
    public boolean allowConnect(IMqttsnContext context, String clientId) throws MqttsnException {
        Set<String> allowedClientId = ((MqttsnGatewayOptions)registry.getOptions()).getAllowedClientIds();
        if(allowedClientId != null && !allowedClientId.isEmpty()){
            return allowedClientId.contains(MqttsnGatewayOptions.DEFAULT_CLIENT_ALLOWED_ALL)
                    || allowedClientId.contains(clientId);
        }
        return false;
    }

    @Override
    public boolean allowedToSubscribe(IMqttsnContext context, String topicPath) throws MqttsnException {
        return true;
    }

    @Override
    public int allowedMaximumQoS(IMqttsnContext context, String topicPath) throws MqttsnException {
        return 2;
    }

    @Override
    public boolean allowedToPublish(IMqttsnContext context, String topicPath, int size, int QoS) throws MqttsnException {
        return true;
    }
}
