ARG VERSION=6.4.22-centos
FROM daggerok/jboss-eap-6.4:${VERSION} AS iaf-as-jboss

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-ear.ear /home/jboss/jboss-eap-6.4/standalone/deployments/iaf.ear

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /home/jboss/jboss-eap-6.4/standalone/lib/