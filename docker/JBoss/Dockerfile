ARG REGISTRY=private.docker.nexus.frankframework.org
ARG JBOSS_VARIANT=eap74-openjdk11-openshift-rhel8
ARG JBOSS_BASE_VERSION=7.4.0
ARG JBOSS_VERSION=7.4.13-7
FROM ${REGISTRY}/jboss-eap-7/${JBOSS_VARIANT}:${JBOSS_BASE_VERSION} AS jboss-base

COPY src/scripts/createCredentialStore.sh /scripts/createCredentialStore.sh
COPY target/dependencies/secrets /opt/frank/secrets

RUN /scripts/createCredentialStore.sh

FROM ${REGISTRY}/jboss-eap-7/${JBOSS_VARIANT}:${JBOSS_VERSION}

RUN mkdir $JBOSS_HOME/standalone/data

# chown is needed because otherwise permissions of the directory conflict with JBoss, resulting in WFLYSRV0126
COPY --chown=jboss:jboss --from=jboss-base $JBOSS_HOME/standalone/data/CS.jceks $JBOSS_HOME/standalone/data/CS.jceks
COPY target/dependencies/*.jar $JBOSS_HOME/standalone/lib/ext/
COPY target/dependencies/*.rar $JBOSS_HOME/standalone/deployments
COPY src/configuration/ $JBOSS_HOME/standalone/configuration/
COPY src/scripts/ /scripts/

RUN python3 /scripts/configureModules.py

COPY target/dependencies/frank /opt/frank
COPY target/dependencies/ibis-adapterframework-test.war $JBOSS_HOME/standalone/deployments/iaf-test.war

ENTRYPOINT ["/scripts/entrypoint.sh"]
CMD ["/opt/eap/bin/openshift-launch.sh"]
