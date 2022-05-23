set MVN=C:\opt\Apache\Maven\apache-maven-3.8.3\bin\mvn
set MVN_SETTINGS=C:\Users\L190409\Onedrive - NN\settings\.m2\settings.xml
set JAVA_HOME=C:\opt\Java\RedHat-openjdk-1.8.0.312-2.b07.x86_64
set CLI_OPTS=-Djavax.net.ssl.trustStrore=C:\Gerrit\Workspaces\eclipse\securereverseproxyserver\NNTrustStore.jks -Dmaven.wagon.http.ssl.insecure=true

call %MVN% %CLI_OPTS% -f %~dp0/../../../../.. -s "%MVN_SETTINGS%" clean
call %MVN% %CLI_OPTS% -f %~dp0/../.. -s "%MVN_SETTINGS%" clean

call %~dp0/build-start.bat
