
docker run --publish 8888:8080 --restart unless-stopped -v %~dp0/shared:/opt/shared --name keycloak keycloak