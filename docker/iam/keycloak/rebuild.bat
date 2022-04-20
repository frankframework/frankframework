
docker container stop keycloak
docker container rm keycloak

docker build -t keycloak %~dp0

%~dp0/start
