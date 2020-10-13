FROM ibmcom/websphere-traditional
#COPY --chown=was:root 001-was-config.props /work/config/
COPY --chown=was:root src/scripts/PASSWORD /tmp/
COPY --chown=was:root src/scripts/*.py /work/config/
COPY --chown=was:root target/ibis-adapterframework-docker-as-was-*-dependencies/*.jar /work/drivers/
COPY --chown=was:root target/ibis-adapterframework-docker-as-was-*-dependencies/ibis-adapterframework-test-ear-*.ear /work/app/ibis-adapterframework-test.ear
ENV ENABLE_BASIC_LOGGING=true
RUN /work/configure.sh