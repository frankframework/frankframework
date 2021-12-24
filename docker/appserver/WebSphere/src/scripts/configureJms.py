cell = AdminControl.getCell()
node = AdminControl.getNode()
server = 'server1'



AdminJMS.createJMSProvider(node, server, 'ActiveMQ', 'org.apache.activemq.jndi.ActiveMQInitialContextFactory', 'tcp://host.docker.internal:5000', 'classpath=/work/drivers/activemq-client.jar')

AdminJMS.createGenericJMSConnectionFactory(node, server, 'ActiveMQ', 'ActiveMQ', 'jms/qcf-activemq', 'jms/qcf-activemq')

p0 = AdminJMS.listJMSProviders()
print 'JMS Provider templates: ', p0
p1 = AdminJMS.listGenericJMSConnectionFactories()
print 'JMS CF templates: ', p1
p2 = AdminJMS.listGenericJMSDestinationTemplates()
print 'JMS Destination templates: ', p2

#https://www.ibm.com/docs/en/was/9.0.5?topic=scripting-jms-configuration-scripts