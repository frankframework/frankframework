cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'

#AdminTask.showJVMProperties(['-serverName', server, '-nodeName', node])

def setJVMSystemProperty(name, value ):
	print "Setting JVM System property:", name, "=", value
	propertyId = AdminTask.setJVMSystemProperties(['-serverName', server, '-nodeName', node, '-propertyName', name, '-propertyValue', value])
	return(propertyId)

def setJVMProperty(name, value ):
	print "Setting JVM property:", name, "=", value
	propertyId = AdminTask.setJVMProperties(['-serverName', server, '-nodeName', node, name, value])
	return(propertyId)

def setSecurityProperty(name, value ):
	print "Setting Security property:", name, "=", value
	security = AdminConfig.list('Security')
	AdminConfig.modify(security, [[name, value]])
	return

setJVMSystemProperty('log.dir', '${SERVER_LOG_ROOT}')
setJVMSystemProperty('dtap.stage', 'TST')
setJVMSystemProperty('web.protocol', 'http')
setJVMSystemProperty('web.port', '9080')
setJVMSystemProperty('scenariosroot1.directory', '/opt/frank/testtool')
setJVMSystemProperty('scenariosroot1.description', 'embedded testtool directory /opt/frank/testtool')
setJVMSystemProperty('scenariosroot2.directory', '/opt/frank/testtool-ext')
setJVMSystemProperty('scenariosroot2.description', 'external testtool directory /opt/frank/testtool-ext')
setJVMSystemProperty('jdbc.dbms.default', 'oracle')
setJVMSystemProperty('active.jms', 'false')
setJVMSystemProperty('active.tibco', 'false')
setJVMSystemProperty('active.ifsa', 'false')
setJVMSystemProperty('test.alias', 'testalias')
setJVMSystemProperty('authAliases.expansion.allowed', 'testalias')
setJVMSystemProperty('APPSERVER_ROOT_DIR', '${USER_INSTALL_ROOT}') # APPSERVER_ROOT_DIR is a NN standard custom property for WAS
setJVMSystemProperty('com.ibm.websphere.java2secman.norethrow', 'true')

#setJVMSystemProperty('javax.net.debug', 'ssl, handshake, data, trustmanager')
#setJVMSystemProperty('javax.net.debug', 'all')

setSecurityProperty('enforceJava2Security', 'true')


AdminConfig.save()
	