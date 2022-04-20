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
docker run --publish 80:8080 -e jdbc.dbms.default=h2 -e larva.adapter.execute= --name %container_name% %image_name%