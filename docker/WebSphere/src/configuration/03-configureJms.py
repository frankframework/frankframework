#Documentation: https://www.ibm.com/docs/en/was/9.0.5?topic=scripting-jms-configuration-scripts
cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'

def createJMSProvider(name, extContextFactory, extProviderURL, attributes):
	print("Creating JMS Provider: ", name)
	jmsProviderId = AdminJMS.createJMSProvider(node, server, name, extContextFactory, extProviderURL, attributes)
	return(jmsProviderId)

def createGenericJMSCF(jmsProviderName, name, jndiName, extJndiName):
	print("Creating Generic JMS Connection Factory: ", name)
	attributes = [['connectionPool', [['agedTimeout', '100'], ['connectionTimeout', '1000'], ['freePoolDistributionTableSize', 10], ['maxConnections', '12'], ['minConnections', '5'], ['numberOfFreePoolPartitions', '3'], ['numberOfSharedPoolPartitions', '6'], ['numberOfUnsharedPoolPartitions', '3'], ['purgePolicy', 'EntirePool'], ['reapTime', '10000'], ['surgeCreationInterval', '10'], ['surgeThreshold', '10'], ['testConnection', 'true'], ['testConnectionInterval', '10'], ['unusedTimeout', '10000']]]]
	jmsCFId = AdminJMS.createGenericJMSConnectionFactory(node, server, jmsProviderName, name, jndiName, extJndiName, attributes)
	return(jmsCFId)

def createGenericJMSDestination(jmsProviderName, name, jndiName, extJndiName):
	print("Creating Generic JMS Destination: ", name)
	jmsDestinationId = AdminJMS.createGenericJMSDestination(node, server, jmsProviderName, name, jndiName, extJndiName)
	return(jmsDestinationId)

# ActiveMQ
createJMSProvider('ActiveMQ', 'org.apache.activemq.jndi.ActiveMQInitialContextFactory', 'tcp://${jms.hostname}:61616?jms.xaAckMode=1', 'classpath=/work/drivers/activemq-client.jar;/work/drivers/hawtbuf.jar;/work/drivers/slf4j-api.jar')

createGenericJMSCF('ActiveMQ', 'qcf', 'jms/qcf-activemq', 'XAConnectionFactory')

createGenericJMSDestination('ActiveMQ', 'i4testiaf_in-activemq', 'jms/i4testiaf_in-activemq', 'dynamicQueues/Q.TEST.IN')
createGenericJMSDestination('ActiveMQ', 'i4testiaf_out-activemq', 'jms/i4testiaf_out-activemq', 'dynamicQueues/Q.TEST.OUT')
createGenericJMSDestination('ActiveMQ', 'i4testiaf_ff-activemq', 'jms/i4testiaf_ff-activemq', 'dynamicQueues/Q.TEST.FF')

# Artemis
createJMSProvider('Artemis', 'org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory', 'tcp://${jms.hostname}:61615?type=XA_CF', 'classpath=/work/drivers/artemis-jms-client-all.jar')

createGenericJMSCF('Artemis', 'qcf', 'jms/qcf-artemis', 'XAConnectionFactory')

createGenericJMSDestination('Artemis', 'i4testiaf_in-artemis', 'jms/i4testiaf_in-artemis', 'dynamicQueues/Q.TEST.IN')
createGenericJMSDestination('Artemis', 'i4testiaf_out-artemis', 'jms/i4testiaf_out-artemis', 'dynamicQueues/Q.TEST.OUT')
createGenericJMSDestination('Artemis', 'i4testiaf_ff-artemis', 'jms/i4testiaf_ff-artemis', 'dynamicQueues/Q.TEST.FF')


#p0 = AdminJMS.listJMSProviders()
#print('JMS Provider templates: ', p0)
#p1 = AdminJMS.listGenericJMSConnectionFactories()
#print('JMS CF: ', p1)
#p2 = AdminJMS.listGenericJMSDestinations()
#print('JMS Destination: ', p2)
