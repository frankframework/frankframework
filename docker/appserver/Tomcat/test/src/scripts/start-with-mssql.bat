cd %~dp0/../..
docker container stop iaf-test-tomcat-with-mssql
docker container rm iaf-test-tomcat-with-mssql
docker run --publish 80:8080 --mount type=bind,source="%~dp0/../../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext -e jdbc.dbms.default=mssql --name iaf-test-tomcat-with-mssql iaf-test-tomcat