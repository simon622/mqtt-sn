package org.slj.mqtt.sn.console;

import org.slj.mqtt.sn.cloud.*;
import org.slj.mqtt.sn.gateway.connector.custom.CustomMqttBrokerConnector;
import org.slj.mqtt.sn.gateway.impl.connector.LoopbackMqttsnConnector;

import java.util.List;

public class NoCloudServiceImpl implements IMqttsnCloudService {

    @Override
    public MqttsnCloudToken authorizeCloudAccount(final MqttsnCloudAccount account) throws MqttsnCloudServiceException {
        throw new MqttsnCloudServiceException("cloud not available");
    }

    @Override
    public List<MqttsnConnectorDescriptor> getAvailableConnectors() throws MqttsnCloudServiceException {
        return List.of(LoopbackMqttsnConnector.DESCRIPTOR, CustomMqttBrokerConnector.DESCRIPTOR);
    }

    @Override
    public List<ProtocolBridgeDescriptor> getAvailableBridges() throws MqttsnCloudServiceException {
        return List.of();
    }

    @Override
    public int getConnectedServiceCount() throws MqttsnCloudServiceException {
        return 0;
    }

    @Override
    public boolean hasCloudConnectivity() {
        return false;
    }

    @Override
    public boolean isAuthorized() {
        return false;
    }

    @Override
    public boolean isVerified() {
        return false;
    }

    @Override
    public MqttsnCloudToken registerAccount(final String emailAddress, final String firstName, final String lastName, final String companyName, final String macAddress, final String contextId) throws MqttsnCloudServiceException {
        throw new MqttsnCloudServiceException("cloud not available");
    }

    @Override
    public MqttsnCloudAccount readAccount() throws MqttsnCloudServiceException {
        throw new MqttsnCloudServiceException("cloud not available");
    }

    @Override
    public void setToken(final MqttsnCloudToken token) throws MqttsnCloudServiceException {
        throw new MqttsnCloudServiceException("cloud not available");
    }

    @Override
    public void sendCloudEmail(final MqttsnCloudEmail email) throws MqttsnCloudServiceException {
        throw new MqttsnCloudServiceException("cloud not available");
    }
}
