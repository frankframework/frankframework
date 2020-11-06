cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'


def createAuthAlias( aliasName, username, password, description ):
	print "Creating Auth Alias ", aliasName
	security = AdminConfig.getid('/Security:/')
	alias = ['alias', aliasName ]
	userid = ['userId', username ]
	pw = ['password', password ]
	descr = ['description', description ]
	jaasAttrs = [alias, userid, pw, descr]
	aliasId = AdminConfig.create('JAASAuthData', security, jaasAttrs)
	AdminConfig.save()
	return(aliasId)

def createTemplatedProvider(templateName, implementationClass, classpath):
	print "Creating JDBC Provider using template: ", templateName
	providerName = templateName
	attributes = 'classpath='+classpath
	providerTemplate = AdminConfig.listTemplates('JDBCProvider', templateName)
	providerId = AdminJDBC.createJDBCProviderUsingTemplate(node, server, providerTemplate, providerName, implementationClass, attributes)
	return(providerId)

def createProvider(providerName, implementationClass, attributes):
	print "Creating JDBC Provider: ", providerName
	providerId = AdminJDBC.createJDBCProvider(node, server, providerName, implementationClass, attributes)
	return(providerId)

def createTemplatedDatasource(datasourceName, providerName, templateName, authAlias, properties):
	print "Creating Datasource using template: ", datasourceName
	attributes = [ [ 'jndiName','jdbc/'+datasourceName ],['authDataAlias',authAlias ], ['propertySet', [[ 'resourceProperties', properties ]]] ]
	template = AdminConfig.listTemplates('DataSource', templateName)
	datasourceId = AdminJDBC.createDataSourceUsingTemplate(node, server, providerName, template, datasourceName, attributes)
	return(datasourceId)

def createDatasource(datasourceName, providerName, authAlias, properties):
	print "Creating Datasource: ", datasourceName
	attributes = [ [ 'jndiName','jdbc/'+datasourceName ],[ 'datasourceHelperClassname','com.ibm.websphere.rsadapter.GenericDataStoreHelper' ],['authDataAlias',authAlias ], ['propertySet', [[ 'resourceProperties', properties ]]] ]
	datasourceId = AdminJDBC.createDataSource(node, server, providerName, datasourceName, attributes)
	return(datasourceId)


#p0 = AdminConfig.listTemplates('JDBCProvider')
#print 'JDBCProvider templates: ', p0
#p1 = AdminConfig.listTemplates('DataSource')
#print 'DataSource templates: ', p1




createTemplatedProvider('Oracle JDBC Driver (XA)', 		 'oracle.jdbc.xa.client.OracleXADataSource', 		'/work/drivers/ojdbc${oracle.driver.jdkversion}.jar')
createTemplatedProvider('Microsoft SQL Server JDBC Driver (XA)', 'com.microsoft.sqlserver.jdbc.SQLServerXADataSource',  '/work/drivers/mssql-jdbc.jar')
createProvider('H2 JDBC Driver (XA)', 'org.h2.jdbcx.JdbcDataSource', 'classpath=/work/drivers/h2.jar,xa=true')

createDatasource('ibis4test-h2', 'H2 JDBC Driver (XA)', [], [
		[['name', 'URL'],['value', 'jdbc:h2:file:/work/ibis4test;MODE=Oracle;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=TRUE']]
	])

authAliasName = 'jdbcTestIaf'

createTemplatedDatasource('ibis4test-oracle-docker', 'Oracle JDBC Driver (XA)', 'Oracle JDBC Driver XA DataSource', authAliasName, [
		[['name', 'URL'],['value', 'jdbc:oracle:thin:@host.docker.internal:1521:ORCLCDB']]
	])

createTemplatedDatasource('ibis4test-mssql', 'Microsoft SQL Server JDBC Driver (XA)', 'Microsoft SQL Server JDBC Driver - XA DataSource', authAliasName, [
		[['name', 'serverName'],  ['value', 'host.docker.internal']],
		[['name', 'portNumber'],  ['value', '1433']],
		[['name', 'databaseName'],['value', 'testiaf']]
	])


createAuthAlias( authAliasName, 'testiaf_user', 'testiaf_user00', 'alias for iaf-test datasources' )
createAuthAlias( 'testAuthAlias', 'testUser', 'testPassword', 'alias for authentication tests' )

