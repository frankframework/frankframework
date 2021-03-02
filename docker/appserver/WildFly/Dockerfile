ARG VERSION=22.0.0.Final
FROM jboss/wildfly:${VERSION} AS iaf-as-wildfly

# copy the frank framework
COPY target/dependencies/ibis-adapterframework-webapp.war /opt/jboss/wildfly/standalone/deployments/iaf.war

# copy JDBC and JMS drivers
COPY target/dependencies/*.jar /opt/jboss/wildfly/standalone/lib/