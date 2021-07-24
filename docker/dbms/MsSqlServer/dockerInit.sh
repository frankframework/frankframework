#!/bin/sh
#
# dockerInit.sh
#

setup() {
#wait for the SQL Server to come up
sleep 25s

mkdir /var/opt/mssql/iaf-test-data/
chown mssql /var/opt/mssql/iaf-test-data/
chgrp mssql /var/opt/mssql/iaf-test-data/

echo "running set up script"
#run the setup script to create the DB and the schema in the DB
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P MssqlPass123 -d master -i /create_user.sql
}

setup &

/opt/mssql/bin/sqlservr
