cd %~dp0
docker-compose down --volumes --remove-orphans

docker build -t iaf-test-monitoring-influx .

docker-compose up --build