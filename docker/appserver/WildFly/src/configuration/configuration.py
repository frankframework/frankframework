import os

# name of the module that war file will depend on
moduleName = "module.frank-framework"

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

print(command)

os.system("/opt/jboss/wildfly/bin/jboss-cli.sh --command='"+command+"'")

path_to_secrets="/opt/frank/secrets/"
secretsList = os.listdir(path_to_secrets)

os.system("/opt/jboss/wildfly/bin/elytron-tool.sh credential-store --create --location '/opt/jboss/wildfly/standalone/data/CS.jceks' --password secret")

for secret in secretsList:
	password=open(path_to_secrets+secret+"/password").read()
	os.system("/opt/jboss/wildfly/bin/elytron-tool.sh credential-store --location '/opt/jboss/wildfly/standalone/data/CS.jceks' --password secret -add "+secret+" --secret "+password)