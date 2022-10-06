
docker-compose down --volumes --remove-orphans
rmdir /S /Q  %~dp0\data\prometheus

docker-compose up --build