docker container stop %CONTAINER_NAME%
docker container rm   %CONTAINER_NAME%

docker run --publish 80:%HTTP_PORT% --publish 443:%HTTPS_PORT% --publish %ADMIN_PORT%:%ADMIN_PORT% %DOCKER_OPTIONS% --mount type=bind,source="%~dp0/../../../test/src/test/testtool",target=/opt/frank/testtool-ext --name %CONTAINER_NAME% %IMAGE_NAME%
