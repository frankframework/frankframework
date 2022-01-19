#
# recreate-iaf-test.sh
#
# creates or recreates iaf-test realm and its containing configuration.
# This script must be run from within the container.
# After running this script, run 'export_realm.bat' (from outside the container)
# to save the configuration.
# N.B. Any hand made changes will be lost without warning.

KEYCLOAK_HOME=/opt/jboss/keycloak
KCADM=$KEYCLOAK_HOME/bin/kcadm.sh

CLIENT_APP_CLIENT_ID=testiaf-client
CLIENT_APP_CLIENT_SECRET=testiaf-client-pwd
SERVICE_APP_CLIENT_ID=testiaf-service
SERVICE_APP_CLIENT_SECRET=testiaf-service-pwd

$KCADM config credentials --server http://localhost:8080/auth --realm master --user admin --password admin

echo "delete realm"
$KCADM delete realms/iaf-test

echo "create realm iaf-test"
$KCADM create realms -s realm=iaf-test -s enabled=true -o

echo "create clients"
CID_CLIENT=$($KCADM create clients -r iaf-test -s clientId=$CLIENT_APP_CLIENT_ID -s 'redirectUris=["http://localhost:8980/testiafClient/*"]' -i)
CID_SERVICE=$($KCADM create clients -r iaf-test -s clientId=$SERVICE_APP_CLIENT_ID -s 'redirectUris=["http://localhost:8980/testiafService/*"]' -i)

#set known passwords
$KCADM update -r iaf-test clients/$CID_CLIENT  -s secret=$CLIENT_APP_CLIENT_SECRET
$KCADM update -r iaf-test clients/$CID_SERVICE -s secret=$SERVICE_APP_CLIENT_SECRET

#enable ClientCredentials flow
$KCADM update -r iaf-test clients/$CID_CLIENT  -s serviceAccountsEnabled=true

#echo "export client config"
#$KCADM get -r iaf-test clients/$CID_CLIENT/installation/providers/keycloak-oidc-keycloak-json > /opt/shared/testiaf-client.json
#$KCADM get -r iaf-test clients/$CID_SERVICE/installation/providers/keycloak-oidc-keycloak-json > /opt/shared/testiaf-service.json

#$KCADM get -r iaf-test clients -F clientId,id,redirectUris,secret > /opt/shared/clients.json