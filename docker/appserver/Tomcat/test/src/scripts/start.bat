cd %~dp0/../..
docker container stop iaf-test-tomcat-with-h2
docker container rm iaf-test-tomcat-with-h2
docker run --publish 80:8080 -e jdbc.dbms.default=h2 --name iaf-test-tomcat-with-h2 iaf-test-tomcat:7.6-SNAPSHOT