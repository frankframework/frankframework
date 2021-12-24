cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'



AdminJMS.createJMSProvider(node, server, 'ActiveMQ', 'org.apache.activemq.jndi.ActiveMQInitialContextFactory', 'tcp://host.docker.internal:5000', 'classpath=/work/drivers/activemq-client.jar;/work/drivers/hawtbuf.jar;/work/drivers/slf4j-api.jar')

AdminJMS.createGenericJMSConnectionFactory(node, server, 'ActiveMQ', 'qcf', 'jms/qcf-activemq', 'jms/qcf-activemq', [['connectionPool',[['agedTimeout','100'],['connectionTimeout','1000'],['freePoolDistributionTableSize',10],['maxConnections','12'],['minConnections','5'],['numberOfFreePoolPartitions','3'],['numberOfSharedPoolPartitions','6'],['numberOfUnsharedPoolPartitions','3'],['purgePolicy','EntirePool'],['reapTime','10000'],['surgeCreationInterval','10'],['surgeThreshold','10'],['testConnection','true'],['testConnectionInterval','10'],['unusedTimeout','10000']]]])

p0 = AdminJMS.listJMSProviders()
print 'JMS Provider templates: ', p0
p1 = AdminJMS.listGenericJMSConnectionFactories()
print 'JMS CF templates: ', p1
p2 = AdminJMS.listGenericJMSDestinationTemplates()
print 'JMS Destination templates: ', p2

#https://www.ibm.com/docs/en/was/9.0.5?topic=scripting-jms-configuration-scripts