import org.slj.mqtt.sn.codec.AbstractProtectionScheme;
import org.slj.mqtt.sn.spi.IMqttsnSecurityService;

/**
 * @author Simon L Johnson
 */
public class ProtectionSchemaIT {



    public void testAll(){

//        logger.debug("------------REGRESSION TESTING CLIENT------------");
//        IMqttsnSecurityService mqttsnSecurityService = getRuntimeRegistry().getSecurityService();
//        try
//        {
//            logger.debug("TS1: Publish QoS-1 with 0x00 protection and incorrect flags 0x02, 0x01, 0x01");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x02,(byte)0x01,(byte)0x01});
//            throw new Exception("Regression testing client failed on TS1");
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        try
//        {
//            logger.debug("TS2: Publish QoS-1 with 0x00 protection and incorrect flags 0x03, 0x03, 0x03");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x03});
//            throw new Exception("Regression testing client failed on TS2");
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        logger.debug("TS3: Publish QoS-1 with 0x00 protection and flags 0x03, 0x00, 0x00");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        byte[] clientProtectionKeyHmac = new byte[] {
//                (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
//                (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e,
//                (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12,
//                (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12};
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyHmac);
//        publish("1", -1, false, "regressionTesting TS3");
//        Thread.sleep(100);
//
//        logger.debug("TS4: Publish QoS-1 with 0x00 protection and flags 0x03, 0x01, 0x01");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x01,(byte)0x01});
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS4");
//        Thread.sleep(100);
//
//        logger.debug("TS5: Publish QoS-1 with 0x00 protection and flags 0x0A, 0x02, 0x01");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0A,(byte)0x02,(byte)0x01});
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS5");
//        Thread.sleep(100);
//
//        logger.debug("TS6: Publish QoS-1 with 0x00 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS6");
//        Thread.sleep(100);
//
//        logger.debug("TS7: Publish QoS-1 with 0x40 protection and flags 0x03, 0x00, 0x00");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_64_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00});
//        byte[] clientProtectionKeyAes128 = new byte[] {
//                (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b};
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS7");
//        Thread.sleep(100);
//
//        logger.debug("TS8 (expected exception on gateway): Publish QoS-1 with 0x40 protection and flags 0x03, 0x00, 0x00 and wrong key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_64_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x00,(byte)0x00});
//        byte[] clientProtectionKeyAes128Wrong = new byte[] {
//                (byte)0x81,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b};
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128Wrong);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS8");
//        Thread.sleep(100);
//
//        logger.debug("TS9: Publish QoS-1 with 0x40 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_64_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS9");
//        Thread.sleep(100);
//
//        logger.debug("TS10: Publish QoS-1 with 0x41 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_64_192);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        byte[] clientProtectionKeyAes192 = new byte[] {
//                (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
//                (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54};
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes192);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS10");
//        Thread.sleep(100);
//
//        logger.debug("TS11: Publish QoS-1 with 0x42 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_64_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        byte[] clientProtectionKeyAes256 = new byte[] {
//                (byte)0x8d,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
//                (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e};
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS11");
//        Thread.sleep(100);
//
//        logger.debug("TS12: Publish QoS-1 with 0x43 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS12");
//        Thread.sleep(100);
//
//        logger.debug("TS13: Publish QoS-1 with 0x44 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_192);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes192);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS13");
//        Thread.sleep(100);
//
//        logger.debug("TS14: Publish QoS-1 with 0x45 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS14");
//        Thread.sleep(100);
//
//        try
//        {
//            logger.debug("TS15: Publish QoS-1 with 0x43 protection and incorrect flags 0x03, 0x03, 0x02");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_128);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        try
//        {
//            logger.debug("TS16: Publish QoS-1 with 0x44 protection and incorrect flags 0x03, 0x03, 0x02");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_192);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        try
//        {
//            logger.debug("TS17: Publish QoS-1 with 0x45 protection and incorrect flags 0x03, 0x03, 0x02");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_256);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        logger.debug("TS18: Publish QoS-1 with 0x01 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA3_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyHmac);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS18");
//        Thread.sleep(100);
//
//        logger.debug("TS19 (expected exception on gateway): Publish QoS-1 with 0x01 protection and flags 0x03, 0x03, 0x02 and wrong key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA3_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        byte[] clientProtectionKeyHmacWrong = new byte[] {
//                (byte)0x81,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
//                (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e,
//                (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12,
//                (byte)0x7a,(byte)0x9c,(byte)0x97,(byte)0x53,(byte)0x16,(byte)0xd4,(byte)0x27,(byte)0xcc,(byte)0x6e,(byte)0x00,(byte)0xdb,(byte)0xe5,(byte)0xc2,(byte)0xe3,(byte)0x89,(byte)0x12};
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyHmacWrong);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS19");
//        Thread.sleep(100);
//
//        logger.debug("TS20: Publish QoS-1 with 0x01 protection and flags 0x03, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA3_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyHmac);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS20");
//        Thread.sleep(100);
//
//        logger.debug("TS21: Publish QoS-1 with 0x02 protection and flags 0x07, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.CMAC_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS21");
//        Thread.sleep(100);
//
//        logger.debug("TS22: Publish QoS-1 with 0x02 protection and flags 0x03, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.CMAC_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS22");
//        Thread.sleep(100);
//
//        logger.debug("TS23: Publish QoS-1 with 0x02 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.CMAC_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS23");
//        Thread.sleep(100);
//
//        logger.debug("TS24 (expected exception on gateway): Publish QoS-1 with 0x02 protection and flags 0x07, 0x03, 0x02 and wrong key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.CMAC_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128Wrong);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS24");
//        Thread.sleep(100);
//
//        logger.debug("TS25: Publish QoS-1 with 0x03 protection and flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.CMAC_192);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes192);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS25");
//        Thread.sleep(100);
//
//        logger.debug("TS26: Publish QoS-1 with 0x04 protection and flags 0x03, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.CMAC_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS26");
//        Thread.sleep(100);
//
//        logger.debug("TS27: Publish QoS-1 with 0x00 protection and flags 0x0F, 0x03, 0x02 with 128-bit key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS27");
//        Thread.sleep(100);
//
//        logger.debug("TS28: Publish QoS-1 with 0x00 protection and flags 0x0F, 0x03, 0x02 with 128-bit key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes192);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS28");
//        Thread.sleep(100);
//
//        logger.debug("TS29: Publish QoS-1 with 0x00 protection and flags 0x0F, 0x03, 0x02 with 128-bit key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.HMAC_SHA256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS29");
//        Thread.sleep(100);
//
//        try
//        {
//            logger.debug("TS30: Publish QoS-1 with 0x46 protection and incorrect flags 0x03, 0x03, 0x02");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_128);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x03,(byte)0x03,(byte)0x02});
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        logger.debug("TS31: Publish QoS-1 with 0x46 protection and incorrect flags 0x07, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS31");
//        Thread.sleep(100);
//
//        logger.debug("TS32: Publish QoS-1 with 0x46 protection and incorrect flags 0x0F, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS32");
//        Thread.sleep(100);
//
//        logger.debug("TS33 (expected exception on gateway): Publish QoS-1 with 0x46 protection and incorrect flags 0x0F, 0x03, 0x02 and wrong key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_128);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128Wrong);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS33");
//        Thread.sleep(100);
//
//        try
//        {
//            logger.debug("TS34: Publish QoS-1 with 0x45 protection and incorrect flags 0x07, 0x03, 0x02 and wrong key");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_CCM_128_256);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//            mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//            logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//            publish("1", -1, false, "regressionTesting TS34");
//            Thread.sleep(100);
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        try
//        {
//            logger.debug("TS35: Publish QoS-1 with 0x47 protection and incorrect flags 0x07, 0x03, 0x02 and wrong key");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_192);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//            mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//            logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//            publish("1", -1, false, "regressionTesting TS35");
//            Thread.sleep(100);
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        logger.debug("TS36: Publish QoS-1 with 0x47 protection and incorrect flags 0x07, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_192);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes192);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS36");
//        Thread.sleep(100);
//
//        logger.debug("TS37: Publish QoS-1 with 0x48 protection and incorrect flags 0x07, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.AES_GCM_128_256);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS37");
//        Thread.sleep(100);
//
//        logger.debug("TS38: Publish QoS-1 with 0x49 protection and flags 0x07, 0x03, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.ChaCha20_Poly1305);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS38");
//        Thread.sleep(100);
//
//        logger.debug("TS39 (expected exception on gateway): Publish QoS-1 with 0x49 protection and flags 0x07, 0x03, 0x02 and wrong key");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.ChaCha20_Poly1305);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//        byte[] clientProtectionKeyAes256Wrong = new byte[] {
//                (byte)0x81,(byte)0x8c,(byte)0x0e,(byte)0x21,(byte)0x13,(byte)0x61,(byte)0x00,(byte)0x52,(byte)0x15,(byte)0xe9,(byte)0x02,(byte)0xcd,(byte)0xfa,(byte)0x4b,(byte)0x1e,(byte)0x0b,
//                (byte)0x9d,(byte)0x25,(byte)0xe4,(byte)0x97,(byte)0xea,(byte)0x71,(byte)0xd7,(byte)0x54,(byte)0x39,(byte)0x22,(byte)0x4e,(byte)0x55,(byte)0x80,(byte)0x4a,(byte)0xea,(byte)0x2e};
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256Wrong);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS39");
//        Thread.sleep(100);
//
//        try
//        {
//            logger.debug("TS40: Publish QoS-1 with 0x49 protection and flags 0x07, 0x03, 0x02 and wrong key");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.ChaCha20_Poly1305);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x07,(byte)0x03,(byte)0x02});
//            mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes128);
//            logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//            publish("1", -1, false, "regressionTesting TS40");
//            Thread.sleep(100);
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        try
//        {
//            logger.debug("TS41: Publish QoS-1 with 0x49 protection and incorrect flags 0x06, 0x03, 0x02");
//            mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.ChaCha20_Poly1305);
//            mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x06,(byte)0x03,(byte)0x02});
//            mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//            logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//            publish("1", -1, false, "regressionTesting TS41");
//            Thread.sleep(100);
//        }
//        catch(Exception ex)
//        {
//            logger.debug("Expected exception:",ex);
//        }
//
//        logger.debug("TS42: Publish QoS-1 with 0x49 protection and flags 0x0F, 0x00, 0x02");
//        mqttsnSecurityService.setProtectionScheme(AbstractProtectionScheme.ChaCha20_Poly1305);
//        mqttsnSecurityService.setProtectionFlags(new byte[] {(byte)0x0F,(byte)0x00,(byte)0x02});
//        mqttsnSecurityService.setProtectionKey(clientProtectionKeyAes256);
//        logger.debug(mqttsnSecurityService.getProtectionConfiguration());
//        publish("1", -1, false, "regressionTesting TS42");
//        Thread.sleep(100);
//
//        logger.debug("------------REGRESSION TESTING OK ------------");
//        throw new Exception("just to exit");

    }

}
