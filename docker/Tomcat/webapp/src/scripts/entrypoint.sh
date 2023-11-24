#!/usr/bin/env bash
set -e

# Append HeapDump and GC properties to existing JAVA_OPTS
export JAVA_OPTS="$JAVA_OPTS \
	-XX:HeapDumpPath=/usr/local/tomcat/logs \
	-XX:+HeapDumpOnOutOfMemoryError \
	-XX:+UseParallelGC"

exec "$@"
