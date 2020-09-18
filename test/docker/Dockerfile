FROM ubuntu:18.04

ARG IAF_VERSION=7.6-20200915.172517
ARG ORACLE_DB_IP

WORKDIR /home
RUN apt-get update
RUN apt-get install --yes wget
RUN apt-get install --yes zip

# Modify the database URL within the Frank!Framework
RUN mkdir work
RUN mkdir work/iaf
RUN wget -O work/iaf/iaf-test.war "https://nexus.ibissource.org/content/groups/public/org/ibissource/ibis-adapterframework-test/${IAF_VERSION}/ibis-adapterframework-test-${IAF_VERSION}.war"
RUN unzip -d work/iaf work/iaf/iaf-test.war
RUN rm work/iaf/iaf-test.war
RUN sed -i -e "s|localhost|${ORACLE_DB_IP}|g" work/iaf/META-INF/context.xml
WORKDIR /home/work/iaf
RUN zip -r ../iaf-test.war .
WORKDIR /home

# Download and unzip 
RUN wget -O jtds-1.3.1.zip "http://www.java2s.com/Code/JarDownload/jtds/jtds-1.3.1.jar.zip"
RUN unzip jtds-1.3.1.zip -d .

FROM tomcat:7.0.93-jre8

WORKDIR /usr/local/tomcat

# Libraries
RUN wget -O lib/geronimo-jms_1.1_spec-1.1.1.jar "https://repo1.maven.org/maven2/org/apache/geronimo/specs/geronimo-jms_1.1_spec/1.1.1/geronimo-jms_1.1_spec-1.1.1.jar"
RUN wget -O lib/commons-dbcp-1.4.jar "https://repo1.maven.org/maven2/commons-dbcp/commons-dbcp/1.4/commons-dbcp-1.4.jar"
RUN wget -O lib/commons-pool-1.5.6.jar "https://repo1.maven.org/maven2/commons-pool/commons-pool/1.5.6/commons-pool-1.5.6.jar"
COPY docker/ojdbc7.jar lib/ojdbc7.jar
COPY --from=0 /home/jtds-1.3.1.jar lib/jtds-1.3.1.jar

RUN echo "dtap.stage=LOC" >> conf/catalina.properties
RUN mkdir -p log/larvatest
RUN echo "log.dir=/usr/local/tomcat/log/larvatest" >> conf/catalina.properties
RUN echo "log.dir.match=/usr/local/tomcat/log/larvatest" >> conf/catalina.properties
RUN echo "jdbc.dbms.default=oracle-docker" >> conf/catalina.properties
RUN echo "web.port=8080" >> conf/catalina.properties
RUN echo "file.separator.regex=/" >> conf/catalina.properties

COPY --from=0 /home/work/iaf-test.war webapps/iaf-test.war
