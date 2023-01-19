#!/bin/bash
echo ""
echo "  MQTT-SN Gateway Startup Script"
echo ""

export VERSION="0.1.17"

if hash java 2>/dev/null; then

    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed 's/\..*//')

    if [[ "$((java_version))" -lt 8 ]]; then
        echoerr "Requires at least Java version 8"
        exit 1
    fi

    ############## VARIABLES
    JAVA_OPTS="$JAVA_OPTS -DwireLoggingEnabled=false"
    JAVA_OPTS="$JAVA_OPTS -noverify"
    JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.lang=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-opens java.base/java.nio=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-opens java.base/sun.nio.ch=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"
    JAVA_OPTS="$JAVA_OPTS --add-exports java.base/jdk.internal.misc=ALL-UNNAMED"

    # JMX Monitoring
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

    JAVA_OPTS="$JAVA_OPTS -XX:+CrashOnOutOfMemoryError"
    JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"

    HEAPDUMP_PATH_OPT="-XX:HeapDumpPath=$HEAPDUMP_PATH/heap-dump.hprof"
    ERROR_FILE_PATH_OPT="-XX:ErrorFile=$HEAPDUMP_PATH/hs_err_pid%p.log"

    JAR_PATH="./mqtt-sn-gateway-${VERSION}.jar"
    exec "java" "${HOME_OPT}" "${HEAPDUMP_PATH_OPT}" "${ERROR_FILE_PATH_OPT}" ${JAVA_OPTS} -jar "${JAR_PATH}"

else
    echoerr "ERROR! You do not have the Java Runtime Environment installed, please install Java JRE from https://adoptium.net/?variant=openjdk11 and try again."
    exit 1
fi