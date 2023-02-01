call %~dp0/setenv.bat

call %MVN% %CLI_OPTS% -f %IAF_DIR% -s "%MVN_SETTINGS%" clean

rem uncomment the below to clean up (all) docker images and volumes
rem docker image prune --force
rem docker volume prune --force
