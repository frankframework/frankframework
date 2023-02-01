docker container stop iaf-example-tomcat
docker container rm iaf-example-tomcat

docker build -t iaf-example-tomcat %~dp0