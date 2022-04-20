
docker container stop iaf-test-activemq
docker container rm iaf-test-activemq

docker build -t iaf-test-activemq %~dp0

docker run --publish 8161:8161 --publish 61616:61616 --restart unless-stopped --name iaf-test-activemq iaf-test-activemq
