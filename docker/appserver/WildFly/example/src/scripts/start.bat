cd %~dp0/../..
docker container stop iaf-example-wildfly
docker container rm iaf-example-wildfly
docker run --publish 80:8080 --name iaf-example-wildfly iaf-example-wildfly