ARG WILDFLY_VERSION=26.1.1.Final
FROM quay.io/wildfly/wildfly:${WILDFLY_VERSION} AS iaf-test-as-wildfly

# Copy dependencies
COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/ext/
COPY target/dependencies/*.rar /opt/jboss/wildfly/standalone/deployments/ 

# Copy in standalone-configuration.xml
COPY src/configuration/standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

# Copy configuration script for modules
COPY src/configuration/configuration.py /home/configuration.py
RUN python /home/configuration.py

# Add test scenarios explictly for easy CI
COPY target/dependencies/frank /opt/frank

# Copy war
COPY target/dependencies/ibis-adapterframework-test.war /opt/jboss/wildfly/standalone/deployments/iaf-test.war
