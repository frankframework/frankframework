#!/bin/bash
set -e

chown -hR tomcat:tomcat ${CATALINA_HOME}
chown -hR tomcat:tomcat /opt/frank
