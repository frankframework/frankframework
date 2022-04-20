ARG IMAGE_VERSION
ARG DOCKER_REPO
FROM ${DOCKER_REPO}:${IMAGE_VERSION}

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-ear.ear ${JBOSS_HOME}/standalone/deployments/iaf.ear

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar ${JBOSS_HOME}/standalone/lib/