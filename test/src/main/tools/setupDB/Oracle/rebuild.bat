cd %~dp0
docker-compose down --volumes
docker image prune --force
docker volume prune --force

docker build -t iaf-test-db-oracle .

docker-compose up --build
