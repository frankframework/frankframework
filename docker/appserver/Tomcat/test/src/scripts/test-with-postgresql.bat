cd %~dp0/../..
docker container stop iaf-test-tomcat
docker container rm iaf-test-tomcat
docker run --publish 80:8080 -e jdbc.dbms.default=postgres -e larva.adapter.execute= --name iaf-test-tomcat iaf-test-tomcat