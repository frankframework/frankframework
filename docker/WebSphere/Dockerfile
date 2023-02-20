ARG WAS_VERSION=9.0.5.11
FROM ibmcom/websphere-traditional:${WAS_VERSION}
#COPY --chown=was:root 001-was-config.props /work/config/
COPY --chown=was:root src/configuration/PASSWORD /tmp/
COPY --chown=was:root src/configuration/wasap.policy /work
RUN cat /work/wasap.policy >> /opt/IBM/WebSphere/AppServer/profiles/AppSrv01/config/cells/DefaultCell01/nodes/DefaultNode01/app.policy  && rm -f /work/wasap.policy
COPY --chown=was:root target/classes/*.py  /work/config/
COPY --chown=was:root target/dependencies/*.jar /work/drivers/
COPY --chown=was:root target/dependencies/ibis-adapterframework-ear.ear /work/app/adapterframework.ear
COPY --chown=was:root target/dependencies/frank /opt/frank
COPY --chown=was:root target/dependencies/secrets /opt/frank/secrets
ENV ENABLE_BASIC_LOGGING=true
ENV TZ=CET
RUN /work/configure.sh \
	# Remove secrets from final image, they would still persist in image layers, but this is good enough for test
	# WAS doesn't really have options to import them (xor is easily decoded) and Docker Buildkit requires some additional changes to CI
	&& rm -rf /opt/frank/secrets
