cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'

#AdminTask.showJVMProperties([ '-serverName', server, '-nodeName', node ])

def setJVMSystemProperty( name, value ):
	print "Setting JVM System property:",name,"=",value
	propertyId = AdminTask.setJVMSystemProperties([ '-serverName', server, '-nodeName', node, '-propertyName', name, '-propertyValue', value ])
	return(propertyId)

def setJVMProperty( name, value ):
	print "Setting JVM property:",name,"=",value
	propertyId = AdminTask.setJVMProperties([ '-serverName', server, '-nodeName', node, name, value ])
	return(propertyId)

setJVMSystemProperty( 'log.dir', '${SERVER_LOG_ROOT}')
setJVMSystemProperty( 'otap.stage', 'TST')
setJVMSystemProperty( 'web.protocol', 'http')
setJVMSystemProperty( 'web.port', '9080')
setJVMSystemProperty( 'strutsConsole.enabled', 'true')
setJVMSystemProperty( 'scenariosroot1.directory', '/work/frank/testtool')
setJVMSystemProperty( 'scenariosroot1.description', 'testtool directory /work/frank/testtool')
setJVMSystemProperty( 'jdbc.dbms.default', 'oracle-docker')
setJVMSystemProperty( 'active.jms', 'false')
setJVMSystemProperty( 'active.tibco', 'false')
setJVMSystemProperty( 'active.ifsa', 'false')
setJVMSystemProperty( 'log.dir.match', '(?i)(([cd]:[\\\\/]temp)|(${SERVER_LOG_ROOT}))')
setJVMSystemProperty( 'test.alias', 'testAuthAlias')
setJVMSystemProperty( 'APPSERVER_ROOT_DIR', '${USER_INSTALL_ROOT}') # APPSERVER_ROOT_DIR is a NN standard custom property for WAS

setJVMProperty( '-classpath', '[ /work/frank/configuration ]')

AdminConfig.save()
	