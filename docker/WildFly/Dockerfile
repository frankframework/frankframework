ARG WILDFLY_VERSION=26.1.1.Final
FROM quay.io/wildfly/wildfly:${WILDFLY_VERSION} AS wildfly-iaf-test-base

USER root
RUN chown -R jboss:jboss /opt/jboss/wildfly/standalone/data
USER jboss

FROM wildfly-iaf-test-base AS wildfly-iaf-test-CredentialStoreCreator
COPY src/configuration/createCS.sh /home/createCS.sh
COPY target/dependencies/secrets /opt/frank/secrets

RUN /home/createCS.sh

FROM wildfly-iaf-test-base
# Copy dependencies
COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/ext/
COPY target/dependencies/*.rar /opt/jboss/wildfly/standalone/deployments/

# Copy in standalone-configuration.xml
COPY src/configuration/standalone.xml /opt/jboss/wildfly/standalone/configuration/standalone.xml

# Copy in the created credential store
# chown is needed because otherwise permissions of the directory conflict with JBoss, resulting in WFLYSRV0126
COPY --from=wildfly-iaf-test-CredentialStoreCreator --chown=jboss:jboss /opt/jboss/wildfly/standalone/data/CS.jceks /opt/jboss/wildfly/standalone/data/CS.jceks

# Copy configuration script for modules
COPY src/configuration/configuration.py /home/configuration.py
RUN python /home/configuration.py

# Add test scenarios explictly for easy CI
COPY target/dependencies/frank /opt/frank

# Copy war
COPY target/dependencies/ibis-adapterframework-test.war /opt/jboss/wildfly/standalone/deployments/iaf-test.war
