FROM quay.io/wildfly/wildfly:34.0.1.Final-jdk21

COPY target/dependencies/secrets/ /opt/frank/secrets/
# use * when copying into existing folders
COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/ext/
COPY target/dependencies/*.rar /opt/jboss/wildfly/standalone/deployments/
COPY src/configuration/* /opt/jboss/wildfly/standalone/configuration/
COPY src/scripts/ /scripts/

RUN /scripts/createCredentialStore.sh
RUN /scripts/configureModules.sh

COPY target/dependencies/frank/ /opt/frank/
COPY target/dependencies/frankframework-test.war /opt/jboss/wildfly/standalone/deployments/iaf-test.war

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["/opt/jboss/wildfly/bin/standalone.sh", "-b", "0.0.0.0"]
