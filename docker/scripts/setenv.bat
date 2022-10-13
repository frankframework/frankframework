
rem N.B. remove maven.test.skip if test.jar is not found, to enable its generation

set MVN=C:\opt\Apache\Maven\apache-maven-3.8.3\bin\mvn
set MVN_SETTINGS=%HOMEDRIVE%\%HOMEPATH%\.m2\settings.xml
set MVN_OPTIONS=-Dmaven.test.skip=true
set JAVA_HOME=C:\opt\Java\RedHat-openjdk-1.8.0.312-2.b07.x86_64
set CLI_OPTS=

set IAF_DIR=%~dp0/../..

rem setup 'setlocalenv.bat' to match your local environment

if exist %~dp0/setlocalenv.bat  call %~dp0/setlocalenv.bat

