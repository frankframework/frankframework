import os

cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'


def createAuthAlias( aliasName, username, password, description ):
	print "Creating Auth Alias ", aliasName
	security = AdminConfig.getid('/Security:/')
	alias = ['alias', aliasName ]
	userid = ['userId', username ]
	pw = ['password', password ]
	descr = ['description', description ]
	jaasAttrs = [alias, userid, pw, descr]
	aliasId = AdminConfig.create('JAASAuthData', security, jaasAttrs)
	AdminConfig.save()
	return(aliasId)

# list secrets
path_to_secrets="/opt/frank/secrets/"
secretsList = os.listdir(path_to_secrets)

for secret in secretsList:
	aliasName = secret
	username=open(path_to_secrets+secret+"/username").read()
	password=open(path_to_secrets+secret+"/password").read()
	print(aliasName)
	print(username)
	print(password)
	
	createAuthAlias( aliasName, username, password, '')