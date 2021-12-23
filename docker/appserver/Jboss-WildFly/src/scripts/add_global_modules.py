import os

JBOSS_HOME = os.getenv("JBOSS_HOME")

# base path to resources, configurations jars
path_to_jars = JBOSS_HOME+"/standalone/standalone/deployments/"

# client executable path
jboss_cli = JBOSS_HOME+"/bin/jboss-cli.sh"

command="module add --name=org.reactivestreams --resources="+path_to_jars+"reactive-streams.jar"
os.system(jboss_cli+" --command='"+command+"'")