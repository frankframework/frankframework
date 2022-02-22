
docker container stop iaf-test-activemq-artemis
docker container rm iaf-test-activemq-artemis

docker run --publish 8160:8161 --publish 61615:61616 --restart unless-stopped --name iaf-test-activemq-artemis iaf-test-activemq-artemis
