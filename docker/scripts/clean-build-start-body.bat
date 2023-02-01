call %~dp0/clean_iaf.bat

echo "clean %PRODUCT% iaf-test maven project"
call %MVN% %CLI_OPTS% -f %~dp0/../%PRODUCT% -s "%MVN_SETTINGS%" clean

call %~dp0/full-build-start-body.bat
