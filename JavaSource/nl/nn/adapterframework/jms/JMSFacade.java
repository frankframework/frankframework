/*
 * $Log: JMSFacade.java,v $
 * Revision 1.5  2004-03-24 08:24:46  L190409
 * enabled XA transactions
 * renamed original 'transacted' into 'jmsTransacted'
 *
 */
package nl.nn.adapterframework.jms;

import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.IbisException;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueue;
import nl.nn.adapterframework.core.INamedObject;

import javax.jms.*;
import javax.naming.NamingException;


/**
 * Provides functions for jms connections, queues and topics and acts as a facade
 * to hide for clients whether a <code>Queue</code> or <code>Topic</code> is used. 
 * <br/>
 * The <code>destinationType</code> field specifies which
 * type should be used.<br/>
 * <p>$Id: JMSFacade.java,v 1.5 2004-03-24 08:24:46 L190409 Exp $</p>
 * @author    Gerrit van Brakel
 */
public class JMSFacade extends JNDIBase implements INamedObject, HasPhysicalDestination, IXAEnabled {
	public static final String version="$Id: JMSFacade.java,v 1.5 2004-03-24 08:24:46 L190409 Exp $";

	private String name;

	private boolean transacted = false;
	private boolean jmsTransacted = false;
	private String subscriberType = "DURABLE"; // DURABLE or TRANSIENT

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
	private String queueConnectionFactoryNameXA;
    private QueueConnectionFactory queueConnectionFactory = null;
    //---------------------------------------------------------------------
    // Topic fields
    //---------------------------------------------------------------------
    private String topicConnectionFactoryName;
	private String topicConnectionFactoryNameXA;
    private TopicConnectionFactory topicConnectionFactory = null;

    
    
	/**
	 *  Gets the queueConnectionFactory 
	 *
	 * @return                                   The queueConnectionFactory value
	 * @exception  javax.naming.NamingException  Description of the Exception
	 */
	private QueueConnectionFactory getQueueConnectionFactory()
		throws NamingException {
		if (null == queueConnectionFactory) {
			String qcfName = isTransacted() ? getQueueConnectionFactoryNameXA() : getQueueConnectionFactoryName();
			log.debug("["+name+"] searching for queueConnectionFactory [" + qcfName + "]");
			queueConnectionFactory =
				(QueueConnectionFactory) getContext().lookup(qcfName);
			log.info("["+name+"] queueConnectionFactory [" + qcfName + "] found: [" + queueConnectionFactory + "]");
		}
		return queueConnectionFactory;
	}
	private TopicConnectionFactory getTopicConnectionFactory()
		throws NamingException, JMSException {
		if (null == topicConnectionFactory) {
			String tcfName = isTransacted() ? getTopicConnectionFactoryNameXA() : getTopicConnectionFactoryName();
			log.debug("["+name+"] searching for topicConnectionFactory [" + tcfName + "]");
			topicConnectionFactory =
				(TopicConnectionFactory) getContext().lookup(tcfName);
			log.info("["+name+"] topicConnectionFactory [" + tcfName + "] found: [" + topicConnectionFactory + "]");
		}
		return topicConnectionFactory;
	}

