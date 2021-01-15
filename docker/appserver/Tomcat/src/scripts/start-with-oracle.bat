cd %~dp0/../..
docker container stop iaf-test-as-tomcat-with-oracle
docker container rm iaf-test-as-tomcat-with-oracle
docker run --publish 8080:80803 -e jdbc.dbms.default=oracle-docker --name iaf-test-as-tomcat-with-oracle iaf-test-as-tomcat:7.6-SNAPSHOT