#!/usr/bin/env bash
set -e

# Append Hazelcast optimisations, HeapDump and GC properties to existing JAVA_OPTS
export JAVA_OPTS="\
	-XX:HeapDumpPath=/usr/local/tomcat/logs \
	-XX:+HeapDumpOnOutOfMemoryError \
	--add-modules java.se \
	--add-exports java.base/jdk.internal.ref=ALL-UNNAMED \
	--add-opens java.base/java.lang=ALL-UNNAMED \
	--add-opens java.base/sun.nio.ch=ALL-UNNAMED \
	--add-opens java.management/sun.management=ALL-UNNAMED \
	--add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED \
	$JAVA_OPTS"

exec "$@"
