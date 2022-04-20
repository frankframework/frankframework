#!/bin/bash
set -e

if [ "$1" = 'catalina.sh' ]; then
	# The base tomcat image runs as "root", which allows developers an easy way to start working with the base image
	# For enhanced security, we do not run the tomcat-process as "root" but as "tomcat", requiring that we fix the file permissions
	# As configurations can be copied in during builds starting from our image and might have "root" set as owner, root is required to set the file permissions
	# It is not possible to add a build trigger to set the permissions after build from our Dockerfile, so by default we set the permissions during startup
	# To improve startup times, developers can execute the step as part of their Dockerfile and set SET_PERMISSIONS_ON_STARTUP to FALSE to skip it during startup
	# 
	# Additionaly, staying root at the end of our Dockerfile simplifies Dockerfiles that start from our image as there is no need to switch users to perform root
	# actions, we perform the switch to the "tomcat"-user on startup
	if [[ "${SET_PERMISSIONS_ON_STARTUP}" != 'FALSE' ]]; then
		echo Setting permissions on startup
		echo To improve startup time, run /setPermissions.sh and set environment variable SET_PERMISSIONS_ON_STARTUP to FALSE as last step in the Dockerfile to skip this step 
		/setPermissions.sh
	fi
	
	exec gosu tomcat "$@"
fi

exec "$@"