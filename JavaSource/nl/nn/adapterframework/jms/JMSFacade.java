package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.core.IbisException;
import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueue;
import org.apache.commons.beanutils.PropertyUtils;
import nl.nn.adapterframework.core.INamedObject;

import javax.jms.*;
import javax.naming.NamingException;


/**
 * Provides functions for jms connections, queues and topics and acts as a facade
 * to hide for clients whether a <code>Queue</code> or <code>Topic</code> is used. 
 * <br/>
 * The <code>destinationType</code> field specifies which
 * type should be used.<br/>
 * <p>$Id: JMSFacade.java,v 1.2 2004-02-04 10:02:06 a1909356#db2admin Exp $</p>
 * @author    Gerrit van Brakel
 */
public class JMSFacade extends JNDIBase implements INamedObject{
	public static final String version="$Id: JMSFacade.java,v 1.2 2004-02-04 10:02:06 a1909356#db2admin Exp $";


    private int ackMode = Session.AUTO_ACKNOWLEDGE;
    private boolean persistent;
    private String destinationName;
    private boolean useTopicFunctions = false;

    private String destinationType="QUEUE";

    private Connection connection;
    private Destination destination;


    //<code>forceMQCompliancy</code> is used to perform MQ specific replying.
    //If the MQ destination is not a JMS receiver, format errors occur.
    //To prevent this, settting replyToComplianceType to MQ will inform
    //MQ that the queue (or destination) on which a message is sent, is not JMS compliant.
    
    private String forceMQCompliancy=null;
    
    //---------------------------------------------------------------------
    // Queue fields
    //---------------------------------------------------------------------
    private String queueConnectionFactoryName;
    private QueueConnectionFactory queueConnectionFactory = null;
    //---------------------------------------------------------------------
    // Topic fields
    //---------------------------------------------------------------------
    private String topicConnectionFactoryName;
    private TopicConnectionFactory topicConnectionFactory = null;

    private boolean transacted = false;
    private String subscriberType = "DURABLE"; // DURABLE or TRANSIENT

    private String name;
public void close() throws IbisException {
    try {
        if (connection != null) {
            connection.close();
        }
    } catch (JMSException e) {
        throw new IbisException(e);
    } finally {
	    destination = null;
	    connection = null;
    }
}
/**
 *  Gets the queueSession 
 *
 * @see javax.jms.QueueSession
 * @return                                   The queueSession value
 * @exception  javax.naming.NamingException
 * @exception  javax.jms.JMSException
 */
private QueueSession createQueueSession(QueueConnection connection)
    throws javax.naming.NamingException, JMSException {
    return connection.createQueueSession(transacted, ackMode);
}
/**
 * Returns a session on the connection for a topic or a queue
 */
public Session createSession() throws NamingException, JMSException {
    if (useTopicFunctions)
        return createTopicSession((TopicConnection)getConnection());
    else
        return createQueueSession((QueueConnection)getConnection());
}
public TextMessage createTextMessage(Session session, String correlationID, String message)
   throws javax.naming.NamingException, JMSException {
    TextMessage textMessage = null;
    textMessage = session.createTextMessage();
    if (null != correlationID) {
        textMessage.setJMSCorrelationID(correlationID);
    }
    textMessage.setText(message);
    return textMessage;
}
private TopicSession createTopicSession(TopicConnection connection)
    throws javax.naming.NamingException, JMSException {
    return connection.createTopicSession(transacted, ackMode);
}
	/**
	 * enforces the setting of <code>forceMQCompliancy</code><br/>
	 * this method has to be called prior to creating a <code>QueueSender</code>
	 */
 	private void enforceMQCompliancy(Queue queue) throws JMSException {
	    if (forceMQCompliancy!=null) {
	    	if (forceMQCompliancy.equalsIgnoreCase("MQ")){
			    ((MQQueue)queue).setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ);
			    log.debug("["+name+"] MQ Compliancy for queue ["+queue.toString()+"] set to NONJMS");
	    	} else
	    	if (forceMQCompliancy.equalsIgnoreCase("JMS")) {
			    ((MQQueue)queue).setTargetClient(JMSC.MQJMS_CLIENT_JMS_COMPLIANT);
			    log.debug("MQ Compliancy for queue ["+queue.toString()+"] set to JMS");
	    	}
	    	
	    }
 
    }
    /**
     *  Gets the ackMode 
     *
     * @return    The ackMode value
     */
    public int getAckMode() {
        return ackMode;
    }
    public String getAcknowledgeMode() {
        return this.getAcknowledgeModeAsString(ackMode);
    }
    /**
     *  Gets the acknowledgeModeAsString attribute of the jms.JMSBase class
     *
     * @param  ackMode  Description of the Parameter
     * @return          The acknowledgeModeAsString value
     */
    public String getAcknowledgeModeAsString(int ackMode) {
        Session session = null;

        String ackString;
        if (session.AUTO_ACKNOWLEDGE == ackMode) {
            ackString = "Auto";
        } else
            if (session.CLIENT_ACKNOWLEDGE == ackMode) {
                ackString = "Client";
            } else
                if (session.DUPS_OK_ACKNOWLEDGE == ackMode) {
                    ackString = "Dups";
                } else {
                    ackString = "none";
                }

        return ackString;
    }
