call %~dp0/setenv.bat

call %MVN% %CLI_OPTS% -f %IAF_DIR% -s "%MVN_SETTINGS%" clean

