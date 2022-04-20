cd %~dp0/../..
@echo off
FOR /F "tokens=1,2 delims==" %%A IN (../server.properties) DO (
	IF "%%A"=="SERVER_TYPE" SET type=%%B
)
@echo on
set container_name=iaf-test-%type%
set image_name=iaf-test-%type%

docker container stop %container_name%
docker container rm %container_name%
docker run --publish 80:8080 --mount type=bind,source="%~dp0/../../../../../../test/src/test/testtool",target=/opt/frank/testtool-ext --name %container_name% %image_name%