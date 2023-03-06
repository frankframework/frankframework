cell = AdminControl.getCell()
node = AdminControl.getNode()
nodeId = AdminConfig.getid('/Cell:'+cell+'/Node:'+node+'/')

server = 'server1'
contextRoot = '/iaf-test'

def installApp(ear, module, war, contextRoot):
	print "installing app:", ear, "contextRoot:", contextRoot
	AdminApp.install('/work/app/'+ear,
		['-nopreCompileJSPs', '-distributeApp', '-nouseMetaDataFromBinary', '-nodeployejb',
		 '-appname', ear,
		 '-createMBeansForResources', '-noreloadEnabled', '-nodeployws', '-validateinstall', 'warn', '-processEmbeddedConfig',
		 '-filepermission', '.*\.dll=755#.*\.so=755#.*\.a=755#.*\.sl=755', '-noallowDispatchRemoteInclude', '-noallowServiceRemoteInclude',
		 '-asyncRequestDispatchType', 'DISABLED', '-nouseAutoLink', '-noenableClientModule', '-clientMode', 'isolated', '-novalidateSchema',
		 '-MapModulesToServers', [[module, war+',WEB-INF/web.xml', 'WebSphere:cell='+cell+',node='+node+',server='+server]],
		 '-MapWebModToVH', [[module, war+',WEB-INF/web.xml', 'default_host']],
		 '-MetadataCompleteForModules', [[module, war+',WEB-INF/web.xml', 'true']],
		 '-CtxRootForWebMod', [[module, war+',WEB-INF/web.xml', contextRoot]]
		])
	return


def setClassloaderMode(ear, mode):
	deployments = AdminConfig.getid('/Deployment:'+ear+'/')
	print "deployments:", deployments
	deployedObject = AdminConfig.showAttribute(deployments, 'deployedObject')
	print "deploymentObject:", deployedObject
	# set Application classloader mode
	classldr = AdminConfig.showAttribute(deployedObject, 'classloader')
	AdminConfig.modify(classldr, [['mode', mode]])
	modules = AdminConfig.showAttribute(deployedObject, 'modules')
	modules = modules[1:len(modules)-1].split(" ")
	print "modules:", modules
	# set WebModule classloader mode
	for module in modules:
		if (module.find('WebModuleDeployment')!= -1):
			print "setting module:", module, "to classloaderMode:", mode
			AdminConfig.modify(module, [['classloaderMode', mode]])
	return

def createSharedLibrary(name, classpath):
	print "creating shared library:", name
	print AdminConfig.create('Library', nodeId, [['name', name], ['classPath', classpath]])
	return

def assignSharedLibrary(ear, name):
	deployments = AdminConfig.getid('/Deployment:'+ear+'/')
	print "deployments:", deployments
	deployedObject = AdminConfig.showAttribute(deployments, 'deployedObject')
	print "deploymentObject:", deployedObject
	# set Application classloader mode
	classldr = AdminConfig.showAttribute(deployedObject, 'classloader')
	print AdminConfig.create('LibraryRef', classldr, [['libraryName', name], ['sharedClassloader', 'true']])
	return

installApp('adapterframework.ear', 'Frank!Framework', 'adapterframework.war', contextRoot)
setClassloaderMode('adapterframework.ear', 'PARENT_LAST')


createSharedLibrary('frankConfig', '/opt/frank/configuration/resources.jar /opt/frank/configuration/configurations.jar')
assignSharedLibrary('adapterframework.ear', 'frankConfig')

AdminConfig.save()