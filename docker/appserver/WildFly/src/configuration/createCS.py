import os

path_to_secrets="/opt/frank/secrets/"
secretsList = os.listdir(path_to_secrets)

os.system("/opt/jboss/wildfly/bin/elytron-tool.sh credential-store --create --location '/opt/jboss/wildfly/standalone/data/CS.jceks' --password secret")

for secret in secretsList:
	password=open(path_to_secrets+secret+"/password").read()
	os.system("/opt/jboss/wildfly/bin/elytron-tool.sh credential-store --location '/opt/jboss/wildfly/standalone/data/CS.jceks' --password secret -add "+secret+" --secret "+password)