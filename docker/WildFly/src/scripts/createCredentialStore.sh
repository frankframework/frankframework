#!/bin/bash

# Converts credentials file into WildFly credentials

SERVER_DIR=/opt/jboss/wildfly
SECRETS_TOOL=$SERVER_DIR/bin/elytron-tool.sh
SECRETS_LOCATION=$SERVER_DIR/standalone/data/CS.jceks
SECRETS_PASSWORD=secret

SECRETS_FILE="/opt/frank/secrets/credentials.properties"

$SECRETS_TOOL credential-store --create --location "$SECRETS_LOCATION" --password "$SECRETS_PASSWORD"

add_alias() {
	name=$1
	secret=$2
	$SECRETS_TOOL credential-store --location "$SECRETS_LOCATION" --password "$SECRETS_PASSWORD" --add "$name" --secret "$secret"
}

echo "Adding credentials from $SECRETS_FILE"

if [ -f "$SECRETS_FILE" ]; then
	while read -r line; do
		if [ -z "$line" ]; then
			continue
		fi
		IFS='=' read -r alias_and_key secret <<< "$line"
		IFS='/' read -r alias key <<< "$alias_and_key"
		if [ "$key" == "password" ]; then
			add_alias "$alias" "$secret"
		else
			add_alias "$alias/$key" "$secret"
		fi
	done < "$SECRETS_FILE"
else
	echo "No credentials file found"
fi
