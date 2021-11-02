docker container stop iaf-test-tomcat
docker container rm iaf-test-tomcat

docker run --publish 80:8080 --mount type=bind,source="%~dp0/../../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext -e jms.provider.default=activemq -e application.server.type.custom=BTM -e jdbc.dbms.default=oracle -e larva.adapter.active=false --name iaf-test-tomcat iaf-test-tomcat