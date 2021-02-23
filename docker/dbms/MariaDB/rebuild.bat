cd %~dp0
docker-compose down --volumes --remove-orphans
docker image prune --force
docker volume prune --force

docker build -t iaf-test-db-mariadb .

docker-compose up --build