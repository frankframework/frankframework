cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'



AdminJMS.createJMSProvider(node, server, 'ActiveMQ', 'org.apache.activemq.jndi.ActiveMQInitialContextFactory', 'tcp://host.docker.internal:5000', 'classpath=/work/drivers/activemq-client.jar;/work/drivers/hawtbuf.jar;/work/drivers/slf4j-api.jar')

AdminJMS.createGenericJMSConnectionFactory(node, server, 'ActiveMQ', 'qcf', 'jms/qcf-activemq', 'QueueConnectionFactory', [['connectionPool',[['agedTimeout','100'],['connectionTimeout','1000'],['freePoolDistributionTableSize',10],['maxConnections','12'],['minConnections','5'],['numberOfFreePoolPartitions','3'],['numberOfSharedPoolPartitions','6'],['numberOfUnsharedPoolPartitions','3'],['purgePolicy','EntirePool'],['reapTime','10000'],['surgeCreationInterval','10'],['surgeThreshold','10'],['testConnection','true'],['testConnectionInterval','10'],['unusedTimeout','10000']]]])

AdminJMS.createGenericJMSDestination(node, server, 'ActiveMQ', 'i4testiaf_in', 'jms/i4testiaf_in', 'dynamicQueues/Q.TEST.IN')
AdminJMS.createGenericJMSDestination(node, server, 'ActiveMQ', 'i4testiaf_out', 'jms/i4testiaf_out', 'dynamicQueues/Q.TEST.OUT')
AdminJMS.createGenericJMSDestination(node, server, 'ActiveMQ', 'i4testiaf_ff', 'jms/i4testiaf_ff', 'dynamicQueues/Q.TEST.FF')

p0 = AdminJMS.listJMSProviders()
print 'JMS Provider templates: ', p0
p1 = AdminJMS.listGenericJMSConnectionFactories()
print 'JMS CF: ', p1
p2 = AdminJMS.listGenericJMSDestinations()
print 'JMS Destination: ', p2

#https://www.ibm.com/docs/en/was/9.0.5?topic=scripting-jms-configuration-scripts