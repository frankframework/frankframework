import os

jbossHomeDir = os.environ['JBOSS_HOME']

# name of the module that war file will depend on
moduleName = "module.frank-framework"

# list of jar files
path_to_jars = jbossHomeDir + "/standalone/lib/ext/"
jarList = os.listdir(path_to_jars)

# resources that the module will be created upon
resources = ""
resourceDelimiter = ':'

for jarFile in jarList:
    resources += path_to_jars + jarFile + resourceDelimiter

resources = resources[0:-1]

command = "module add --name=" + moduleName + " --resources=" + resources

print(command)

os.system(jbossHomeDir + "/bin/jboss-cli.sh --command='" + command + "'")
