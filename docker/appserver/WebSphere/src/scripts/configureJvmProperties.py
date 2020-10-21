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
setJvmProperty( 'scenariosroot1.directory', '${USER_INSTALL_ROOT}/installedApps/DefaultCell01/adapterframework.ear.ear/adapterframework.war/testtool')
setJvmProperty( 'scenariosroot1.description', 'testtool directory in unpacked ear')
setJvmProperty( 'jdbc.dbms.default', 'oracle-docker')
setJvmProperty( 'active.jms', 'false')
setJvmProperty( 'active.tibco', 'false')
setJvmProperty( 'active.ifsa', 'false')
setJvmProperty( 'log.dir.match', '(?i)(([cd]:[\\\\/]temp)|(${SERVER_LOG_ROOT}))')
setJvmProperty( 'test.alias', 'testAuthAlias')
setJvmProperty( 'APPSERVER_ROOT_DIR', '${USER_INSTALL_ROOT}') # APPSERVER_ROOT_DIR is a NN standard custom property for WAS

AdminConfig.save()
	