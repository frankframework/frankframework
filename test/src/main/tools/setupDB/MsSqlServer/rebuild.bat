cd %~dp0
docker-compose down --volumes
docker image prune --force
docker volume prune --force
docker container rm iaf-db-test-mssql

docker build -t iaf-test-db-mssql .

docker-compose up --build
