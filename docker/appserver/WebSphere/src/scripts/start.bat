cd %~dp0/../..
docker container stop iaf-test-as-websphere-with-h2
docker container rm iaf-test-as-websphere-with-h2
docker run --publish 9443:9443 --publish 9043:9043 -e jdbc.dbms.default=h2 --name iaf-test-as-websphere-with-h2 iaf-test-as-websphere:7.6-SNAPSHOT