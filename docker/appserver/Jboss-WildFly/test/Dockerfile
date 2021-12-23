ARG SERVER_TYPE
FROM iaf-as-${SERVER_TYPE}
ARG SERVER_TYPE
ARG STANDALONE_VERSION
# copy any additional drivers
COPY target/dependencies/*.jar ${JBOSS_HOME}/standalone/lib/ext/

# copy war
COPY target/dependencies/ibis-adapterframework-test.war ${JBOSS_HOME}/standalone/deployments/iaf-test.war

# copy database and jms configuration
COPY src/configurations/standalone_${SERVER_TYPE}${STANDALONE_VERSION}.xml ${JBOSS_HOME}/standalone/configuration/standalone.xml

COPY src/scripts/add_modules.py /home/add_modules.py

RUN if [ "$SERVER_TYPE" = "jboss" ]; then python3 /home/add_modules.py; else python /home/add_modules.py; fi