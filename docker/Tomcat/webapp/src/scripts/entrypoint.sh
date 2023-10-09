#!/usr/bin/env bash
set -e

# Append HeapDump property to existing JAVA_OPTS
export JAVA_OPTS="$JAVA_OPTS -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/usr/local/tomcat/logs"

exec "$@"
