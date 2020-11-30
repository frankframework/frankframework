cd %~dp0/../..
docker container stop iaf-test-as-websphere-with-mssql
docker container rm iaf-test-as-websphere-with-mssql
docker run --publish 9043:9043 --publish 9443:9443 -e jdbc.dbms.default=mssql --name iaf-test-as-websphere-with-mssql iaf-test-as-websphere:7.6-SNAPSHOT