/*
 * $Log: JMSBase.java,v $
 * Revision 1.5  2004-03-23 18:24:38  L190409
 * delegated copying of JmsRealm properties to method of JmsRealm
 *
 */
package nl.nn.adapterframework.jms;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueue;

import javax.jms.*;
import javax.naming.NamingException;


/**
 * Provides functions for jms connections, queues and topics and acts as a facade
 * to hide for clients whether a <code>Queue</code> or <code>Topic</code> is used. 
 * <br/>
 * The <code>destinationType</code> field specifies which
 * type should be used.<br/>
 *
 * <p>$Id: JMSBase.java,v 1.5 2004-03-23 18:24:38 L190409 Exp $</p>
 * 
 * @deprecated This class remembers too much: It stores jms-receivers and jms-senders
 *             as object-members. Please use {@link JMSFacade} instead.
 *
 * @author     Johan Verrips
 */
public class JMSBase extends JNDIBase {
	public static final String version="$Id: JMSBase.java,v 1.5 2004-03-23 18:24:38 L190409 Exp $";


    private int ackMode;
    private boolean persistent;
    private String destinationName;
    private boolean useTopicFunctions = false;

    private String destinationType="QUEUE";


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
    private QueueConnection queueConnection = null;
    private QueueSession queueReceiverSession = null;
    private QueueSession queueSenderSession = null;
    private Queue queue = null;
    private QueueReceiver queueReceiver = null;
    private QueueSender queueSender = null;
    

    private String selector = null;

    //---------------------------------------------------------------------
    // Topic fields
    //---------------------------------------------------------------------
    private String topicConnectionFactoryName;
    private TopicConnectionFactory topicConnectionFactory = null;
    private TopicConnection topicConnection = null;
    private TopicSession topicSubscriberSession = null;
    private TopicSession topicPublisherSession = null;
    private boolean transacted = false;
    private String subscriberType = "DURABLE"; // DURABLE or TRANSIENT
    private Topic topic = null;
    private TopicSubscriber topicSubscriber = null;
    private TopicPublisher topicPublisher = null;

    private String name;