/**
 * Returns a connection for a topic or a queue
 */
public Connection getConnection() throws NamingException, JMSException {
	if (connection == null) {
    if (useTopicFunctions)
        connection = getTopicConnectionFactory().createTopicConnection();
    else
        connection = getQueueConnectionFactory().createQueueConnection();
	}
	return connection;
}
public Destination getDestination() throws NamingException, JMSException {

    if (destination == null) {
	    destination = getDestination(getConnection());
    }
    return destination;
}
    /**
     * Utilitiy function to retrieve a Destination from a jndi.
     * @param destinationName
     * @return javax.jms.Destination
     * @throws javax.naming.NamingException
     */
    public Destination getDestination(String destinationName) throws javax.naming.NamingException {
        Destination dest=null;
        dest=(Destination) getContext().lookup(destinationName);
        return dest;
    }
protected Destination getDestination(Connection connection)
    throws NamingException, JMSException {

    if (!useTopicFunctions || persistent) {
        return getDestination(getDestinationName());
    } else {
        return createTopicSession((TopicConnection) connection).createTopic(
            getDestinationName());
    }

}
    /**
     *  Gets the name of the destination, this may be a 
     * <code>queue</code> or <code>topic</code> name. 
     *
     * @return    The destination name value
     */
    public String getDestinationName() {
        return destinationName;
    }
    /**
      *  Retrieves the name of the destination, this may be a
      * <code>queue</code> or <code>topic</code> name.
      */
      public String getDestinationType() {
        return destinationType;
    }
    public String getForceMQCompliancy() {
	    return forceMQCompliancy;
    }
public MessageConsumer getMessageConsumer(Session session, Destination destination, String selector) throws NamingException, JMSException {
    if (useTopicFunctions)
        return getTopicSubscriber((TopicSession)session, (Topic)destination, selector);
    else
        return getQueueReceiver((QueueSession)session, (Queue)destination, selector);
}
    /**
     * gets a sender. if topicName is used the <code>getTopicPublisher()</code>
     * is used, otherwise the <code>getQueueSender()</code>
     */

    public MessageProducer getMessageProducer(Session session, Destination destination)
        throws javax.naming.NamingException, JMSException {
        if (useTopicFunctions)
            return getTopicPublisher((TopicSession)session, (Topic)destination);

        else
            return getQueueSender((QueueSession)session, (Queue)destination);
    }
/**
 * Insert the method's description here.
 * Creation date: (22-05-2003 12:09:18)
 * @return java.lang.String
 */
public java.lang.String getName() {
	return name;
}
    /**
     *  Gets the persistent 
     *
     * @return    The persistent value
     */
    public boolean getPersistent() {
        return persistent;
    }
public String getPhysicalDestinationName(Destination destination) {

    String result = "-";

    if (destination != null) {
        try {
            if (useTopicFunctions)
                result = ((Topic) destination).getTopicName();
            else
                result = ((Queue) destination).getQueueName();
        } catch (javax.jms.JMSException je) {
            log.warn("[" + name + "] got exception on getPhysicalDestinationName", je);
        }
    }
    return result;
}
    /**
     *  Gets the queueConnectionFactory 
     *
     * @return                                   The queueConnectionFactory value
     * @exception  javax.naming.NamingException  Description of the Exception
     */
    private QueueConnectionFactory getQueueConnectionFactory()
        throws javax.naming.NamingException {
        if (null == queueConnectionFactory) {
            log.debug("["+name+"] searching for queueConnectionFactory [" + queueConnectionFactoryName + "]");
            queueConnectionFactory =
                (QueueConnectionFactory) getContext().lookup(queueConnectionFactoryName);
            log.debug("["+name+"] queueConnectionFactory " + queueConnectionFactoryName + " found");
        }
        return queueConnectionFactory;
    }
    /**
     *  Gets the queueConnectionFactoryName 
     *
     * @return    The queueConnectionFactoryName value
     */
    public String getQueueConnectionFactoryName() {
        return queueConnectionFactoryName;
    }
    /**
     *  Gets the queueReceiver 
     * @see javax.jms.QueueReceiver
     * @return                                   The queueReceiver value
     * @exception  javax.naming.NamingException  Description of the Exception
     * @exception  javax.jms.JMSException                  Description of the Exception
     */
