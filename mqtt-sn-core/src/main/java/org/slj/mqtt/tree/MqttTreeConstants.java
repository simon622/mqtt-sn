package org.slj.mqtt.tree;

public interface MqttTreeConstants {

    char MIN_HIGH_UTF = '\uD800';
    char MAX_HIGH_UTF = '\uDBFF';

    //-- the optionally restricted range
    char MIN_CONTROL1_UTF = '\u0001';
    char MAX_CONTROL1_UTF = '\u001F';

    char MIN_CONTROL2_UTF = '\u007F';
    char MAX_CONTROL2_UTF = '\u009F';
    char UNICODE_ZERO = '\u0000';
    int UNSIGNED_MAX_16 = 65535;
    int MAX_TOPIC_LENGTH = UNSIGNED_MAX_16;

    String SINGLE_LEVEL_WILDCARD = "+"; //U+002B
    String MULTI_LEVEL_WILDCARD = "#"; //U+0023

    char PATH_SEP = '/'; //U+002F
    char SINGLE_WILDCARD_CHAR = '+';
}
