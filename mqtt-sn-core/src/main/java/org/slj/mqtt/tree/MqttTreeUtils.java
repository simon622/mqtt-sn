package org.slj.mqtt.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;


/**
 * The basis of this method was token from Apache commons'
 * org.apache.commons.lang3.StringUtils please refer to;
 *
 * org.apache.commons.lang3.StringUtils.splitPreserveAllTokens.
 *
 * Pursuant to the licensing terms (Apache2) this code has been modified from
 * the original version, as allowed by the license; subject to the
 * existence of such a NOTICE. ( Clause 4(b) ). I would like to thank
 * the original authors for their work.
 *
 */
public class MqttTreeUtils {

    public static final String SPLIT = "/";
    public static final char SPLIT_C = '/';

    private final static String[] EMPTY = new String[0];

    public static String[] splitPath(final String str){
        return splitPathRetainingSplitChar(str, SPLIT_C, SPLIT);
    }

    public static String[] splitPathRetainingSplitChar(final String str, char splitC, String splitS){
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY;
        }
        final List<String> list = new ArrayList<>();
        int i = 0;
        int start = 0;
        boolean match = false;
        while (i < len) {
            if (str.charAt(i) == splitC) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                }
                list.add(splitS);
                start = ++i;
                continue;
            }
            match = true;
            i++;
        }
        if (match) {
            list.add(str.substring(start, i));
        }
        return list.toArray(EMPTY);
    }


    /**
     *  The character data in a UTF-8 Encoded String MUST be well-formed UTF-8 as defined by the Unicode
     *  specification [Unicode] and restated in RFC 3629 [RFC3629]. In particular, the character data MUST NOT
     *  include encodings of code points between U+D800 and U+DFFF
     *
     */
    public static boolean validStringData(String data, boolean allowNull){
        if(data == null && !allowNull){
            return false;
        }
        if(data == null && allowNull){
            return true;
        }
        if(data.length() > MqttTreeConstants.UNSIGNED_MAX_16){
            return false;
        }
        for (int i = 0; i < data.length(); i++) {
            char c = data.charAt(i);
            if(c >= MqttTreeConstants.MIN_HIGH_UTF &&
                    c <= MqttTreeConstants.MAX_HIGH_UTF) return false;

            if(c >= MqttTreeConstants.MIN_CONTROL1_UTF &&
                    c <= MqttTreeConstants.MAX_CONTROL1_UTF) return false;

            if(c >= MqttTreeConstants.MIN_CONTROL2_UTF &&
                    c <= MqttTreeConstants.MAX_CONTROL2_UTF) return false;
        }
        return true;
    }

    public static boolean isValidPublishTopic(String topicPath){
        return isValidPublishTopic(topicPath, MqttTreeConstants.MAX_TOPIC_LENGTH);
    }

    public static boolean isValidPublishTopic(String topicPath, int maxLength){
        return isValidTopicInternal(topicPath, maxLength, false);
    }

    public static boolean isValidSubscriptionTopic(String topicPath, int maxLength){
        boolean valid = isValidTopicInternal(topicPath, maxLength, true);
        if(valid && topicPath.contains(MqttTreeConstants.MULTI_LEVEL_WILDCARD)) {
            valid &= topicPath.endsWith(MqttTreeConstants.MULTI_LEVEL_WILDCARD);
            if(topicPath.length() > 1){
                valid &= topicPath.charAt(topicPath.indexOf(MqttTreeConstants.MULTI_LEVEL_WILDCARD) - 1)
                        == MqttTreeConstants.PATH_SEP;
            }
        }

        if(valid && topicPath.contains(MqttTreeConstants.SINGLE_LEVEL_WILDCARD)) {
            if(topicPath.length() > 1){
                char[] c = topicPath.toCharArray();
                for(int i = 0; i < c.length; i++){
                    if(c[i] == MqttTreeConstants.SINGLE_WILDCARD_CHAR){
                        //check the preceeding char
                        if(i > 0){
                            valid &= c[i - 1] == MqttTreeConstants.PATH_SEP;
                        }
                        //check the next char
                        if(c.length > (i + 1)){
                            valid &= c[i + 1] == MqttTreeConstants.PATH_SEP;
                        }
                    }
                }
            }
        }

        return valid;
    }
    public static boolean isValidSubscriptionTopic(String topicPath){
        return isValidSubscriptionTopic(topicPath, MqttTreeConstants.MAX_TOPIC_LENGTH);
    }

    private static boolean isValidTopicInternal(String topicPath, int maxLength, boolean allowWild){
        boolean valid = topicPath != null && topicPath.length() > 0 &&
                topicPath.length() < Math.min(maxLength, MqttTreeConstants.MAX_TOPIC_LENGTH) &&
                topicPath.indexOf(MqttTreeConstants.UNICODE_ZERO) == -1;
        if(valid){
            valid &= MqttTreeUtils.validStringData(topicPath, false);
        }
        if(valid && !allowWild){
            valid &= !topicPath.contains(MqttTreeConstants.SINGLE_LEVEL_WILDCARD) &&
                    !topicPath.contains(MqttTreeConstants.MULTI_LEVEL_WILDCARD);
        }
        return valid;
    }

    public static void main(String[] args) {
        System.err.println(Arrays.toString(splitPathRetainingSplitChar("/this/is/my/string//", '/', "/")));
    }

    public static String generateRandomTopic(int segments){
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < segments; i++){
            if(i == 0) sb.append("/");
            int r = ThreadLocalRandom.current().nextInt(0, 1000);
            sb.append(r);
            sb.append("/");
        }
        return sb.toString();
    }
}
