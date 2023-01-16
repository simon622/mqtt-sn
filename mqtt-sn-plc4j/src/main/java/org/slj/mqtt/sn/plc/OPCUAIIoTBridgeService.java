package org.slj.mqtt.sn.plc;

import org.apache.plc4x.java.PlcDriverManager;
import org.apache.plc4x.java.api.PlcConnection;
import org.apache.plc4x.java.api.exceptions.PlcConnectionException;
import org.apache.plc4x.java.api.messages.*;
import org.apache.plc4x.java.api.metadata.PlcConnectionMetadata;
import org.apache.plc4x.java.api.model.PlcSubscriptionHandle;
import org.slj.mqtt.sn.spi.AbstractMqttsnService;
import org.slj.mqtt.sn.spi.IMqttsnRuntimeRegistry;
import org.slj.mqtt.sn.spi.MqttsnException;
import org.slj.mqtt.sn.spi.MqttsnService;
import org.slj.mqtt.sn.utils.StringTable;
import org.slj.mqtt.sn.utils.StringTableWriters;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

@MqttsnService
public class OPCUAIIoTBridgeService  extends AbstractMqttsnService {

    protected PlcDriverManager driverManager;
    protected PlcConnection plcConnection;

    @Override
    public void start(IMqttsnRuntimeRegistry runtime) throws MqttsnException {
        super.start(runtime);

        driverManager = new PlcDriverManager();
        Set<String> drivers =  driverManager.listDrivers();
        for (String s : drivers){
            System.out.println(s);
        }

        String connectionString = "opcua:tcp://Simons-Laptop.broadband:53530/OPCUA/SimulationServer";
//        String connectionString = "opc.tcp://Simons-Laptop.broadband:53530/OPCUA/SimulationServer";
        try {
            plcConnection = driverManager.getConnection(connectionString);
            plcConnection.connect();
            if (!plcConnection.getMetadata().canSubscribe()) {
                logger.error("This connection doesn't support subscriptions.");
            } else {
                PlcConnectionMetadata metadata = plcConnection.getMetadata();

                debug(StringTable.fromBean(metadata));

//                PlcBrowseResponse browse =
//                        plcConnection.browseRequestBuilder().addQuery("Namespace", "ns=3").build().execute().get();
//
//                debug(StringTable.fromBean(browse));
//                PlcReadResponse read = plcConnection.readRequestBuilder().addItem("Namespace", "ns=3;").
//                        build().execute().get();
//                debug(StringTable.fromBean(read));

                final PlcSubscriptionRequest.Builder builder = plcConnection.subscriptionRequestBuilder();
//                builder.addChangeOfStateField("Value7", "ns=3;i=1006");
                builder.addChangeOfStateField("Value7", "ns=3;i=1007");

                PlcSubscriptionRequest subscriptionRequest = builder.build();
                final PlcSubscriptionResponse subscriptionResponse =
                        subscriptionRequest.execute().get();

                for (String subscriptionName : subscriptionResponse.getFieldNames()) {
                    final PlcSubscriptionHandle subscriptionHandle =
                            subscriptionResponse.getSubscriptionHandle(subscriptionName);
                    subscriptionHandle.register(plcSubscriptionEvent -> {
                                OPCUAIIoTBridgeService.debug(StringTable.fromBean(plcSubscriptionEvent));
                                Collection<String> s = plcSubscriptionEvent.getFieldNames();
                                for (String field : s) {
                                    System.out.println(field + "->" +  plcSubscriptionEvent.getDouble(field));
                                }
                            }
                        );
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void stop() throws MqttsnException {
        super.stop();

        if(plcConnection != null && plcConnection.isConnected()){
            try {
                plcConnection.close();
            } catch(Exception e){
                throw new MqttsnException(e);
            }
        }
    }


    public static void debug(StringTable st){
        System.out.println(StringTableWriters.writeStringTableAsASCII(st));
    }
}

