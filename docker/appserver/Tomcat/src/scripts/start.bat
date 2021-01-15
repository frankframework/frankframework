cd %~dp0/../..
docker container stop iaf-test-as-tomcat-with-h2
docker container rm iaf-test-as-tomcat-with-h2
docker run --publish 8080:8080 -e jdbc.dbms.default=h2 --name iaf-test-as-tomcat-with-h2 iaf-test-as-tomcat:7.6-SNAPSHOT