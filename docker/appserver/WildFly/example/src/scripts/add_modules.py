import os
# base path to resources, configurations jars
path_to_jars='/opt/jboss/wildfly/standalone/lib/ext/'
# name of the module that war file will depend on
moduleName='module.nl.nn.adapterframework'
# list of jar files
jarList = os.listdir(path_to_jars)
# resources that the module will be created upon
resources = ''
resourceDelimiter=','
# client executable path
jboss_cli = '/opt/jboss/wildfly/bin/jboss-cli.sh'

for jarFile in jarList:
	resources += path_to_jars+jarFile+resourceDelimiter

resources=resources[0:-1]

command="module add --name="+moduleName+" --resources="+resources+" --resource-delimiter="+resourceDelimiter

os.system(jboss_cli+" --command='"+command+"'")

