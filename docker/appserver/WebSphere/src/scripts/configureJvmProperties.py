cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'



def setJvmProperty( name, value ):
	print "Setting JVM property:",name,"=",value
	propertyId = AdminTask.setJVMSystemProperties([ '-serverName', server, '-nodeName', node, '-propertyName', name, '-propertyValue', value ]) 
	return(propertyId)

setJvmProperty( 'log.dir', '${SERVER_LOG_ROOT}')
setJvmProperty( 'otap.stage', 'TST')
setJvmProperty( 'web.protocol', 'http')
setJvmProperty( 'web.port', '9080')
setJvmProperty( 'strutsConsole.enabled', 'true')
setJvmProperty( 'scenariosroot1.directory', '/opt/IBM/WebSphere/AppServer/profiles/AppSrv01/installedApps/DefaultCell01/ibis-adapterframework-test.ear.ear/adapterframework.war/testtool')
setJvmProperty( 'scenariosroot1.description', 'testtool directory in unpacked ear')
setJvmProperty( 'jdbc.dbms.default', 'oracle-docker')
setJvmProperty( 'active.jms', 'false')
setJvmProperty( 'active.tibco', 'false')
setJvmProperty( 'active.ifsa', 'false')
setJvmProperty( 'log.dir.match', '(?i)(([cd]:[\\\\/]temp)|(${SERVER_LOG_ROOT}))')
setJvmProperty( 'test.alias', 'testAuthAlias')


AdminConfig.save()
	