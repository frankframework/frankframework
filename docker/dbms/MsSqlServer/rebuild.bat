cd %~dp0
docker-compose down --volumes --remove-orphans
docker image prune --force
docker volume prune --force
docker container rm iaf-test-db-mssql

docker build -t iaf-test-db-mssql .

docker-compose up --build
