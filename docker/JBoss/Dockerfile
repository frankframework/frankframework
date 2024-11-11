ARG REGISTRY=private.docker.nexus.frankframework.org
ARG JBOSS_VARIANT=eap8-openjdk17-runtime-openshift-rhel8-prebuild
ARG JBOSS_VERSION=1.0.0.GA
FROM ${REGISTRY}/jboss-eap-8/${JBOSS_VARIANT}:${JBOSS_VERSION}

COPY src/scripts/ /scripts/
COPY target/dependencies/secrets/ /opt/frank/secrets/
COPY src/configuration/* $JBOSS_HOME/standalone/configuration/
COPY target/dependencies/*.jar $JBOSS_HOME/standalone/lib/ext/
COPY target/dependencies/*.rar $JBOSS_HOME/standalone/deployments/

RUN /scripts/createCredentialStore.sh
RUN /scripts/configureModules.sh

COPY target/dependencies/frank/ /opt/frank/
COPY target/dependencies/frankframework-test.war $JBOSS_HOME/standalone/deployments/iaf-test.war

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["/opt/eap/bin/openshift-launch.sh"]
