docker stop iaf-test-db-db2
docker container rm iaf-test-db-db2

docker build -t iaf-test-db-db2 .

%~dp0start.bat