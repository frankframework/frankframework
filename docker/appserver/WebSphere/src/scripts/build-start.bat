set MVN=C:\opt\Apache\Maven\apache-maven-3.8.3\bin\mvn
set MVN_SETTINGS=C:\Users\L190409\Onedrive - NN\settings\.m2\settings.xml
set JAVA_HOME=C:\opt\Java\RedHat-openjdk-1.8.0.312-2.b07.x86_64
set CLI_OPTS=-Djavax.net.ssl.trustStrore=C:\Gerrit\Workspaces\eclipse\securereverseproxyserver\NNTrustStore.jks -Dmaven.wagon.http.ssl.insecure=true

set IMAGE_NAME=iaf-test-as-websphere
set CONTAINER_NAME=iaf-test-as-websphere-with-h2

docker container stop %CONTAINER_NAME%
docker container rm   %CONTAINER_NAME%

docker image rm %IMAGE_NAME%
docker image prune --force
docker volume prune --force

echo "build framework" 
call %MVN% %CLI_OPTS% -f %~dp0/../../../../.. -s "%MVN_SETTINGS%" install -DskipTests=true
echo "build WebSphere iaf-test docker image" 
call %MVN% %CLI_OPTS% -f %~dp0/../.. -s "%MVN_SETTINGS%" install

call %~dp0/start.bat
