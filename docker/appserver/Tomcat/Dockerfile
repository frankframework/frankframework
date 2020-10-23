FROM tomcat:7.0.93-jre8

WORKDIR /usr/local/tomcat

RUN mkdir -p log/larvatest/testdata
RUN rm -rf webapps/ROOT
RUN rm -rf webapps/ROOT.war
RUN sed -i -e 's|Connector port=\"8080\"|Connector port=\"80\"|g' conf/server.xml
RUN echo "dtap.stage=LOC" >> conf/catalina.properties
RUN echo "log.dir=/usr/local/tomcat/log/larvatest" >> conf/catalina.properties
RUN echo "log.dir.match=(?i)(([cd]:[\\\\/]temp)|(/usr/local/tomcat/log/larvatest))" >> conf/catalina.properties
RUN echo "jdbc.dbms.default=oracle-docker" >> conf/catalina.properties
RUN echo "web.port=80" >> conf/catalina.properties
RUN echo "file.separator.regex=/" >> conf/catalina.properties
RUN echo 'testdata.dir.match=${log.dir.match}[\\/]testdata' >> conf/catalina.properties
RUN echo 'log.level=DEBUG' >> conf/catalina.properties
COPY target/ibis-adapterframework-docker-as-tomcat-*-dependencies/tomcat/* conf/

# We deploy the Frank!Framework to the root context of the application server.
# See https://stackoverflow.com/questions/7276989/how-to-set-the-context-path-of-a-web-application-in-tomcat-7-0
#
# We do not copy the .war to webapps, but to a location outside that directory. Then we
# reference that file in ROOT.xml, the file that selects the app in the root context.
COPY target/ibis-adapterframework-docker-as-tomcat-*-dependencies/*.war .
