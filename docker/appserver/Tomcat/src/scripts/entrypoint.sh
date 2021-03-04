#!/bin/bash
set -e

if [ "$1" = 'catalina.sh' ]; then
	# Fix permissions
	if [[ "${SET_PERMISSIONS_ON_STARTUP}" != 'FALSE' ]]; then
		echo Setting permissions on startup
		echo To improve startup time, run /setPermissions.sh and set environment variable SET_PERMISSIONS_ON_STARTUP to FALSE as last step in the Dockerfile to skip this step 
		/setPermissions.sh
	fi
	
	exec gosu tomcat "$@"
fi

exec "$@"