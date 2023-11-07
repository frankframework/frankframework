ARG JBOSS_VARIANT=eap74-openjdk11-openshift-rhel8
ARG JBOSS_VERSION=7.4.0
ARG REGISTRY=private.docker.nexus.frankframework.org
FROM ${REGISTRY}/jboss-eap-7/${JBOSS_VARIANT}:${JBOSS_VERSION} AS jboss-base

USER root
RUN mkdir $JBOSS_HOME/standalone/data
RUN chown jboss:jboss $JBOSS_HOME/standalone/data

COPY src/configuration/createCS.sh /home/createCS.sh
COPY target/dependencies/secrets /opt/frank/secrets

RUN /home/createCS.sh
USER jboss

FROM jboss-base

ENV DISABLE_EMBEDDED_JMS_BROKER=true
# Copy dependencies
COPY target/dependencies/*.jar $JBOSS_HOME/standalone/lib/ext/
COPY target/dependencies/*.rar $JBOSS_HOME/standalone/deployments

# Copy in standalone-configuration.xml
COPY src/configuration/standalone.xml $JBOSS_HOME/standalone/configuration/standalone-openshift.xml

# Copy in the created credential store
# chown is needed because otherwise permissions of the directory conflict with JBoss, resulting in WFLYSRV0126
COPY --from=jboss-base --chown=jboss:jboss $JBOSS_HOME/standalone/data/CS.jceks $JBOSS_HOME/standalone/data/CS.jceks

# Copy configuration script for modules
COPY src/configuration/configuration.py /home/configuration.py
RUN python3 /home/configuration.py

# Add test scenarios explictly for easy CI
COPY target/dependencies/frank /opt/frank

# Copy war
COPY target/dependencies/ibis-adapterframework-test.war $JBOSS_HOME/standalone/deployments/iaf-test.war

