import os


# name of the module that war file will depend on
moduleName = "module.nl.nn.adapterframework"

# list of jar files
path_to_jars="/opt/jboss/wildfly/standalone/lib/ext/"
jarList = os.listdir(path_to_jars)

# resources that the module will be created upon
resources = ""
resourceDelimiter = ':'


for jarFile in jarList:
	resources += path_to_jars+jarFile+resourceDelimiter

resources = resources[0:-1]

command="module add --name="+moduleName+" --resources="+resources

os.system("/opt/jboss/wildfly/bin/jboss-cli.sh --command='"+command+"'")