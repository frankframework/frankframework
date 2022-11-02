# createCS.sh
#
# converts filesystem credentials into WildFly credentials

SERVER_DIR=/home/jboss/jboss-eap-7.3
SECRETS_TOOL=$SERVER_DIR/bin/elytron-tool.sh
SECRETS_LOCATION=$SERVER_DIR/standalone/data/CS.jceks
SECRETS_PASSWORD=secret

SOURCE_PATH="/opt/frank/secrets"

$SECRETS_TOOL credential-store --create --location $SECRETS_LOCATION --password $SECRETS_PASSWORD

add_alias() {
	name=$1
	file=$SOURCE_PATH/$2
	if [ -f $file ]
	then
		$SECRETS_TOOL credential-store --location $SECRETS_LOCATION --password $SECRETS_PASSWORD --add "$name" --secret "`cat $file`"
	fi
}

for alias in `ls $SOURCE_PATH`
do
	if [ -d $SOURCE_PATH/$alias ] 
	then 
		add_alias $alias/username $alias/username
		add_alias $alias          $alias/password
	else
		add_alias $alias $alias
	fi
done	