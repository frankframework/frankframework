docker container stop iaf-test-as-jboss
docker container rm iaf-test-as-jboss

docker run --publish 80:8080 -e jdbc.dbms.default=oracle --name iaf-test-as-jboss iaf-test-as-jboss