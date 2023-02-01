docker image rm %IMAGE_NAME%

echo "build %PRODUCT% iaf-test docker image" 
call %MVN% %CLI_OPTS% -f %~dp0/../%PRODUCT% -s "%MVN_SETTINGS%" %MVN_OPTIONS% install

call %~dp0/start-body.bat
