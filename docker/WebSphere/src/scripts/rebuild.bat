set IMAGE_NAME=iaf-test-as-websphere
set CONTAINER_NAME=iaf-test-as-websphere-with-h2

docker container stop %CONTAINER_NAME%
docker container rm   %CONTAINER_NAME%

docker image rm %IMAGE_NAME%
docker image prune --force
docker volume prune --force

docker build -t %IMAGE_NAME% %~dp0/../..

call %~dp0/start.bat
