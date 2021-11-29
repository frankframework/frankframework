cd %~dp0/../..

docker-compose down --volumes --remove-orphans

docker build -t iaf-test-as-websphere .

docker run --publish 9443:9443 --publish 9043:9043 --mount type=bind,source="%~dp0/../../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext --name iaf-test-as-websphere