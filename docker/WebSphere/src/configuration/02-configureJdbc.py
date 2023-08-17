cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'

#p0 = AdminConfig.listTemplates('JDBCProvider')
#print('JDBCProvider templates: ', p0)
#p1 = AdminConfig.listTemplates('DataSource')
#print('DataSource templates: ', p1)

def createTemplatedProvider(templateName, implementationClass, classpath):
	print("Creating JDBC Provider using template: ", templateName)
	providerName = templateName
	attributes = 'classpath='+classpath
	providerTemplate = AdminConfig.listTemplates('JDBCProvider', templateName)
	providerId = AdminJDBC.createJDBCProviderUsingTemplate(node, server, providerTemplate, providerName, implementationClass, attributes)
	return(providerId)

def createProvider(providerName, implementationClass, attributes):
	print("Creating JDBC Provider: ", providerName)
	providerId = AdminJDBC.createJDBCProvider(node, server, providerName, implementationClass, attributes)
	return(providerId)

def createTemplatedDatasource(datasourceName, providerName, templateName, authAlias, properties):
	print("Creating Datasource using template: ", datasourceName)
	attributes = [['jndiName', 'jdbc/'+datasourceName], ['authDataAlias', authAlias], ['propertySet', [['resourceProperties', properties]]]]
	template = AdminConfig.listTemplates('DataSource', templateName)
	datasourceId = AdminJDBC.createDataSourceUsingTemplate(node, server, providerName, template, datasourceName, attributes)
	return(datasourceId)

def createDatasource(datasourceName, providerName, authAlias, properties):
	print("Creating Datasource: ", datasourceName)
	attributes = [['jndiName', 'jdbc/'+datasourceName], ['datasourceHelperClassname', 'com.ibm.websphere.rsadapter.GenericDataStoreHelper'], ['authDataAlias', authAlias], ['propertySet', [['resourceProperties', properties]]]]
	datasourceId = AdminJDBC.createDataSource(node, server, providerName, datasourceName, attributes)
	return(datasourceId)

createTemplatedProvider('Oracle JDBC Driver (XA)', 'oracle.jdbc.xa.client.OracleXADataSource', '/work/drivers/ojdbc${oracle.driver.jdkversion}.jar')
createTemplatedProvider('Microsoft SQL Server JDBC Driver (XA)', 'com.microsoft.sqlserver.jdbc.SQLServerXADataSource', '/work/drivers/mssql-jdbc.jar')
createProvider('H2 JDBC Driver (XA)', 'org.h2.jdbcx.JdbcDataSource', 'classpath=/work/drivers/h2.jar,xa=true')
createProvider('MySQL JDBC Driver', 'com.mysql.cj.jdbc.MysqlXADataSource', 'classpath=/work/drivers/mysql-connector-j.jar')
createProvider('MariaDB JDBC Driver', 'org.mariadb.jdbc.MariaDbDataSource', 'classpath=/work/drivers/mariadb-java-client.jar')
createProvider('PostgreSQL JDBC Driver', 'org.postgresql.xa.PGXADataSource', 'classpath=/work/drivers/postgresql.jar')
createProvider('DB2 JDBC Driver (XA)', 'com.ibm.db2.jcc.DB2XADataSource', 'classpath=/work/drivers/jcc.jar')
createProvider('DB2 JDBC Driver', 'com.ibm.db2.jcc.DB2ConnectionPoolDataSource', 'classpath=/work/drivers/jcc.jar')

createDatasource('ibis4test-h2', 'H2 JDBC Driver (XA)', [], [
		[['name', 'URL'], ['value', 'jdbc:h2:file:/work/ibis4test;NON_KEYWORDS=VALUE;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE']]
	])

authAliasName = 'testiaf_user'

createTemplatedDatasource('ibis4test-oracle', 'Oracle JDBC Driver (XA)', 'Oracle JDBC Driver XA DataSource', authAliasName, [
		[['name', 'URL'], ['value', 'jdbc:oracle:thin:@${jdbc.hostname}:1521:XE']]
	])

createTemplatedDatasource('ibis4test-mssql', 'Microsoft SQL Server JDBC Driver (XA)', 'Microsoft SQL Server JDBC Driver - XA DataSource', authAliasName, [
		[['name', 'serverName'], ['value', '${jdbc.hostname}']],
		[['name', 'portNumber'], ['value', '1433']],
		[['name', 'databaseName'], ['value', 'testiaf']]
	])


createDatasource('ibis4test-mysql', 'MySQL JDBC Driver', authAliasName, [
		[['name', 'URL'], ['value', 'jdbc:mysql://${jdbc.hostname}:3307/testiaf']],
		[['name', 'serverTimezone'], ['value', 'Europe/Amsterdam']],
		[['name', 'allowPublicKeyRetrieval'], ['value', 'true']],
		[['name', 'pinGlobalTxToPhysicalConnection'], ['value', 'true']],
		[['name', 'socketTimeout'], ['value', '5000']]
	])

createDatasource('ibis4test-mariadb', 'MariaDB JDBC Driver', authAliasName, [
		[['name', 'url'], ['value', 'jdbc:mariadb://${jdbc.hostname}:3306/testiaf']],
	])

createDatasource('ibis4test-postgres-xa', 'PostgreSQL JDBC Driver', authAliasName, [
		[['name', 'URL'], ['value', 'jdbc:postgresql://${jdbc.hostname}:5432/testiaf']]
	])

createDatasource('ibis4test-db2-xa', 'DB2 JDBC Driver (XA)', authAliasName, [
		[['name', 'serverName'], ['value', '${jdbc.hostname}']],
		[['name', 'portNumber'], ['value', '50000']],
		[['name', 'databaseName'], ['value', 'testiaf']],
		[['name', 'driverType'], ['value', '4']]
	])

createDatasource('ibis4test-db2', 'DB2 JDBC Driver', authAliasName, [
	[['name', 'url'], ['value', 'jdbc:db2://${jdbc.hostname}:50000/testiaf']]
])
