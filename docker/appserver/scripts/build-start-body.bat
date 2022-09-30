call %~dp0/../../scripts/setenv.bat

docker image rm %IMAGE_NAME%
docker image prune --force
docker volume prune --force

echo "build framework" 
call %MVN% %CLI_OPTS% -f %~dp0/../../.. -s "%MVN_SETTINGS%" install -DskipTests=true
echo "build %PRODUCT% iaf-test docker image" 
call %MVN% %CLI_OPTS% -f %~dp0/../%PRODUCT% -s "%MVN_SETTINGS%" %MVN_OPTIONS% install

call %~dp0/start-body.bat
