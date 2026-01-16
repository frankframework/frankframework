# Use EAP 8 Builder image to create a JBoss EAP 8 server with its default configuration

# FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-builder-openshift-rhel8:latest AS builder
FROM private.docker.nexus.frankframework.org/jboss-eap-8/eap81-openjdk21-builder-openshift-rhel9:1.0.0.GA-14.1764763265 AS builder

# Set up environment variables for provisioning.
ENV GALLEON_PROVISION_FEATURE_PACKS org.jboss.eap:wildfly-ee-galleon-pack,org.jboss.eap.cloud:eap-cloud-galleon-pack
ENV GALLEON_PROVISION_LAYERS cloud-default-config
# Specify the JBoss EAP version
ENV GALLEON_PROVISION_CHANNELS org.jboss.eap.channels:eap-8.0


# Run the assemble script to provision the server.
RUN /usr/local/s2i/assemble

# Copy the JBoss EAP 8 server from the builder image to the runtime image.
# FROM registry.redhat.io/jboss-eap-8/eap8-openjdk17-runtime-openshift-rhel8:latest AS runtime
FROM private.docker.nexus.frankframework.org/jboss-eap-8/eap81-openjdk21-runtime-openshift-rhel9:1.0.0.GA-13.1764763264 AS runtime

# Set appropriate ownership and permissions.
COPY --from=builder --chown=jboss:root $JBOSS_HOME $JBOSS_HOME

# Ensure appropriate permissions for the copied files.
RUN chmod -R ug+rwX $JBOSS_HOME