private QueueReceiver getQueueReceiver(QueueSession session, Queue destination, String selector)
    throws javax.naming.NamingException, JMSException {
    QueueReceiver queueReceiver = session.createReceiver(destination, selector);
    log.debug("["+name+"] got receiver for queue " + queueReceiver.getQueue().getQueueName()+"]");
    return queueReceiver;
}
/**
  *  Gets the queueSender for a specific queue, not the one in <code>destination</code>
  * @see javax.jms.QueueSender
  * @return                                   The queueReceiver value
  * @exception  javax.naming.NamingException  Description of the Exception
  * @exception  javax.jms.JMSException
  */
private QueueSender getQueueSender(QueueSession session, Queue destination)
    throws javax.naming.NamingException, JMSException {
    QueueSender queueSender;
    enforceMQCompliancy(destination);
    queueSender = session.createSender(destination);

    return queueSender;
}
    /**
     * Only applicable for topics <br/>
     * SubscriberType should <b>DURABLE</b> or <b>TRANSIENT</b>
     */
    public String getSubscriberType() {
        return subscriberType;
    }
    private TopicConnectionFactory getTopicConnectionFactory()
        throws javax.naming.NamingException, JMSException {
        if (null == topicConnectionFactory) {
            topicConnectionFactory =
                (TopicConnectionFactory) getContext().lookup(topicConnectionFactoryName);
        }
        return topicConnectionFactory;
    }
    /**
     *  Gets the topicConnectionFactoryName 
     *
     * @return    The topicConnectionFactoryName value
     */
    public String getTopicConnectionFactoryName() {
        return topicConnectionFactoryName;
    }
/**
 * Gets a topicPublisher for a specified topic
 */
private TopicPublisher getTopicPublisher(TopicSession session, Topic topic)
    throws javax.naming.NamingException, JMSException {
    return session.createPublisher(topic);
}
private TopicSubscriber getTopicSubscriber(
    TopicSession session,
    Topic topic,
    String selector)
    throws javax.naming.NamingException, JMSException {

    TopicSubscriber topicSubscriber;
    if (subscriberType.equalsIgnoreCase("DURABLE")) {
        topicSubscriber =
            session.createDurableSubscriber(topic, destinationName, selector, false);
        log.debug(
            "["
                + name
                + "] got durable subscriber for topic ["
                + destinationName
                + "] with selector ["
                + selector
                + "]");

    } else {
        topicSubscriber = session.createSubscriber(topic, selector, false);
        log.debug(
            "["
                + name
                + "] got transient subscriber for topic ["
                + destinationName
                + "] with selector ["
                + selector
                + "]");
    }

    return topicSubscriber;
}
    /**
     * parameter for creating the <code>TopicSession</code>
     * @see javax.jms.TopicConnection#createTopicSession
     */
    public boolean getTransacted() {
        return transacted;
    }
public void open() throws IbisException {
    try {
        connection = getConnection();
        connection.start();
        destination = getDestination(connection);
    } catch (Exception e) {
        throw new IbisException(e);
    }
}
public void send(MessageProducer messageProducer, Message message)
    throws NamingException, JMSException {

    if (messageProducer instanceof TopicPublisher)
         ((TopicPublisher) messageProducer).publish(message);
    else
         ((QueueSender) messageProducer).send(message);
}
public void send(Session session, Destination dest, Message message)
    throws NamingException, JMSException {

    if (dest instanceof Topic)
        sendByTopic((TopicSession)session, (Topic)dest, message);
    else
        sendByQueue((QueueSession)session, (Queue)dest, message);
}
/**
 * Send a message to a Destination of type Queue.
 * This method respects the <code>replyToComplianceType</code> field,
 * as if it is set to "MQ", 
 */
