cd %~dp0/../..

docker-compose down --volumes --remove-orphans

docker build -t iaf-test-as-tomcat .

docker-compose up --build