cd %~dp0/../..
docker container stop iaf-test-tomcat
docker container rm iaf-test-tomcat
docker run --publish 80:8080 --mount type=bind,source="%~dp0/../../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext -e jdbc.dbms.default=h2 --name iaf-test-tomcat iaf-test-tomcat