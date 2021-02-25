cd %~dp0/../..
docker container stop iaf-test-tomcat
docker container rm iaf-test-tomcat
docker run --publish 80:8080 -e application.server.type.custom=BTM -e jdbc.dbms.default=h2 -e larva.adapter.execute= --name iaf-test-tomcat iaf-test-tomcat