private void sendByQueue(QueueSession session, Queue destination, Message message)
    throws NamingException, JMSException {
    enforceMQCompliancy(destination);
    QueueSender tqs = session.createSender(destination);
    tqs.send(message);
    tqs.close();
}
private void sendByTopic(TopicSession session, Topic destination, Message message)
    throws NamingException, JMSException {

    TopicPublisher tps = session.createPublisher(destination);
    tps.publish(message);
    tps.close();

}
    /**
     *  Sets the ackMode 
     *
     * @param  value  The new ackMode value
     */
    public void setAckMode(int value) {
        ackMode = value;
    }
    public void setAcknowledgeMode(String acknowledgeMode) {

        if (acknowledgeMode.equals("auto")) {
            ackMode = Session.AUTO_ACKNOWLEDGE;
        } else
            if (acknowledgeMode.equals("dups")) {
                ackMode = Session.DUPS_OK_ACKNOWLEDGE;
            } else
                if (acknowledgeMode.equals("client")) {
                    ackMode = Session.CLIENT_ACKNOWLEDGE;
                } else {
                    // ignore all ack modes, to test no acking
                    log.warn(
                        "["+name+"] invalid acknowledgemode:[" + acknowledgeMode + "] setting no acknowledge");
                    ackMode = -1;
                }

    }
    /**
     *  sets the name of the destination, this may be a 
     * <code>queue</code> or <code>topic</code> name. 
     */

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }
    /**
     * should be <code>QUEUE</code> or <code>TOPIC</code><br/>
     * This function also sets the <code>useTopicFunctions</code> field,
     * that controls wether Topic functions are used or Queue functions.
     */
    public void setDestinationType(String type) {
        this.destinationType = type;
        if (destinationType.equalsIgnoreCase("TOPIC"))
            useTopicFunctions = true;
        else
            useTopicFunctions = false;
    }
    /*<code>forceMQCompliancy</code> is used to perform MQ specific sending.
     *If the MQ destination is not a JMS receiver, format errors occur.
     *To prevent this, settting <code>forceMQCompliancy</code>  to MQ will inform
     *MQ that the replyto queue is not JMS compliant. Setting <code>forceMQCompliancy</code>
     *to "JMS" will cause that on mq the destination is identified as jms-compliant.
     *Other specifics information for different providers may be
     *implemented. Defaults to "JMS".<br/>
     */
    public void setForceMQCompliancy(String ct) {
	    if ((!(ct.equals("MQ")) && (!(ct.equals("JMS")))))
	    	throw new IllegalArgumentException("forceMQCompliancy has a wrong value ["+forceMQCompliancy+"] should be JMS or MQ");
	    forceMQCompliancy=ct;
	    
    }
 	/**
 	 * loads JNDI properties from a JmsRealm
 	 * @see JmsRealm
 	 */ 
	public void setJmsRealm(String jmsRealmName){
	    JmsRealm jmsRealm=JmsRealmFactory.getInstance().getJmsRealm(jmsRealmName);
	    if (null==jmsRealm){
		    log.error("["+name+"] jmsRealm ["+jmsRealmName+"] does not exist");
		    return;
	    }
	    try {
		    PropertyUtils.copyProperties(this, jmsRealm);
	    }catch (Exception e) {
			log.error("["+name+"] unable to copy properties of JmsRealm:"+e.getMessage());
		}
	    log.info("["+name+"] loaded properties from jmsRealm ["+jmsRealm.toString()+"]");
		    
    }
/**
 * Insert the method's description here.
 * Creation date: (22-05-2003 12:09:18)
 * @param newName java.lang.String
 */
public void setName(java.lang.String newName) {
	name = newName;
}
    /**
     *  Sets the persistent 
     *
     * @param  value  The new persistent value
     */
    public void setPersistent(boolean value) {
        persistent = value;
    }
    /**
     *  sets the queueConnectionFactoryName 
     *
    */
    public void setQueueConnectionFactoryName(String name) {
        queueConnectionFactoryName=name;
    }
    /**
     * SubscriberType should <b>DURABLE</b> or <b>TRANSIENT</b>
     */

    public void setSubscriberType(String subscriberType) {
        if ((!subscriberType.equalsIgnoreCase("DURABLE"))
            && (!subscriberType.equalsIgnoreCase("TRANSIENT"))) {
            throw new IllegalArgumentException(
                "invalid subscriberType, should be DURABLE or TRANSIENT. "
                    + this.subscriberType
                    + " is assumed");
        } else
            this.subscriberType = subscriberType;
    }
    /**
     *  sets the topicConnectionFactoryName 
     *
     */
    public void setTopicConnectionFactoryName(String name) {
        topicConnectionFactoryName=name;
    }
    /**
     * Is it under transaction control?
     * @see javax.jms.TopicConnection#createTopicSession
     * @param transacted
     */
    public void setTransacted(boolean transacted) {
        this.transacted = transacted;
    }
    /**
     *  To string
     *
     * @return    Description of the Return Value
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        if (useTopicFunctions) {
            sb.append("[topicName=" + destinationName + "]");
            sb.append("[transacted=" + transacted + "]");
	        sb.append("[topicConnectionFactoryName=" + topicConnectionFactoryName + "]");
        } else {
            sb.append("[queueName=" + destinationName + "]");
	        sb.append("[queueConnectionFactoryName=" + queueConnectionFactoryName + "]");
        }
//        sb.append("[physicalDestinationName="+getPhysicalDestinationName()+"]");
        sb.append("[ackMode=" + getAcknowledgeModeAsString(ackMode) + "]");
        sb.append("[persistent=" + persistent + "]");
        return sb.toString();
    }
}
