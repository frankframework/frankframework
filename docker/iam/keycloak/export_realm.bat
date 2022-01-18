rem
rem export_realm.bat
rem
rem Exports live keycloak configuration of realm iaf-test to file shared/iaf-test.json,
rem to be used as startup configuration when the container is recreated
rem 
rem N.B. script does not return by itself. It can be stopped when the following message appears in the log:
rem    Admin console listening on http://127.0.0.1:10090

set REALM=iaf-test
set SHARE=/opt/shared


set OPTIONS=-Djboss.socket.binding.port-offset=100
set OPTIONS=%OPTIONS% -Dkeycloak.migration.action=export
set OPTIONS=%OPTIONS% -Dkeycloak.migration.provider=singleFile
set OPTIONS=%OPTIONS% -Dkeycloak.migration.realmName=%REALM%
set OPTIONS=%OPTIONS% -Dkeycloak.migration.usersExportStrategy=REALM_FILE
set OPTIONS=%OPTIONS% -Dkeycloak.migration.file=%SHARE%/%REALM%.json

docker exec -it keycloak /opt/jboss/keycloak/bin/standalone.sh %OPTIONS%