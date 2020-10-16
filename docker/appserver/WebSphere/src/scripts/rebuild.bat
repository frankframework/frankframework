cd %~dp0/../..

docker-compose down --volumes --remove-orphans
docker image rm iaf-test-as-websphere
docker image prune --force
docker volume prune --force

docker build -t iaf-test-as-websphere .

docker-compose up --build