#!/bin/bash
# Wrapper script to run gradle with Java 21 set

export JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.12
export PATH="$JAVA_HOME/bin:$PATH"

exec "$(dirname "$0")/gradlew" "$@"
