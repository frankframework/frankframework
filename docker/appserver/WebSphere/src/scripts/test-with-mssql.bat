cd %~dp0/../..
docker container stop iaf-test-as-websphere-with-mssql
docker container rm iaf-test-as-websphere-with-mssql
docker run --publish 9443:9443 --publish 9043:9043  -e larva.adapter.execute= -e jdbc.dbms.default=mssql --name iaf-test-as-websphere-with-mssql iaf-test-as-websphere