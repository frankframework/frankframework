set IMAGE_NAME=iaf-test-as-websphere
set CONTAINER_NAME=iaf-test-as-websphere-with-h2

docker container stop %CONTAINER_NAME%
docker container rm   %CONTAINER_NAME%

docker run --publish 80:9080 --publish 9443:9443 --publish 9043:9043 -e larva.adapter.execute= --mount type=bind,source="%~dp0/../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext --name %CONTAINER_NAME% %IMAGE_NAME%
