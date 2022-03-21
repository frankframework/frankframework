#!/bin/sh
#
# dockerInit.sh
#

setup() {
	mkdir /var/opt/mssql/iaf-test-data/
	chown mssql /var/opt/mssql/iaf-test-data/
	chgrp mssql /var/opt/mssql/iaf-test-data/
	
	echo "Running create_user script"
	for i in {1..120};
		do
		    /opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P MssqlPass123 -d master -i /create_user.sql
		    if [ $? -eq 0 ]
		    then
		        echo "create_user completed"
		        break
		    else
		        sleep 1
		    fi
		done
}

setup &

/opt/mssql/bin/sqlservr
