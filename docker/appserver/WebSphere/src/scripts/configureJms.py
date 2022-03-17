#Documentation: https://www.ibm.com/docs/en/was/9.0.5?topic=scripting-jms-configuration-scripts
cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'

# ActiveMQ
AdminJMS.createJMSProvider(node, server, 'ActiveMQ', 'org.apache.activemq.jndi.ActiveMQInitialContextFactory', 'tcp://host.docker.internal:61616', 'classpath=/work/drivers/activemq-client.jar;/work/drivers/hawtbuf.jar;/work/drivers/slf4j-api.jar')

AdminJMS.createGenericJMSConnectionFactory(node, server, 'ActiveMQ', 'qcf', 'jms/qcf-activemq', 'XAConnectionFactory', [['connectionPool',[['agedTimeout','100'],['connectionTimeout','1000'],['freePoolDistributionTableSize',10],['maxConnections','12'],['minConnections','5'],['numberOfFreePoolPartitions','3'],['numberOfSharedPoolPartitions','6'],['numberOfUnsharedPoolPartitions','3'],['purgePolicy','EntirePool'],['reapTime','10000'],['surgeCreationInterval','10'],['surgeThreshold','10'],['testConnection','true'],['testConnectionInterval','10'],['unusedTimeout','10000']]]])

AdminJMS.createGenericJMSDestination(node, server, 'ActiveMQ', 'i4testiaf_in-activemq', 'jms/i4testiaf_in-activemq', 'dynamicQueues/Q.TEST.IN')
AdminJMS.createGenericJMSDestination(node, server, 'ActiveMQ', 'i4testiaf_out-activemq', 'jms/i4testiaf_out-activemq', 'dynamicQueues/Q.TEST.OUT')
AdminJMS.createGenericJMSDestination(node, server, 'ActiveMQ', 'i4testiaf_ff-activemq', 'jms/i4testiaf_ff-activemq', 'dynamicQueues/Q.TEST.FF')

# Artemis
AdminJMS.createJMSProvider(node, server, 'Artemis', 'org.apache.activemq.artemis.jndi.ActiveMQInitialContextFactory', 'tcp://host.docker.internal:61615?type=XA_CF', 'classpath=/work/drivers/artemis-jms-client-all.jar')

AdminJMS.createGenericJMSConnectionFactory(node, server, 'Artemis', 'qcf', 'jms/qcf-activemq-artemis', 'XAConnectionFactory', [['connectionPool',[['agedTimeout','100'],['connectionTimeout','1000'],['freePoolDistributionTableSize',10],['maxConnections','12'],['minConnections','5'],['numberOfFreePoolPartitions','3'],['numberOfSharedPoolPartitions','6'],['numberOfUnsharedPoolPartitions','3'],['purgePolicy','EntirePool'],['reapTime','10000'],['surgeCreationInterval','10'],['surgeThreshold','10'],['testConnection','true'],['testConnectionInterval','10'],['unusedTimeout','10000']]]])

AdminJMS.createGenericJMSDestination(node, server, 'Artemis', 'i4testiaf_in-activemq-artemis', 'jms/i4testiaf_in-activemq-artemis', 'dynamicQueues/Q.TEST.IN')
AdminJMS.createGenericJMSDestination(node, server, 'Artemis', 'i4testiaf_out-activemq-artemis', 'jms/i4testiaf_out-activemq-artemis', 'dynamicQueues/Q.TEST.OUT')
AdminJMS.createGenericJMSDestination(node, server, 'Artemis', 'i4testiaf_ff-activemq-artemis', 'jms/i4testiaf_ff-activemq-artemis', 'dynamicQueues/Q.TEST.FF')


#p0 = AdminJMS.listJMSProviders()
#print 'JMS Provider templates: ', p0
#p1 = AdminJMS.listGenericJMSConnectionFactories()
#print 'JMS CF: ', p1
#p2 = AdminJMS.listGenericJMSDestinations()
#print 'JMS Destination: ', p2