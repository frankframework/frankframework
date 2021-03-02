cd %~dp0/../..
docker container stop iaf-example-wildfly
docker container rm iaf-example-wildfly
docker run --publish 8080:8080 --name iaf-example-wildfly iaf-example-wildfly