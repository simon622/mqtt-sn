package org.slj.mqtt.sn.utils.tree;

import java.util.ArrayList;
import java.util.List;


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
public class PathTreeUtils {

    private final static String[] EMPTY = new String[0];

    public static String[] splitPathWithoutRegex(final String str, char separatorChar, boolean preserve){
        if (str == null) {
            return null;
        }
        final int len = str.length();
        if (len == 0) {
            return EMPTY;
        }
        final List<String> tokens = new ArrayList<>();
        int i = 0, start = 0;
        boolean match = false, lMat = false;
        while (i < len) {
            if (str.charAt(i)
                    == separatorChar) {
                if (match || preserve) {
                    tokens.add(str.substring(start, i));
                    match = false;
                    lMat = true;
                }
                start = ++i;
                continue;
            }
            lMat = false;
            match = true;
            i++;
        }
        if (match || preserve && lMat) {
            tokens.add(str.substring(start, i));
        }
        return tokens.toArray(EMPTY);
    }
}
