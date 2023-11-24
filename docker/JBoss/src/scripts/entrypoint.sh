#!/usr/bin/env bash
set -e

export DISABLE_EMBEDDED_JMS_BROKER=true

# Append HeapDump and GC properties to existing JAVA_OPTS
export JAVA_OPTS="$JAVA_OPTS \
	-XX:HeapDumpPath=$JBOSS_HOME/standalone/log \
	-XX:+HeapDumpOnOutOfMemoryError \
	-XX:+UseSerialGC \
	-XX:+UseParallelGC \
	-XX:+USeParNewGC \
	-XX:+UseG1GC"

exec "$@"
