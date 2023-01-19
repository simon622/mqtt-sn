#!/bin/bash
echo ""
echo "  MQTT-SN Gateway Startup Script"
echo ""

export VERSION="0.1.17"

echoerr() { printf "%s\n" "$*" >&2; }

if hash java 2>/dev/null; then

    java_version=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | sed 's/\..*//')

    if [[ "$((java_version))" -lt 8 ]]; then
        echoerr "HiveMQ Edge requires at least Java version 8"
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

    if [ -c "/dev/urandom" ]; then
        # Use /dev/urandom as standard source for secure randomness if it exists
        JAVA_OPTS="$JAVA_OPTS -Djava.security.egd=file:/dev/./urandom"
    fi

    # JMX Monitoring
    JAVA_OPTS="$JAVA_OPTS -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9010 -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"

    if [ -z "$HIVEMQ_HOME" ]; then
        HIVEMQ_FOLDER="$( cd "$( dirname "${BASH_SOURCE[0]}" )/../" && pwd )"
        HOME_OPT="-Dhivemq.home=$HIVEMQ_FOLDER"
    else
        HIVEMQ_FOLDER="$HIVEMQ_HOME"
        HOME_OPT="-Dhivemq.home=$HIVEMQ_FOLDER"
    fi

    if [ -z "$HIVEMQ_HEAPDUMP_FOLDER" ]; then
        HEAPDUMP_PATH="$HIVEMQ_FOLDER"
    else
        HEAPDUMP_PATH="$HIVEMQ_HEAPDUMP_FOLDER"
    fi

    if [ ! -d "$HIVEMQ_FOLDER" ]; then
        echoerr "ERROR! HiveMQ Home Folder not found."
    else

        if [ ! -w "$HIVEMQ_FOLDER" ]; then
            echoerr "ERROR! HiveMQ Home Folder Permissions not correct."
        else

            if [ ! -f "$HIVEMQ_FOLDER/bin/hivemq.jar" ]; then
                echoerr "ERROR! HiveMQ JAR not found."
                echoerr "$HIVEMQ_FOLDER";
            else
                JAVA_OPTS="$JAVA_OPTS -XX:+CrashOnOutOfMemoryError"
                JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError"
                HEAPDUMP_PATH_OPT="-XX:HeapDumpPath=$HEAPDUMP_PATH/heap-dump.hprof"
                ERROR_FILE_PATH_OPT="-XX:ErrorFile=$HEAPDUMP_PATH/hs_err_pid%p.log"

                echo "-------------------------------------------------------------------------"
                echo ""
                echo "  HIVEMQ_HOME: $HIVEMQ_FOLDER"
                echo ""
                echo "  JAVA_OPTS: $JAVA_OPTS"
                echo ""
                echo "  JAVA_VERSION: $java_version"
                echo ""
                echo "-------------------------------------------------------------------------"
                echo ""
                # Run HiveMQ
                JAR_PATH="$HIVEMQ_FOLDER/bin/hivemq.jar"
                exec "java" "${HOME_OPT}" "${HEAPDUMP_PATH_OPT}" "${ERROR_FILE_PATH_OPT}" ${JAVA_OPTS} -jar "${JAR_PATH}"
            fi
        fi
    fi

else
    echoerr "ERROR! You do not have the Java Runtime Environment installed, please install Java JRE from https://adoptium.net/?variant=openjdk11 and try again."
    exit 1
fi