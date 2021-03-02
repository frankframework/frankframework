#!/bin/bash
set -e

if [ "$1" = 'catalina.sh' ]; then
	# Fix permissions
	chown -R tomcat:tomcat ${CATALINA_HOME}
	find ${CATALINA_HOME}/conf/ -type f -print0|xargs -0 chmod 400
	chown -R tomcat:tomcat /opt/frank
	chmod -R 700 /opt/frank
	
	exec gosu tomcat "$@"
fi

exec "$@"