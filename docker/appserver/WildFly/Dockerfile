ARG WILDFLY_VERSION=6.1.1.Final
FROM quay.io/wildfly/wildfly:${WILDFLY_VERSION} AS iaf-test-as-wildfly

# copy dependencies
COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/ext/

# Copy configuration script
COPY src/scripts/configuration.py /home/configuration.py
RUN python /home/configuration.py

# copy war
COPY target/dependencies/ibis-adapterframework-test.war /opt/jboss/wildfly/standalone/deployments/iaf-test.war