	/**
	 * Returns a connection for a topic or a queue
	 */
	protected Connection getConnection() throws NamingException, JMSException {
		if (connection == null) {
		log.debug("["+getName()+"] creating connection, useTopicFunctions=["+useTopicFunctions+"], isTransacted=["+isTransacted()+"]");
		if (useTopicFunctions)
			connection = getTopicConnectionFactory().createTopicConnection();
		else
			connection = getQueueConnectionFactory().createQueueConnection();
		}
		connection.start();
		return connection;
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
		throws NamingException, JMSException {
		return connection.createQueueSession(isJmsTransacted(), getAckMode());
	}
	private TopicSession createTopicSession(TopicConnection connection)
		throws NamingException, JMSException {
		return connection.createTopicSession(isJmsTransacted(), getAckMode());
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

/*
 * Returns a session on the connection for a topic or a queue, and enlists the XA-resources to the transaction 
 * 
 */
/*
public Session createSession(Transaction tx) throws NamingException, JMSException, SystemException, RollbackException {
	XASession xas;
	Session result;
//	log.debug("["+getName()+"] creating XASession");
	log.debug("["+getName()+"] creating XASession in transaction, status before="+JtaUtil.displayTransactionStatus(tx));
	if (!isTransacted()) {
		log.warn("["+getName()+"] has attribute transacted=\"false\", will not take part in transaction ["+tx+"]");
		return createSession();
	}
    if (useTopicFunctions) {
		XATopicSession xats;
		xats = ((XATopicConnection)getConnection(true)).createXATopicSession();
		xas = xats;
		result = xats.getTopicSession();
	}
    else{
		XAQueueSession xaqs;
		xaqs = ((XAQueueConnection)getConnection(true)).createXAQueueSession();
		xas = xaqs;
		result = xaqs.getQueueSession();
    }
	log.debug("["+getName()+"] enlisting XAResource of XASession ["+xas+"]");
	log.debug("enlisting XAResource of XASession ["+xas+"] to transaction, status before="+JtaUtil.displayTransactionStatus(tx));
	tx.enlistResource(xas.getXAResource());
	log.debug("enlisted XAResource of XASession ["+xas+"] to transaction, status before="+JtaUtil.displayTransactionStatus(tx));
	log.debug("["+getName()+"] registering ResourceCloser for XASession ["+xas+"]");
	tx.registerSynchronization(new ResourceCloser(this, xas));
	return result;
}
*/

public void open() throws IbisException {
	try {
		connection = getConnection();
		destination = getDestination();
	} catch (Exception e) {
		throw new IbisException(e);
	}
}
   
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

public Destination getDestination() throws NamingException, JMSException {

    if (destination == null) {
	    if (!useTopicFunctions || persistent) {
	        destination = getDestination(getDestinationName());
	    } else {
	        destination = createTopicSession((TopicConnection) getConnection()).createTopic(
	            getDestinationName());
	    }
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

public MessageConsumer getMessageConsumerForCorrelationId(Session session, Destination destination, String correlationId) throws NamingException, JMSException {
	if (correlationId==null)
		return getMessageConsumer(session, destination, null);
	else
		return getMessageConsumer(session, destination, "JMSCorrelationID='" + correlationId + "'");
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
        throws NamingException, JMSException {
        if (useTopicFunctions)
            return getTopicPublisher((TopicSession)session, (Topic)destination);

        else
            return getQueueSender((QueueSession)session, (Queue)destination);
    }
    
public String getName() {
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
public String getPhysicalDestinationName() {

    String result = null;

	try {
	    if (getDestination() != null) {
            if (useTopicFunctions)
                result = ((Topic) destination).getTopicName();
            else
                result = ((Queue) destination).getQueueName();
	    }
    } catch (Exception je) {
        log.warn("[" + name + "] got exception in getPhysicalDestinationName", je);
    }
    return getDestinationType()+"("+getDestinationName()+") ["+result+"]";
}
    /**
     *  Gets the queueReceiver 
     * @see javax.jms.QueueReceiver
     * @return                                   The queueReceiver value
     * @exception  javax.naming.NamingException  Description of the Exception
     * @exception  javax.jms.JMSException                  Description of the Exception
     */
private QueueReceiver getQueueReceiver(QueueSession session, Queue destination, String selector)
    throws NamingException, JMSException {
    QueueReceiver queueReceiver = session.createReceiver(destination, selector);
//    log.debug("["+name+"] got receiver for queue " + queueReceiver.getQueue().getQueueName()+"]");
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
    throws NamingException, JMSException {
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

/**
 * Gets a topicPublisher for a specified topic
 */
private TopicPublisher getTopicPublisher(TopicSession session, Topic topic)
    throws NamingException, JMSException {
    return session.createPublisher(topic);
}
private TopicSubscriber getTopicSubscriber(
    TopicSession session,
    Topic topic,
    String selector)
    throws NamingException, JMSException {

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
    public boolean isTransacted() {
        return transacted;
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
	        sb.append("[topicConnectionFactoryName=" + topicConnectionFactoryName + "]");
	        sb.append("[topicConnectionFactoryNameXA=" + topicConnectionFactoryNameXA + "]");
        } else {
            sb.append("[queueName=" + destinationName + "]");
	        sb.append("[queueConnectionFactoryName=" + queueConnectionFactoryName + "]");
	        sb.append("[queueConnectionFactoryNameXA=" + queueConnectionFactoryNameXA + "]");
        }
//        sb.append("[physicalDestinationName="+getPhysicalDestinationName()+"]");
        sb.append("[ackMode=" + getAcknowledgeModeAsString(ackMode) + "]");
        sb.append("[persistent=" + persistent + "]");
        sb.append("[transacted=" + transacted + "]");
        return sb.toString();
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
	 * Returns the queueConnectionFactoryNameXA.
	 * @return String
	 */
	public String getQueueConnectionFactoryNameXA() {
		return queueConnectionFactoryNameXA;
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
	 * Returns the topicConnectionFactoryNameXA.
	 * @return String
	 */
	public String getTopicConnectionFactoryNameXA() {
		return topicConnectionFactoryNameXA;
	}

	/**
	 * Sets the queueConnectionFactoryNameXA.
	 * @param queueConnectionFactoryNameXA The queueConnectionFactoryNameXA to set
	 */
	public void setQueueConnectionFactoryNameXA(String queueConnectionFactoryNameXA) {
		this.queueConnectionFactoryNameXA = queueConnectionFactoryNameXA;
	}

	/**
	 * Sets the topicConnectionFactoryNameXA.
	 * @param topicConnectionFactoryNameXA The topicConnectionFactoryNameXA to set
	 */
	public void setTopicConnectionFactoryNameXA(String topicConnectionFactoryNameXA) {
		this.topicConnectionFactoryNameXA = topicConnectionFactoryNameXA;
	}

	public boolean isJmsTransacted() {
		return jmsTransacted;
	}

	public void setJmsTransacted(boolean jmsTransacted) {
		this.jmsTransacted = jmsTransacted;
	}

}
