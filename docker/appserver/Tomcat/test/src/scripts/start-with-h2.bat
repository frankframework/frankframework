docker container stop iaf-test-tomcat
docker container rm iaf-test-tomcat

docker run --publish 80:8080 -v "%~dp0/../../../../../../test/src/test/testtool":/opt/frank/testtool-ext -e larva.adapter.active=false -e jdbc.dbms.default=h2 --name iaf-test-tomcat iaf-test-tomcat
