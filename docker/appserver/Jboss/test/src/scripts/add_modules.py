import os
# base path to resources, configurations jars
path_to_jars='/home/jboss/jboss-eap-6.4/standalone/lib/ext/'
# name of the module that war file will depend on
moduleName='module.frankframework.resources'
# list of jar files
jarList = os.listdir(path_to_jars)
# resources that the module will be created upon
resources = ''
resourceDelimiter=':'
# client executable path
jboss_cli = '/home/jboss/jboss-eap-6.4/bin/jboss-cli.sh'

for jarFile in jarList:
	resources += path_to_jars+jarFile+resourceDelimiter

resources=resources[0:-1]

command="module add --name="+moduleName+" --resources="+resources

os.system(jboss_cli+" --command='"+command+"'")

