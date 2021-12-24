
docker container stop iaf-test-activemq
docker container rm iaf-test-activemq

docker run --publish 8161:8161 --publish 5000:61616 --restart unless-stopped --name iaf-test-activemq iaf-test-activemq
