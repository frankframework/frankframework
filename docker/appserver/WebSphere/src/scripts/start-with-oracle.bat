cd %~dp0/../..
docker container stop iaf-test-as-websphere-with-oracle
docker container rm iaf-test-as-websphere-with-oracle
docker run --publish 9043:9043 --publish 9443:9443 -e jdbc.dbms.default=oracle-docker --name iaf-test-as-websphere-with-oracle iaf-test-as-websphere:7.6-SNAPSHOT