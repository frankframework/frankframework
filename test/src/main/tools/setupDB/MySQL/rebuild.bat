cd %~dp0
docker-compose down --volumes
docker image prune --force
docker volume prune --force
docker-compose up --build