FROM tomcat:7.0.93-jre8

WORKDIR /usr/local/tomcat

RUN mkdir -p logs/larvatest/testdata
RUN rm -rf webapps/ROOT
RUN rm -rf webapps/ROOT.war
RUN sed -i -e 's|Connector port=\"8080\"|Connector port=\"80\"|g' conf/server.xml
RUN echo "dtap.stage=LOC" >> conf/catalina.properties
RUN echo "log.dir=/usr/local/tomcat/logs/larvatest" >> conf/catalina.properties
RUN echo "log.dir.match=(?i)(([cd]:[\\\\/]temp)|(/usr/local/tomcat/logs/larvatest))" >> conf/catalina.properties
RUN echo "jdbc.dbms.default=oracle-docker" >> conf/catalina.properties
RUN echo "web.port=80" >> conf/catalina.properties
RUN echo "file.separator.regex=/" >> conf/catalina.properties
RUN echo 'testdata.dir.match=${log.dir.match}[\\/]testdata' >> conf/catalina.properties
RUN echo 'log.level=DEBUG' >> conf/catalina.properties
COPY target/ibis-adapterframework-docker-as-tomcat-${version}-dependencies/ibis-adapterframework-test-${iaf.version}.war webapps/iaf-test.war
