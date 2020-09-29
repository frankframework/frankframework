FROM tomcat:7.0.93-jre8

WORKDIR /usr/local/tomcat

RUN sed -i -e 's|Connector port=\"8080\"|Connector port=\"80\"|g' conf/server.xml
RUN echo "dtap.stage=LOC" >> conf/catalina.properties
RUN mkdir -p log/larvatest
RUN echo "log.dir=/usr/local/tomcat/log/larvatest" >> conf/catalina.properties
RUN echo "(?i)(([cd]:[\\\\/]temp)|(/usr/local/tomcat/log/larvatest))" >> conf/catalina.properties
RUN echo "jdbc.dbms.default=oracle-docker" >> conf/catalina.properties
RUN echo "web.port=8080" >> conf/catalina.properties
RUN echo "file.separator.regex=/" >> conf/catalina.properties

COPY ../../../../../target/*.war webapps/iaf-test.war
