docker container stop iaf-example-tomcat
docker container rm iaf-example-tomcat

docker run --publish 80:8080 --name iaf-example-tomcat iaf-example-tomcat