#!/bin/bash
set -e

chown -R tomcat:tomcat ${CATALINA_HOME}
find ${CATALINA_HOME}/conf/ -type f -print0|xargs -0 chmod 400
chown -R tomcat:tomcat /opt/frank
chmod -R 700 /opt/frank