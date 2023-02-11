#!/usr/bin/env bash

# Simple command that uses the Maven exec plugin to run the program.

./mvnw clean compile
export JAVA_PROGRAM_ARGS=`echo "$@"`
./mvnw exec:java -Dexec.args="$JAVA_PROGRAM_ARGS"
