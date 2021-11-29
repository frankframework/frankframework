ARG SERVER_TYPE
FROM iaf-as-${SERVER_TYPE}
ARG SERVER_TYPE
ARG STANDALONE_VERSION
# copy any additional drivers
COPY target/dependencies/*.jar ${JBOSS_HOME}/standalone/lib/

# copy database and jms configuration
COPY src/configurations/standalone_${SERVER_TYPE}${STANDALONE_VERSION}.xml ${JBOSS_HOME}/standalone/configuration/standalone.xml

# copy configuration, properties and testscenarios
COPY target/dependencies/frank/configuration/*-resources.jar ${JBOSS_HOME}/standalone/lib/ext/
COPY target/dependencies/frank/configuration/*-configurations.jar ${JBOSS_HOME}/standalone/lib/ext/
COPY target/dependencies/frank/testtool /opt/frank/testtool


COPY src/scripts/add_modules.py /home/add_modules.py


USER root
RUN if [ "$SERVER_TYPE" = "jboss" ]; then yum -y install python3; fi

USER jboss
RUN if [ "$SERVER_TYPE" = "jboss" ]; then python3 /home/add_modules.py; else python /home/add_modules.py; fi