ARG FF_VERSION=latest
# Make repository variable so we can build and test the image before pushing it to the registry
ARG REGISTRY=nexus.frankframework.org
FROM ${REGISTRY}/frankframework:${FF_VERSION}

# Copy resources
COPY --chown=tomcat target/resources/ /opt/frank/resources/
