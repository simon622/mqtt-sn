package org.slj.mqtt.sn.utils.tree;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;


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
public class MqttSubscriptionTreeUtils {

    private final static String[] EMPTY = new String[0];

    public static String[] splitPathRetainingSplitChar(final String str, char separatorChar){
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
            if (str.charAt(i) == separatorChar) {
                if (match) {
                    list.add(str.substring(start, i));
                    match = false;
                }
                list.add(separatorChar + "");
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

    public static void main(String[] args) {
        System.err.println(Arrays.toString(splitPathRetainingSplitChar("/this/is/my/string//", '/')));
    }
}
