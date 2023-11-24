#!/usr/bin/env bash
set -e

export JBOSS_JAVA_SIZING="-Xms64m -Xmx${JAVA_MAX_MEM:-512M} -XX:MetaspaceSize=512M -XX:MaxMetaspaceSize=1024m" \

# Append HeapDump and GC properties to existing JAVA_OPTS
export JAVA_OPTS="$JAVA_OPTS \
	-XX:HeapDumpPath=/opt/jboss/wildfly/standalone/log/ \
	-XX:+HeapDumpOnOutOfMemoryError \
	-XX:+UnlockExperimentalVMOptions \
	-XX:+UseZGC"

exec "$@"
