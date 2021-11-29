ARG WAS_VERSION=9.0.5.7
FROM ibmcom/websphere-traditional:${WAS_VERSION} AS iaf-test-as-websphere
#COPY --chown=was:root 001-was-config.props /work/config/
COPY --chown=was:root src/scripts/PASSWORD /tmp/
COPY --chown=was:root src/scripts/wasap.policy /work
RUN cat /work/wasap.policy >> /opt/IBM/WebSphere/AppServer/profiles/AppSrv01/config/cells/DefaultCell01/nodes/DefaultNode01/app.policy  && rm -f /work/wasap.policy
# when the line below fails due to the '*' in the directory name, then disable buildkit in Docker Engine settings, or wait until docker bug has been fixed (https://github.com/moby/buildkit/issues/1900)
COPY --chown=was:root target/ibis-adapterframework-docker-as-was-*-dependencies/*.py  /work/config/
COPY --chown=was:root target/dependencies/*.jar /work/drivers/
COPY --chown=was:root target/dependencies/ibis-adapterframework-ear.ear /work/app/adapterframework.ear
COPY --chown=was:root target/dependencies/frank /opt/frank
ENV ENABLE_BASIC_LOGGING=true
ENV TZ=CET
RUN /work/configure.sh