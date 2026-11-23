#!/bin/sh

# Minimal POSIX Gradle wrapper fallback if gradle is not installed
which gradle >/dev/null 2>&1 && exec gradle "$@"

# Standard gradle wrapper dispatch
DIR=$(cd "$(dirname "$0")" && pwd)
JAR="$DIR/gradle/wrapper/gradle-wrapper.jar"

if [ ! -f "$JAR" ]; then
    echo "Downloading gradle-wrapper.jar..."
    mkdir -p "$DIR/gradle/wrapper"
    curl -sLo "$JAR" https://raw.githubusercontent.com/gradle/gradle/v8.5.0/gradle/wrapper/gradle-wrapper.jar || true
fi

if [ -f "$JAR" ]; then
    exec java -jar "$JAR" "$@"
else
    echo "Error: Gradle wrapper jar not found and curl failed."
    exit 1
fi
