cd %~dp0/../..
docker container stop iaf-test-as-tomcat-with-mssql
docker container rm iaf-test-as-tomcat-with-mssql
docker run --publish 8080:8080 -e jdbc.dbms.default=mssql --name iaf-test-as-tomcat-with-mssql iaf-test-as-tomcat:7.6-SNAPSHOT