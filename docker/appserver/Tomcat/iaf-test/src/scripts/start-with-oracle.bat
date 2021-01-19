cd %~dp0/../..
docker container stop iaf-test-tomcat-with-oracle
docker container rm iaf-test-tomcat-with-oracle
docker run --publish 80:8080 -e jdbc.dbms.default=oracle-docker --name iaf-test-tomcat-with-oracle iaf-test-tomcat:7.6-SNAPSHOT