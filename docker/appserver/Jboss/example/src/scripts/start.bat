cd %~dp0/../..
docker container stop iaf-example-jboss
docker container rm iaf-example-jboss
docker run --publish 80:8080 --name iaf-example-jboss iaf-example-jboss