cd %~dp0/../..

docker-compose down --volumes --remove-orphans

docker build -t iaf-test-tomcat .

docker-compose up --build