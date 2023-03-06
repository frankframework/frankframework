import os

cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'


def createAuthAlias(aliasName, username, password):
	print("Creating Auth Alias ", aliasName)
	security = AdminConfig.getid('/Security:/')
	alias = ['alias', aliasName ]
	userid = ['userId', username ]
	pw = ['password', password ]
	jaasAttrs = [alias, userid, pw]
	aliasId = AdminConfig.create('JAASAuthData', security, jaasAttrs)
	AdminConfig.save()
	return(aliasId)

# list secrets
path_to_secrets="/opt/frank/secrets/"
secretsList = os.listdir(path_to_secrets)

for secret in secretsList:
	aliasName = secret
	username = ""
	try:
		username=open(path_to_secrets+secret+"/username").read()
	except:
		print("Secret "+secret+" has no username")
	password=open(path_to_secrets+secret+"/password").read()
	
	createAuthAlias(aliasName, username, password)