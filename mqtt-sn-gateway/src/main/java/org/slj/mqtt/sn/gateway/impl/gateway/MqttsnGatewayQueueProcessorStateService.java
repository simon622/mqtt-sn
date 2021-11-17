package org.slj.mqtt.sn.gateway.impl.gateway;

import org.slj.mqtt.sn.gateway.spi.gateway.IMqttsnGatewayRuntimeRegistry;
import org.slj.mqtt.sn.model.IMqttsnContext;
import org.slj.mqtt.sn.model.IMqttsnSessionState;
import org.slj.mqtt.sn.model.MqttsnClientState;
import org.slj.mqtt.sn.spi.IMqttsnMessage;
import org.slj.mqtt.sn.spi.IMqttsnQueueProcessorStateService;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;
import org.slj.mqtt.sn.utils.MqttsnUtils;

import java.util.logging.Level;

public class MqttsnGatewayQueueProcessorStateService extends MqttsnService<IMqttsnGatewayRuntimeRegistry>
        implements IMqttsnQueueProcessorStateService {

    @Override
    public boolean canReceive(IMqttsnContext context) throws MqttsnException {
        IMqttsnSessionState state = getRegistry().getGatewaySessionService().getSessionState(context, false);
        return state != null && MqttsnUtils.in(state.getClientState() , MqttsnClientState.CONNECTED, MqttsnClientState.AWAKE);
    }

    @Override
    public void queueEmpty(IMqttsnContext context) throws MqttsnException {

        IMqttsnSessionState state = getRegistry().getGatewaySessionService().getSessionState(context, false);
        logger.log(Level.INFO, String.format("notified that the queue is empty, post process state is - [%s]", state));
        if(state != null){
            if(MqttsnUtils.in(state.getClientState() , MqttsnClientState.AWAKE)){
                logger.log(Level.INFO, String.format("notified that the queue is empty, putting device back to sleep and sending ping-resp - [%s]", context));
                //-- need to transition the device back to sleep
                getRegistry().getGatewaySessionService().disconnect(state, state.getKeepAlive());
                //-- need to send the closing ping-resp
                IMqttsnMessage pingResp = getRegistry().getMessageFactory().createPingresp();
                getRegistry().getTransport().writeToTransport(getRegistry().getNetworkRegistry().getContext(context), pingResp);
            }
        }
    }
}
