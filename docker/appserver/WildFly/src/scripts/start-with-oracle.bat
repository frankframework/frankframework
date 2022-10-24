docker container stop iaf-test-as-wildfly
docker container rm iaf-test-as-wildfly

docker run --publish 80:8080 -e jdbc.dbms.default=oracle --mount type=bind,src=%~dp0/../../../../../test/src/main/secrets,dst=/opt/frank/secrets --name iaf-test-as-wildfly iaf-test-as-wildfly