    /**
     *Constructor for the jms.JMSBase object
     */
    public JMSBase() {
        super();
        log.warn("Deprecated version of JMSBase. Use JMSFacade instead");
    }
    public void closeConnection()
        throws javax.naming.NamingException, JMSException {
            closeTopicConnection();
            closeQueueConnection();
    }
    /**
     *  Description of the Method
     * @see javax.jms.QueueConnection
     * @exception  javax.naming.NamingException
     * @exception  javax.jms.JMSException
     */
    private void closeQueueConnection()
        throws javax.naming.NamingException, JMSException {
        closeQueueSenderSession();
        closeQueueReceiverSession();
        if (queueConnection!=null){
            queueConnection.close();
            queueConnection = null;
            log.debug("["+name+"] closed QueueConnection");
        }
    }
    private void closeQueueReceiver()
        throws javax.naming.NamingException, JMSException {
        if (null!=queueReceiver){
            log.debug("["+name+"] closed QueueReceiver to queue ["+queueReceiver.getQueue().getQueueName()+"]");
            queueReceiver.close();
            queueReceiver = null;
        }
    }
    private void closeQueueReceiverSession()
        throws javax.naming.NamingException, JMSException {
        if (null != queueReceiver)
            closeQueueReceiver();
        if (null!=queueReceiverSession){
            queueReceiverSession.close();
            queueReceiverSession = null;
            log.debug("["+name+"] closed QueueReceiverSession");
        }
    }
    private void closeQueueSender()
        throws javax.naming.NamingException, JMSException {
        if (null !=queueSender){
            log.debug("["+name+"] closed QueueSender to queue ["+queueSender.getQueue().getQueueName()+"]");
            queueSender = null;
        }
    }
    private void closeQueueSenderSession()
        throws javax.naming.NamingException, JMSException {
        if (null != queueSender)
            closeQueueSender();
        if (null!=queueSenderSession) {
            queueSenderSession.close();
            queueSenderSession = null;
            log.debug("["+name+"] closed QueueSession");
        }
    }
    public void closeReceiver() throws javax.naming.NamingException, JMSException {
            closeTopicSubscriber();
            closeQueueReceiver();
    }
    public void closeSender() throws javax.naming.NamingException, JMSException {
            closeTopicPublisher();
            closeQueueSender();
    }
    public void closeSession() throws javax.naming.NamingException, JMSException {
            closeTopicSubscriberSession();
            closeTopicPublisherSession();
            closeQueueReceiverSession();
            closeQueueSenderSession();
    }
    private void closeTopicConnection()
        throws javax.naming.NamingException, JMSException {
        closeTopicPublisherSession();
        closeTopicSubscriberSession();
        if (null != topicConnection) {
       	    topicConnection.close();

            topicConnection = null;
            log.debug("["+name+"] closed connection to topic");
        }


    }
    private void closeTopicPublisher()
        throws javax.naming.NamingException, JMSException {

        if (null != topicPublisher) {
            log.debug("["+name+"] closed publisher for topic [" + topicPublisher.getTopic().getTopicName() + "]");
            topicPublisher.close();
            topicPublisher = null;
        }

    }
    private void closeTopicPublisherSession()
        throws javax.naming.NamingException, JMSException {
        if (null != topicPublisher)
            closeTopicPublisher();
        if (null != topicPublisherSession) {
            topicPublisherSession.close();
            topicPublisherSession = null;
	        log.debug("["+name+"] closed topicPublisher session for topic ");
        }

    }
    private void closeTopicSubscriber()
        throws javax.naming.NamingException, JMSException {
        if (null != topicSubscriber) {
            log.debug("["+name+"] closed subscriber for topic [" + topicSubscriber.getTopic().getTopicName() + "]");
            topicPublisher.close();
            topicSubscriber = null;
        }
    }
    private void closeTopicSubscriberSession()
        throws javax.naming.NamingException, JMSException {
        if (null != topicSubscriber)
            closeTopicSubscriber();
        if (null != topicSubscriberSession) {
            topicSubscriberSession.close();
            log.debug("["+name+"] closed topicSubscriber session");
            topicSubscriberSession = null;
        }

    }
    public TextMessage createTextMessage(String correlationID, String message)
        throws javax.naming.NamingException, JMSException {
        TextMessage textMessage = null;
        if (useTopicFunctions)
            textMessage = getTopicPublisherSession().createTextMessage();
        else
            textMessage = getQueueSenderSession().createTextMessage();
        if (null != correlationID)
            textMessage.setJMSCorrelationID(correlationID);
        textMessage.setText(message);
        return textMessage;
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
        String ackString;
        if (Session.AUTO_ACKNOWLEDGE == ackMode) {
            ackString = "Auto";
        } else
            if (Session.CLIENT_ACKNOWLEDGE == ackMode) {
                ackString = "Client";
            } else
                if (Session.DUPS_OK_ACKNOWLEDGE == ackMode) {
                    ackString = "Dups";
                } else {
                    ackString = "none";
                }

        return ackString;
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
    /**
    * gets a receiver. if topicName is used the <code>getTopicSubscriber()</code>
    * is used, otherwise the <code>getQueueReceiver()</code>
    */

    public MessageConsumer getMessageReceiver()
        throws javax.naming.NamingException, JMSException {
        if (useTopicFunctions)
            return getTopicSubscriber();
        else
            return getQueueReceiver();
    }
    /**
     * gets a sender. if topicName is used the <code>getTopicPublisher()</code>
     * is used, otherwise the <code>getQueueSender()</code>
     */

    public MessageProducer getMessageSender()
        throws javax.naming.NamingException, JMSException {
        if (useTopicFunctions)
            return getTopicPublisher();

        else
            return getQueueSender();
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
    public String getPhysicalDestinationName(){
	    String result="-";
	    try {
		    if (topic!=null) 
			    	result= topic.getTopicName();
			else
				if (queue!=null) result=queue.getQueueName();
	    } catch (javax.jms.JMSException je) {
		    log.warn("["+name+"] got exception on getPhysicalDestinationName", je);
		}
		return result;	
    }
    /**
     *  Gets the queue 
     * @see javax.jms.Queue
     * @return                                   The queue value
     * @exception  javax.naming.NamingException  Exception with JNDI
     * @exception  javax.jms.JMSException        Exception with JMS
     */
    private Queue getQueue() throws javax.naming.NamingException, JMSException {
        if (null == queue) {
            queue = (Queue) getContext().lookup(destinationName);
            log.debug("["+name+"] got Queue for queue [" + queue.getQueueName()+ "] destination ["+destinationName+"]");
        }
        return queue;
    }
    /**
     *  Gets the queueConnection 
     * @see javax.jms.QueueConnection
     * @return                                   The queueConnection value
     * @exception  javax.naming.NamingException
     * @exception  javax.jms.JMSException
     */
    private QueueConnection getQueueConnection()
        throws javax.naming.NamingException, JMSException {
        if (null == queueConnection) {
            queueConnection = getQueueConnectionFactory().createQueueConnection();
            log.debug("["+name+"] got QueueConnection");
        } 
        return queueConnection;
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
    private QueueReceiver getQueueReceiver()
        throws javax.naming.NamingException, JMSException {
        if (null == queueReceiver) {
            queueReceiver = getQueueReceiverSession().createReceiver(getQueue(), selector);
            log.debug("["+name+"] got receiver for queue " + queueReceiver.getQueue().getQueueName()+"]");
        }
        return queueReceiver;
    }
    /**
     *  Gets the queueSession 
     *
     * @see javax.jms.QueueSession
     * @return                                   The queueSession value
     * @exception  javax.naming.NamingException
     * @exception  javax.jms.JMSException
     */
    private QueueSession getQueueReceiverSession()
        throws javax.naming.NamingException, JMSException {
        if (null == queueReceiverSession) {
            queueReceiverSession = getQueueConnection().createQueueSession(false, ackMode);
            log.debug(
                "["+name+"] got queueReceiverSession");
        }
        return queueReceiverSession;
    }
    /**
     *  Gets the queueSender
     * @see javax.jms.QueueSender
     * @return                                   The queueReceiver value
     * @exception  javax.naming.NamingException  Description of the Exception
     * @exception  javax.jms.JMSException
     */
    private QueueSender getQueueSender()
        throws javax.naming.NamingException, JMSException {
        if (null == queueSender) {
	        enforceMQCompliancy(getQueue());
            queueSender = getQueueSenderSession().createSender(getQueue());
            log.debug("["+name+"] got sender for queue [" + queueSender.getQueue().getQueueName()+"]");
        }
        return queueSender;
    }
    /**
     *  Gets the queueSession
     *
     * @see javax.jms.QueueSession
     * @return                                   The queueSession value
     * @exception  javax.naming.NamingException
     * @exception  javax.jms.JMSException
     */
    private QueueSession getQueueSenderSession()
        throws javax.naming.NamingException, JMSException {
        if (null == queueSenderSession) {
            queueSenderSession = getQueueConnection().createQueueSession(false, ackMode);
            log.debug(
                "["+name+"] got queueSenderSession");
        }
        return queueSenderSession;
    }
    public MessageConsumer getReceiver() throws NamingException, JMSException {
	    if (useTopicFunctions) return getTopicSubscriber();
	    	else return getQueueReceiver();
    }
    /**
     * gets the JMS Message Selector.
     */
    public String getSelector() {
        return selector;
    }
    /**
     * Only applicable for topics <br/>
     * SubscriberType should <b>DURABLE</b> or <b>TRANSIENT</b>
     * @return
     */
    public String getSubscriberType() {
        return subscriberType;
    }
    private Topic getTopic() throws javax.naming.NamingException, JMSException {
        if (topic == null) {
            if (persistent) {
                topic = (Topic) getContext().lookup(destinationName);
            } else {
                topic = getTopicSubscriberSession().createTopic(destinationName);
            }
            log.debug("["+name+"] got topic ["+topic.getTopicName()+"] destination ["+destinationName+"]");
        }
        return topic;
    }
    private TopicConnection getTopicConnection()
        throws javax.naming.NamingException, JMSException {
        if (null == topicConnection) {
            topicConnection = getTopicConnectionFactory().createTopicConnection();
            log.debug("["+name+"] got topic connection");
        }
        return topicConnection;
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
    private TopicPublisher getTopicPublisher()
        throws javax.naming.NamingException, JMSException {
        if (topicPublisher == null) {
            topicPublisher = getTopicPublisherSession().createPublisher(getTopic());
            log.debug("["+name+"] got topic publisher for topic [" + topicPublisher.getTopic().getTopicName() + "]");

        }
        return topicPublisher;
    }
    private TopicSession getTopicPublisherSession()
        throws javax.naming.NamingException, JMSException {
        if (null == topicPublisherSession) {
            topicPublisherSession = getTopicConnection().createTopicSession(transacted, ackMode);
            log.debug( "["+name+"] got topicPublisher session");
        }
        return topicPublisherSession;
    }
    private TopicSubscriber getTopicSubscriber()
        throws javax.naming.NamingException, JMSException {

        if (topicSubscriber == null) {
            if (subscriberType.equalsIgnoreCase("DURABLE")) {
                    topicSubscriber=getTopicSubscriberSession().createDurableSubscriber(
                        getTopic(),
                        destinationName,
                        selector,
                        false);
                log.debug(
                    "["+name+"] got durable subscriber for topic ["
                        + destinationName
                        + "] with selector ["
                        + selector
                        + "]");

            } else {
                topicSubscriber =
                    getTopicSubscriberSession().createSubscriber(getTopic(), selector, false);
                log.debug(
                    "["+name+"] got transient subscriber for topic ["
                        + destinationName
                        + "] with selector ["
                        + selector
                        + "]");
            }
        }
        return topicSubscriber;
    }
    private TopicSession getTopicSubscriberSession()
        throws javax.naming.NamingException, JMSException {
        if (null == topicSubscriberSession) {
            topicSubscriberSession = getTopicConnection().createTopicSession(transacted, ackMode);
            log.debug(
                "["+name+"] got topicSubscriber session");
        }
        return topicSubscriberSession;
    }
    /**
     * parameter for creating the <code>TopicSession</code>
     * @see javax.jms.TopicConnection#createTopicSession
     */
    public boolean getTransacted() {
        return transacted;
    }
    public Message receive(long timeout) throws NamingException, JMSException {
        if (useTopicFunctions)
            return receiveByTopic(timeout);
        else
            return receiveByQueue(timeout);
    }
    private Message receiveByQueue(long timeout)
        throws NamingException, JMSException {
        return getQueueReceiver().receive(timeout);
    }
    private Message receiveByTopic(long timeout)
        throws NamingException, JMSException {
        return getTopicSubscriber().receive(timeout);
    }
	/**
	 * Resets dynamic objects of this class.<br/>
	 * It sets the values of the (possibly) active <ul>
	 * <li><code>queuSender</code></li>
	 * <li><code>queuReceiver</code></li>
	 * <li><code>queuSenderSession</code></li>
	 * <li><code>queuReceiverSession</code></li>
	 * <li><code>queueConnection</code></li>
	 * <li><code>topicSubscriber</code></li>
	 * <li><code>topicPublisher</code></li>
	 * <li><code>topicSubscriberSession</code></li>
	 * <li><code>topicPublisherSession</code></li>
	 * <li><code>topicConnection</code></li>
	 * </ul>
	 * to <code>null</code>, so that they will be rebuilt
	 * on the next call to a <code>get...</code> function.
	 */
    protected void reset() {
	    queueReceiver=null;
	    queueSender=null;
	    queueReceiverSession=null;
	    queueSenderSession=null;
	    queue=null;
	    queueConnection=null;
	    topicSubscriberSession=null;
	    topicPublisherSession=null;
	    topicConnection=null;
	    log.debug("Dynamic objects in ["+name+"] have been reset");
    }
    public Message send(Destination dest, Message message)
        throws NamingException, JMSException {

        if (dest instanceof Topic)
            message=sendByTopic(dest, message);
        else
            message=sendByQueue(dest, message);
        return message;
    }
    public Message send(Message message)
        throws NamingException, JMSException {
        if (useTopicFunctions)
            message=sendByTopic(message);
        else
            message=sendByQueue(message);
        return message;
    }
	/**
	 * Send a message to a Destination of type Queue.
	 * This method respects the <code>replyToComplianceType</code> field,
	 * as if it is set to "MQ", 
	 */
	private Message sendByQueue(
        Destination dest,
        Message message)
        throws NamingException, JMSException {
	    enforceMQCompliancy((Queue)dest);
        QueueSession tqsn=getQueueConnection().createQueueSession(false, ackMode);
        QueueSender tqs= tqsn.createSender((Queue) dest);
        tqs.send(message);
        tqs.close();
        tqsn.close();
        return message;
        
    }
    private Message sendByQueue(Message message)
        throws NamingException, JMSException {
        getQueueSender().send(message);
        return message;
    }
    private Message sendByTopic(
        Destination dest,
        Message message)
        throws NamingException, JMSException {
        
        TopicSession tpsn=getTopicConnection().createTopicSession(false, ackMode);
        TopicPublisher tps= tpsn.createPublisher((Topic) dest);
        tps.publish(message);
        tps.close();
        tpsn.close();
        return message;
   
    }
    private Message sendByTopic(Message message)
        throws NamingException, JMSException {
        getTopicPublisher().publish(message);
        return message;
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
		JmsRealm.copyRealm(this,jmsRealmName);
	}
public void setName(java.lang.String newName) {
	name = newName;
}
    public void setPersistent(boolean value) {
        persistent = value;
    }
    public void setQueueConnectionFactoryName(String name) {
        queueConnectionFactoryName=name;
    }
    /**
     * The selector is a criteria for getting topics, e.g. <code>JMSPriority=1</code>
     * selects only prio 1 messages.
     * @param selector
     */
    public void setSelector(String selector) {
        this.selector = selector;
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
     * Starts a connection for a topic or a queue
     */
    public void startConnection()
        throws javax.naming.NamingException, JMSException {
        if (useTopicFunctions)
            startTopicConnection();
        else
            startQueueConnection();
    }
    /**
     *  Starts a QueueConnection
     * @see javax.jms.QueueConnection
     * @exception  javax.naming.NamingException
     * @exception  javax.jms.JMSException
     */
    private void startQueueConnection()
        throws javax.naming.NamingException, JMSException {
        getQueueConnection().start();
        log.debug("["+name+"] queue connection started for queue " + destinationName);
    }
    /**
    *  Starts a TopicConnection
    * @see javax.jms.QueueConnection
    * @exception  javax.naming.NamingException
    * @exception  javax.jms.JMSException
    */
    private void startTopicConnection()
        throws javax.naming.NamingException, JMSException {
        getTopicConnection().start();
        log.debug("["+name+"] topic connection started for topic " + destinationName);
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
        sb.append("[physicalDestinationName="+getPhysicalDestinationName()+"]");
		sb.append("[selector="+getSelector()+"]");
        sb.append("[ackMode=" + getAcknowledgeModeAsString(ackMode) + "]");
        sb.append("[persistent=" + persistent + "]");
        return sb.toString();
    }
}
