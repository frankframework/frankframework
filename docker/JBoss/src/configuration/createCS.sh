# createCS.sh
#
# converts filesystem credentials into WildFly credentials

SECRETS_TOOL=$JBOSS_HOME/bin/elytron-tool.sh
SECRETS_LOCATION=$JBOSS_HOME/standalone/data/CS.jceks
SECRETS_PASSWORD=secret

SOURCE_PATH="/opt/frank/secrets"

$SECRETS_TOOL credential-store --create --location $SECRETS_LOCATION --password $SECRETS_PASSWORD

add_alias() {
	name=$1
	file=$SOURCE_PATH/$2
	if [ -f $file ]
	then
		$SECRETS_TOOL credential-store --location $SECRETS_LOCATION --password $SECRETS_PASSWORD --add "$name" --secret "$(cat "$file")"
	fi
}

cd $SOURCE_PATH || return
for alias in *
do
	if [ -d $alias ] 
	then 
		add_alias "$alias/username" "$alias/username"
		add_alias "$alias"          "$alias/password"
	else
		add_alias "$alias" "$alias"
	fi
done
