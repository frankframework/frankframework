cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'

def installApp( ear, module, war ):
	print "installing app:",ear
	AdminApp.install('/work/app/'+ear,
		[ '-nopreCompileJSPs', '-distributeApp', '-nouseMetaDataFromBinary', '-nodeployejb',
		  '-appname', ear,
		  '-createMBeansForResources', '-noreloadEnabled', '-nodeployws', '-validateinstall', 'warn', '-processEmbeddedConfig',
		  '-filepermission', '.*\.dll=755#.*\.so=755#.*\.a=755#.*\.sl=755', '-noallowDispatchRemoteInclude', '-noallowServiceRemoteInclude',
		  '-asyncRequestDispatchType', 'DISABLED', '-nouseAutoLink', '-noenableClientModule', '-clientMode', 'isolated', '-novalidateSchema',
		  '-MapModulesToServers',        [[ module, war+',WEB-INF/web.xml', 'WebSphere:cell='+cell+',node='+node+',server='+server ]],
		  '-MapWebModToVH',              [[ module, war+',WEB-INF/web.xml', 'default_host' ]],
		  '-MetadataCompleteForModules', [[ module, war+',WEB-INF/web.xml', 'true' ]]
		] )
	return


def setClassloaderMode( ear, mode ):
	deployments = AdminConfig.getid('/Deployment:ibis-adapterframework-test.ear/')
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


installApp('ibis-adapterframework-test.ear', 'IBIS AdapterFramework', 'adapterframework.war')

AdminConfig.save()

setClassloaderMode('ibis-adapterframework-test.ear', 'PARENT_LAST')


AdminConfig.save()