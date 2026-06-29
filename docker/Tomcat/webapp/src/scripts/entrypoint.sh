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

# By default, GC debugging is disabled.
if [ "$GC_LOG_ENABLED" = "true" ]; then
	export JAVA_OPTS="-Xlog:gc*,gc+age=trace:file=/usr/local/tomcat/logs/gc-log.log:time,uptime:filesize=104857600,filecount=5 $JAVA_OPTS"
else
	export JAVA_OPTS="-XX:+PerfDisableSharedMem $JAVA_OPTS"
fi

exec "$@"
