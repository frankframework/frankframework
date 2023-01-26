docker container stop iaf-test-tomcat
docker container rm iaf-test-tomcat

docker build -t iaf-test-tomcat %~dp0