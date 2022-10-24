import os

path_to_secrets="/opt/frank/secrets/"
secretsList = os.listdir(path_to_secrets)

os.system("/home/jboss/jboss-eap-7.3/bin/elytron-tool.sh credential-store --create --location '/home/jboss/jboss-eap-7.3/standalone/data/CS.jceks' --password secret")

for secret in secretsList:
	password=open(path_to_secrets+secret+"/password").read()
	os.system("/home/jboss/jboss-eap-7.3/bin/elytron-tool.sh credential-store --location '/home/jboss/jboss-eap-7.3/standalone/data/CS.jceks' --password secret -add "+secret+" --secret "+password)