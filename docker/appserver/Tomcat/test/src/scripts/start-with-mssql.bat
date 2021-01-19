cd %~dp0/../..
docker container stop iaf-test-tomcat-with-mssql
docker container rm iaf-test-tomcat-with-mssql
docker run --publish 80:8080 -e jdbc.dbms.default=mssql --name iaf-test-tomcat-with-mssql iaf-test-tomcat:7.6-SNAPSHOT