docker container stop iaf-test-as-wildfly
docker container rm iaf-test-as-wildfly

docker run --publish 80:8080 -e jdbc.dbms.default=oracle --name iaf-test-as-wildfly iaf-test-as-wildfly