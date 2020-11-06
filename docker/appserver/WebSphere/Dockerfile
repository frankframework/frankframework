ARG WAS_VERSION=8.5.5.17
FROM ibmcom/websphere-traditional:${WAS_VERSION} AS iaf-test-as-websphere
#COPY --chown=was:root 001-was-config.props /work/config/
COPY --chown=was:root src/scripts/PASSWORD /tmp/
COPY --chown=was:root target/ibis-adapterframework-docker-as-was-*-dependencies/*.py  /work/config/
COPY --chown=was:root target/dependencies/*.jar /work/drivers/
COPY --chown=was:root target/dependencies/ibis-adapterframework-ear.ear /work/app/adapterframework.ear
COPY --chown=was:root target/dependencies/frank /work/frank
ENV ENABLE_BASIC_LOGGING=true
ENV TZ=CET
RUN /work/configure.sh