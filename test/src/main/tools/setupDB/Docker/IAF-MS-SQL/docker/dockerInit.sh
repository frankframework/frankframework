#wait for the SQL Server to come up
sleep 25s

mkdir /var/opt/mssql/wearefrank-data/
chown mssql /var/opt/mssql/wearefrank-data/
chgrp mssql /var/opt/mssql/wearefrank-data/

echo "enable XA"
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P MssqlPass123 -d master -i xa_install.sql
echo "running set up script"
#run the setup script to create the DB and the schema in the DB
/opt/mssql-tools/bin/sqlcmd -S localhost -U sa -P MssqlPass123 -d master -i setup.sql
