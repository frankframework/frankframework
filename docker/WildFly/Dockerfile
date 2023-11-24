ARG WILDFLY_VERSION=26.1.3.Final-jdk11
FROM quay.io/wildfly/wildfly:${WILDFLY_VERSION}

USER root
RUN mkdir /opt/jboss/wildfly/standalone/data
RUN chown -R jboss:jboss /opt/jboss/wildfly/standalone/data
USER jboss

COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/ext/
COPY target/dependencies/*.rar /opt/jboss/wildfly/standalone/deployments/
COPY target/dependencies/secrets /opt/frank/secrets
COPY src/configuration/ /opt/jboss/wildfly/standalone/configuration/
COPY src/scripts/ /scripts/

RUN /scripts/createCredentialStore.sh
RUN python /scripts/configureModules.py

COPY target/dependencies/frank /opt/frank
COPY target/dependencies/ibis-adapterframework-test.war /opt/jboss/wildfly/standalone/deployments/iaf-test.war

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
