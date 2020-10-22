cd %~dp0/../..

docker-compose down --volumes --remove-orphans

docker build -t iaf-test-as-websphere .

docker-compose up --build