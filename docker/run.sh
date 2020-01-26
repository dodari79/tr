#!/bin/sh

exec /usr/bin/java \
    -Djsse.enableSNIExtension=false \
    -Djava.security.egd=file:/dev/./urandom \
    $JAVA_OPTS \
    -jar /app.jar
