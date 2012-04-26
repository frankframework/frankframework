/*
 * $Log: JMSFacade.java,v $
 * Revision 1.47  2012-04-26 10:57:02  m00f069
 * Added "milliseconds" to javadoc/ibis description MessageTimeToLive
 *
 * Revision 1.46  2012/04/02 08:42:20  Jaco de Groot <jaco.de.groot@ibissource.org>
 * Added correlationIdToHex
 *
 * Revision 1.45  2011/11/30 13:51:51  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:48  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.43  2011/10/04 09:55:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use jndiContextPrefix
 *
 * Revision 1.42  2011/03/17 08:12:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * separate method extractMessageBody()
 *
 * Revision 1.41  2010/02/02 14:34:49  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * separate method for getting connectionfactory info
 *
 * Revision 1.40  2010/01/28 14:58:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'Connection' classes to 'MessageSource'
 *
 * Revision 1.39  2009/09/17 08:21:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed timeout handling of JmsSender
 *
 * Revision 1.38  2009/09/09 14:34:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed forced target client setting in secondary send() method
 *
 * Revision 1.37  2009/08/20 12:12:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added generic getStringFromRawMessage
 *
 * Revision 1.36  2009/07/28 12:37:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.35  2008/07/24 12:20:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for authenticated JMS
 *
 * Revision 1.34  2008/05/15 14:55:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * make connection available to descender classes
 *
 * Revision 1.33  2008/02/22 14:31:48  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added Selector to getPhysicalDestinationName
 *
 * Revision 1.32  2008/02/19 09:39:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.31  2007/11/23 14:47:55  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fix check on XA connection factories
 *
 * Revision 1.30  2007/11/23 14:17:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * remove XA connectionfactories
 *
 * Revision 1.29  2007/10/16 09:12:27  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge with changes from EJB branch in preparation for creating new EJB brance
 *
 * Revision 1.26.4.4  2007/10/12 09:09:07  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix compilation-problems after code-merge
 *
 * Revision 1.26.4.3  2007/10/10 14:30:42  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * synchronize with HEAD (4.8-alpha1)
 *
 * Revision 1.28  2007/10/10 08:42:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added isUseTopicFunctions()
 *
 * Revision 1.27  2007/09/24 13:03:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused code
 *
 * Revision 1.26  2007/05/23 09:14:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use alternate connectionfactoryname, if appropriate one not set
 *
 * Revision 1.25  2006/10/13 08:14:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.24  2006/02/23 10:48:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed typo in name of setTopicConnectionFactoryName
 *
 * Revision 1.23  2005/12/20 16:59:26  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.22  2005/10/24 15:15:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made sessionsArePooled configurable via appConstant 'jms.sessionsArePooled'
 * added getLogPrefix()
 *
 * Revision 1.21  2005/10/20 15:44:49  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified JMS-classes to use shared connections
 * open()/close() became openFacade()/closeFacade()
 *
 * Revision 1.20  2005/08/02 06:49:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * deliveryMode to String and vv
 * method to send to (reply) destination with msgtype, priority and timetolive
 *
 * Revision 1.19  2005/03/31 08:14:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added todo for setting delivery mode
 *
 * Revision 1.18  2004/10/05 10:41:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed unused imports
 *
 * Revision 1.17  2004/08/23 13:08:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated JavaDoc
 *
 * Revision 1.16  2004/08/18 09:20:58  unknown <unknown@ibissource.org>
 * Make getConnection and closeConnection thread safe
 *
 * Revision 1.15  2004/08/16 11:27:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed timeToLive back to messageTimeToLive
 *
 * Revision 1.14  2004/08/16 09:26:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed messageTimeToLive to timeToLive
 *
 * Revision 1.13  2004/06/16 12:25:52  Johan Verrips <johan.verrips@ibissource.org>
 * Initial version of Queue browsing functionality
 *
 * Revision 1.12  2004/05/21 10:47:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox retriever implementation
 *
 * Revision 1.11  2004/05/03 07:11:32  Johan Verrips <johan.verrips@ibissource.org>
 * Updated message selector behaviour
 *
 * Revision 1.10  2004/04/26 09:58:06  Johan Verrips <johan.verrips@ibissource.org>
 * Added time-to-live on sent messages
 *
 * Revision 1.9  2004/03/31 12:04:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.8  2004/03/30 07:30:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.7  2004/03/26 10:42:51  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.6  2004/03/26 09:50:51  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.5  2004/03/24 08:24:46  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabled XA transactions
 * renamed original 'transacted' into 'jmsTransacted'
 *
 */
package nl.nn.adapterframework.jms;

import java.io.IOException;
import java.util.Map;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.DomBuilderException;

import org.apache.commons.lang.StringUtils;

import com.ibm.mq.jms.JMSC;
import com.ibm.mq.jms.MQQueue;


/**
 * Provides functions for jms connections, queues and topics and acts as a facade
 * to hide for clients whether a <code>Queue</code> or <code>Topic</code> is used. 
 * <br/>
 * The <code>destinationType</code> field specifies which
 * type should be used.<br/>
 * This class sends messages with JMS.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jms.JMSFacade</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>either <code>QUEUE</code> or <code>TOPIC</code></td><td><code>QUEUE</code></td></tr>
 * <tr><td>{@link #setQueueConnectionFactoryName(String) queueConnectionFactoryName}</td><td>jndi-name of the queueConnectionFactory, used when <code>destinationType<code>=</code>QUEUE</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTopicConnectionFactoryName(String) topicConnectionFactoryName}</td><td>jndi-name of the topicConnectionFactory, used when <code>destinationType<code>=</code>TOPIC</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageSelector(String) messageSelector}</td><td>When set, the value of this attribute is used as a selector to filter messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setMessageTimeToLive(long) messageTimeToLive}</td><td>the time (in milliseconds) it takes for the message to expire. If the message is not consumed before, it will be lost. Make sure to set it to a positive value for request/repy type of messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>rather useless attribute, and not the same as <code>deliveryMode</code>. You probably want to use that.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>&nbsp;</td><td>AUTO_ACKNOWLEDGE</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>If the MQ destination is not a JMS receiver, format errors occur.
	 To prevent this, settting <code>forceMQCompliancy</code> to MQ will inform
	 MQ that the replyto queue is not JMS compliant. Setting <code>forceMQCompliancy</code>
	 to "JMS" will cause that on mq the destination is identified as jms-compliant.</td><td>JMS</td></tr>
 * <tr><td>{@link #setCorrelationIdToHex(boolean) correlationIdToHex}</td><td>Transform the value of the correlationId to a hexadecimal value if it starts with ID: (preserving the ID: part). Useful when sending messages to MQ which expects this value to be in hexadecimal format when it starts with ID:, otherwise generating an error: MQJMS1044: String is not a valid hexadecimal number</td><td>false</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to JMS server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 *
 * @author 	Gerrit van Brakel
 * @version Id
 */
public class JMSFacade extends JNDIBase implements INamedObject, HasPhysicalDestination, IXAEnabled {

	public static final String MODE_PERSISTENT="PERSISTENT";
	public static final String MODE_NON_PERSISTENT="NON_PERSISTENT";

	private String name;

	private boolean transacted = false;
	private boolean jmsTransacted = false;
	private String subscriberType = "DURABLE"; // DURABLE or TRANSIENT

    private int ackMode = Session.AUTO_ACKNOWLEDGE;
    private boolean persistent;
	private long messageTimeToLive=0;
    private String destinationName;
    private boolean useTopicFunctions = false;
    private String authAlias;

    private String destinationType="QUEUE"; // QUEUE or TOPIC

    protected MessagingSource messagingSource;
    private Destination destination;



    //<code>forceMQCompliancy</code> is used to perform MQ specific replying.
    //If the MQ destination is not a JMS receiver, format errors occur.
    //To prevent this, settting replyToComplianceType to MQ will inform
    //MQ that the queue (or destination) on which a message is sent, is not JMS compliant.
    
    private String forceMQCompliancy=null;
    private boolean forceTargetClientMQ=false;
	private boolean forceTargetClientJMS=false;
    
    //---------------------------------------------------------------------
    // Queue fields
    //---------------------------------------------------------------------
    private String queueConnectionFactoryName;
    //---------------------------------------------------------------------
    // Topic fields
    //---------------------------------------------------------------------
    private String topicConnectionFactoryName;

	//the MessageSelector will provide filter functionality, as specified
	//javax.jms.Message.
    private String messageSelector=null;

    private boolean correlationIdToHex=false;

	public static int stringToDeliveryMode(String mode) {
		if (MODE_PERSISTENT.equalsIgnoreCase(mode)) {
			return DeliveryMode.PERSISTENT;
		} 
		if (MODE_NON_PERSISTENT.equalsIgnoreCase(mode)) {
			return DeliveryMode.NON_PERSISTENT;
		}
		return 0; 
   }

   public static String deliveryModeToString(int mode) {
	   if (mode==0) {
		   return "not set by application";
	   }
	   if (mode==DeliveryMode.PERSISTENT) {
		   return MODE_PERSISTENT;
	   }
	   if (mode==DeliveryMode.NON_PERSISTENT) {
		   return MODE_NON_PERSISTENT;
	   }
	   return "unknown delivery mode ["+mode+"]";
   }
    
	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	public String getConnectionFactoryName() throws JmsException {
		String result = useTopicFunctions ? getTopicConnectionFactoryName() : getQueueConnectionFactoryName();
		if (StringUtils.isEmpty(result)) {
			throw new JmsException(getLogPrefix()+"no "+(useTopicFunctions ?"topic":"queue")+"ConnectionFactoryName specified");
		}
		return result;
	}

	public String getConnectionFactoryInfo() throws JmsException {
		return getMessagingSource().getPhysicalName();
	}
	
	protected JmsMessagingSource getJmsMessagingSource() throws JmsException {
		return (JmsMessagingSource)getMessagingSource();
	}
	/*
	 * Override this method in descender classes.
	 */
	protected MessagingSourceFactory getMessagingSourceFactory() {
		return new JmsMessagingSourceFactory();
	}
	
	protected MessagingSource getMessagingSource() throws JmsException {
		// We use double-checked locking here
        // We're aware of the risks involved, but consider them
        // to be acceptable since this method is invoked first time
        // from 'configure', at which time only 1 single thread will
        // access the JMS Facade instance.
        if (messagingSource == null) {
            synchronized (this) {
                if (messagingSource == null) {
                    log.debug("instantiating MessagingSourceFactory");
                    MessagingSourceFactory messagingSourceFactory = getMessagingSourceFactory();
                    try {
                        String connectionFactoryName = getConnectionFactoryName();
                        log.debug("creating MessagingSource");
						messagingSource = messagingSourceFactory.getMessagingSource(getJndiContextPrefix()+connectionFactoryName,getAuthAlias());
                    } catch (IbisException e) {
                        if (e instanceof JmsException) {
                                throw (JmsException)e;
                        }
                        throw new JmsException(e);
                    }
                }
            }
		}
		return messagingSource;
	}

   
	/**
	 * Returns a session on the connection for a topic or a queue
	 */
	protected Session createSession() throws JmsException {
		try {
			return getMessagingSource().createSession(isJmsTransacted(), getAckMode());
		} catch (IbisException e) {
			if (e instanceof JmsException) {
				throw (JmsException)e;
			}
			throw new JmsException(e);
		}		
	}

	protected void closeSession(Session session) {
		try {
			getMessagingSource().releaseSession(session);
		} catch (JmsException e) {
			log.warn("Exception releasing session", e);
		}
	}

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getDestinationName())) {
			throw new ConfigurationException("destinationName must be specified");
		}
		if (StringUtils.isEmpty(getDestinationType())) {
			throw new ConfigurationException("destinationType must be specified");
		}
	}

	protected void cleanUpAfterException() {
		try {
			closeFacade();
		} catch (JmsException e) {
			log.warn("exception closing ifsaConnection after previous exception, current:",e);
		}
	}

	/** 
	 * Prepares object for communication on the IFSA bus.
	 * Obtains a connection and a serviceQueue.
	 */
	public void openFacade() throws JmsException {
		try {
			getMessagingSource();   // obtain and cache connection, then start it.
			destination = getDestination();
		} catch (Exception e) {
			cleanUpAfterException();
			throw new JmsException(e);
		}
	}
	/** 
	 * Stops communication on the IFSA bus.
	 * Releases references to serviceQueue and connection.
	 */
	public void closeFacade() throws JmsException {
		try {
			if (messagingSource != null) {
				try {
					messagingSource.close();
				} catch (IbisException e) {
					if (e instanceof JmsException) {
						throw (JmsException)e;
					}
					throw new JmsException(e);
				}
				log.debug("closed connection");
			}
		} finally {
			// make sure all objects are reset, to be able to restart after IFSA parameters have changed (e.g. at iterative installation time)
			destination = null;
			messagingSource = null;
		}
	}

	public TextMessage createTextMessage(Session session, String correlationID, String message)
			throws javax.naming.NamingException, JMSException {
		TextMessage textMessage = null;
		textMessage = session.createTextMessage();
		if (null != correlationID) {
			if (correlationIdToHex && correlationID.startsWith("ID:")) {
				String hexCorrelationID = "ID:";
				int i;
				for (i=3;i<correlationID.length();i++) {
					int c=correlationID.charAt(i);
					hexCorrelationID+=Integer.toHexString(c);
				};
				correlationID = hexCorrelationID;
			}
			textMessage.setJMSCorrelationID(correlationID);
		}
		textMessage.setText(message);
		return textMessage;
	}

	/**
	 * Enforces the setting of <code>forceMQCompliancy</code><br/>.
	 * this method has to be called prior to creating a <code>QueueSender</code>
	 */
 	private void enforceMQCompliancy(Queue queue) throws JMSException {
 		if (forceTargetClientMQ) {
			((MQQueue)queue).setTargetClient(JMSC.MQJMS_CLIENT_NONJMS_MQ);
			if (log.isDebugEnabled()) log.debug("["+name+"] MQ Compliancy for queue ["+queue.toString()+"] set to NONJMS");
 		} else {
			if (forceTargetClientJMS) {
				((MQQueue)queue).setTargetClient(JMSC.MQJMS_CLIENT_JMS_COMPLIANT);
				if (log.isDebugEnabled()) log.debug("MQ Compliancy for queue ["+queue.toString()+"] set to JMS");
			}
 		}
    }

	public Destination getDestination() throws NamingException, JMSException, JmsException  {
	
	    if (destination == null) {
	    	String destinationName = getDestinationName();
	    	if (StringUtils.isEmpty(destinationName)) {
	    		throw new NamingException("no destinationName specified");
	    	}
		    if (!useTopicFunctions || getPersistent()) {
		        destination = getDestination(destinationName);
		    } else {
				TopicSession session = null;
		    	try {
					session = (TopicSession)createSession();
					destination = session.createTopic(destinationName);
		    	} finally {
					closeSession(session);
		    	}
		    }
		    if (destination==null) {
		    	throw new NamingException("cannot get Destination from ["+destinationName+"]");
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
    
    public Destination getDestination(String destinationName) throws JmsException, NamingException {
    	return getJmsMessagingSource().lookupDestination(getJndiContextPrefix()+destinationName);
    }

	/**
	 * Gets a MessageConsumer object for either Topics or Queues.
	 * @param session the Session object
	 * @param destination a Destination object
	 * @param correlationId the Correlation ID as a String value
	 * @return a MessageConsumer with the right filter (messageSelector)
	 * @throws NamingException
	 * @throws JMSException
	 */

	public MessageConsumer getMessageConsumerForCorrelationId(Session session, Destination destination, String correlationId) throws NamingException, JMSException {
		if (correlationId==null)
			return getMessageConsumer(session, destination, null);
		else
			return getMessageConsumer(session, destination, "JMSCorrelationID='" + correlationId + "'");
	}
	
	/**
	 * Create a MessageConsumer. In this overloaded function the selector is taken into account.
	 * This ensures that listeners (or other extensions of this class) do not influence how the selector
	 * is used: when a correlationID should be in the filter the  <code>getMessageConsumerForCorrelationId</code>
	 * should be used, other wise the <code>getMessageConsumer</code> function which has no attribute for
	 * <code>selector</code>. Whe a MessageSelector is set, it will be used when no correlation id is required.
	 * @param session the Session
	 * @param destination the Destination
	 * @param selector the MessageSelector
	 * @return MessageConsumer
	 * @throws NamingException
	 * @throws JMSException
	 */
	public MessageConsumer getMessageConsumer(Session session, Destination destination, String selector) throws NamingException, JMSException {
			if (useTopicFunctions)
				return getTopicSubscriber((TopicSession)session, (Topic)destination, selector);
			else
				return getQueueReceiver((QueueSession)session, (Queue)destination, selector);
	}
	/**
	 * Create a MessageConsumer, on a specific session and for a specific destination.
	 * This functions hides wether we work via Topics or Queues and wether or not a 
	 * messageSelector is set. 
	 * @param session the Session
	 * @param destination the Destination
	 * @return the MessageConsumer
	 * @throws NamingException
	 * @throws JMSException
	 */
	public MessageConsumer getMessageConsumer(Session session, Destination destination) throws NamingException, JMSException {
		return getMessageConsumer(session, destination, getMessageSelector());
	}

    /**
     * gets a sender. if topicName is used the <code>getTopicPublisher()</code>
     * is used, otherwise the <code>getQueueSender()</code>
     */
    public MessageProducer getMessageProducer(Session session, Destination destination)
        throws NamingException, JMSException {
		
		MessageProducer mp;
        if (useTopicFunctions) {
			mp = getTopicPublisher((TopicSession)session, (Topic)destination);
        } else {
			mp = getQueueSender((QueueSession)session, (Queue)destination);
        }
		if (getMessageTimeToLive()>0)
			mp.setTimeToLive(getMessageTimeToLive());	    
        return mp;
    }
    
	public String getPhysicalDestinationName() {
	
	    String result = null;
	
		try {
            Destination d = getDestination();
		    if (d != null) {
	            if (useTopicFunctions)
	                result = ((Topic) d).getTopicName();
	            else
	                result = ((Queue) d).getQueueName();
		    }
	    } catch (Exception je) {
	        log.warn("[" + name + "] got exception in getPhysicalDestinationName", je);
	    }
	    result=getDestinationType()+"("+getDestinationName()+") ["+result+"]";
		if (StringUtils.isNotEmpty(getMessageSelector())) {
			result+=" selector ["+getMessageSelector()+"]";
		}
		try {
			result+=" on "+getMessagingSource().getPhysicalName();
		} catch (JmsException e) {
			log.warn("[" + name + "] got exception in messagingSource.getPhysicalName", e);
		}
	    return result;
	}
	
    /**
     *  Gets a queueReceiver 
     * @see javax.jms.QueueReceiver
     * @return                                   The queueReceiver value
     * @exception  javax.naming.NamingException  Description of the Exception
     * @exception  javax.jms.JMSException                  Description of the Exception
     */
	private QueueReceiver getQueueReceiver(QueueSession session, Queue destination, String selector) throws NamingException, JMSException {
	    QueueReceiver queueReceiver = session.createReceiver(destination, selector);
	    return queueReceiver;
	}
	/**
	  *  Gets the queueSender for a specific queue, not the one in <code>destination</code>
	  * @see javax.jms.QueueSender
	  * @return                                   The queueReceiver value
	  * @exception  javax.naming.NamingException  Description of the Exception
	  * @exception  javax.jms.JMSException
	  */
	private QueueSender getQueueSender(QueueSession session, Queue destination) throws NamingException, JMSException {
	    enforceMQCompliancy(destination);
		QueueSender queueSender = session.createSender(destination);
	    return queueSender;
	}
	
	/**
	 * Gets a topicPublisher for a specified topic
	 */
	private TopicPublisher getTopicPublisher(TopicSession session, Topic topic) throws NamingException, JMSException {
	    return session.createPublisher(topic);
	}
	private TopicSubscriber getTopicSubscriber(TopicSession session, Topic topic, String selector) throws NamingException, JMSException {
	
	    TopicSubscriber topicSubscriber;
	    if (subscriberType.equalsIgnoreCase("DURABLE")) {
	        topicSubscriber =
	            session.createDurableSubscriber(topic, destinationName, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + name  + "] got durable subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
	
	    } else {
	        topicSubscriber = session.createSubscriber(topic, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + name + "] got transient subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
	    }
	
	    return topicSubscriber;
	}



	public String send(Session session, Destination dest, String correlationId, String message, String messageType, long timeToLive, int deliveryMode, int priority) throws NamingException, JMSException, SenderException {
		TextMessage msg = createTextMessage(session, correlationId, message);
		MessageProducer mp;

		if ((session instanceof TopicSession) && (dest instanceof Topic)) {
			mp = getTopicPublisher((TopicSession)session, (Topic)dest);
		} else {
			if ((session instanceof QueueSession) && (dest instanceof Queue)) {
				mp = getQueueSender((QueueSession)session, (Queue)dest);
			} else {
				throw new SenderException("classes of Session ["+session.getClass().getName()+"] and Destination ["+dest.getClass().getName()+"] do not match (Queue vs Topic)");
			}
		}
		if (messageType!=null) {
			msg.setJMSType(messageType); 
		}
		if (deliveryMode>0) {
			msg.setJMSDeliveryMode(deliveryMode);
			mp.setDeliveryMode(deliveryMode);
		}
		if (priority>=0) {
			msg.setJMSPriority(priority);
			mp.setPriority(priority);
		}
		if (timeToLive>0) {
			mp.setTimeToLive(timeToLive);
		}
		String result = send(mp, msg);
		mp.close();
		return result;
	}


	    
	/**
	 * Send a message
	 * @param messageProducer
	 * @param message
	 * @return messageID of the sent message
	 * @throws NamingException
	 * @throws JMSException
	 */    
	public String send(MessageProducer messageProducer, Message message)
	    throws NamingException, JMSException {

	    if (messageProducer instanceof TopicPublisher) {
	         ((TopicPublisher) messageProducer).publish(message);
	    } else {
	         ((QueueSender) messageProducer).send(message);
		}

	    return message.getJMSMessageID();
	}
	/**
	 * Send a message
	 * @param session
	 * @param dest destination
	 * @param message
	 * @return message ID of the sent message
	 * @throws NamingException
	 * @throws JMSException
	 */
	public String send(Session session, Destination dest, Message message)
	    throws NamingException, JMSException {
	
	    if (dest instanceof Topic)
	        return sendByTopic((TopicSession)session, (Topic)dest, message);
	    else
	        return sendByQueue((QueueSession)session, (Queue)dest, message);
	}
	/**
	 * Send a message to a Destination of type Queue.
	 * This method respects the <code>replyToComplianceType</code> field,
	 * as if it is set to "MQ", 
	 * @return messageID of the sent message
	 */
	private String sendByQueue(QueueSession session, Queue destination, Message message)
	    throws NamingException, JMSException {
	    enforceMQCompliancy(destination);
	    QueueSender tqs = session.createSender(destination);
	    tqs.send(message);
	    tqs.close();
	    return message.getJMSMessageID();
	}
	private String sendByTopic(TopicSession session, Topic destination, Message message)
	    throws NamingException, JMSException {
	
	    TopicPublisher tps = session.createPublisher(destination);
	    tps.publish(message);
	    tps.close();
		return message.getJMSMessageID();
	
	}

	public boolean isSessionsArePooled() {
		try {
			return isTransacted() || getMessagingSource().sessionsArePooled();
		} catch (JmsException e) {
			log.error("could not get session",e);
			return false;
		}
	}
    
	/**
	 * Extracts string from message obtained from {@link #getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return String  input message for adapter.
	 */
	public String getStringFromRawMessage(Object rawMessage, Map context, boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper) throws JMSException, DomBuilderException, TransformerException, IOException {
		TextMessage message = null;
		try {
			message = (TextMessage) rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
			return null;
		}
		String rawMessageText;
		rawMessageText= message.getText();
		if (!soap) {
			return rawMessageText;
		}
		String messageText=extractMessageBody(rawMessageText, context, soapWrapper);
		if (StringUtils.isNotEmpty(soapHeaderSessionKey)) {
			String soapHeader=soapWrapper.getHeader(rawMessageText);
			context.put(soapHeaderSessionKey,soapHeader);
		}
		return messageText;
	}

	protected String extractMessageBody(String rawMessageText, Map context, SoapWrapper soapWrapper) throws DomBuilderException, TransformerException, IOException {
		return soapWrapper.getBody(rawMessageText);
	}
    
   
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(super.toString());
        if (useTopicFunctions) {
            sb.append("[topicName=" + destinationName + "]");
	        sb.append("[topicConnectionFactoryName=" + topicConnectionFactoryName + "]");
        } else {
            sb.append("[queueName=" + destinationName + "]");
	        sb.append("[queueConnectionFactoryName=" + queueConnectionFactoryName + "]");
        }
	//  sb.append("[physicalDestinationName="+getPhysicalDestinationName()+"]");
        sb.append("[ackMode=" + getAcknowledgeModeAsString(ackMode) + "]");
        sb.append("[persistent=" + getPersistent() + "]");
        sb.append("[transacted=" + transacted + "]");
        return sb.toString();
    }


	/**
	 * The name of the object. 
	 */
	public void setName(String newName) {
		name = newName;
	}
	public String getName() {
		return name;
	}

	/**
	 * The name of the destination, this may be a <code>queue</code> or <code>topic</code> name. 
	 */
	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}
	public String getDestinationName() {
		return destinationName;
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
    public String getDestinationType() {
		return destinationType;
	}

	/**
	 * Sets the JMS-acknowledge mode. This controls for non transacted listeners the way messages are acknowledged. 
	 * See the jms-documentation.
	 */
	public void setAckMode(int ackMode) {
		this.ackMode = ackMode;
	}
	public int getAckMode() {
		return ackMode;
	}

	/**
	 * Convencience function to convert the numeric value of an (@link #setAckMode(int) acknowledgeMode} to a human-readable string.
	 */
	public static String getAcknowledgeModeAsString(int ackMode) {
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
	 * String-version of {@link #setAckMode(int)}
	 */
	public void setAcknowledgeMode(String acknowledgeMode) {

		if (acknowledgeMode.equalsIgnoreCase("auto") || acknowledgeMode.equalsIgnoreCase("AUTO_ACKNOWLEDGE")) {
			ackMode = Session.AUTO_ACKNOWLEDGE;
		} else
			if (acknowledgeMode.equalsIgnoreCase("dups") || acknowledgeMode.equalsIgnoreCase("DUPS_OK_ACKNOWLEDGE")) {
				ackMode = Session.DUPS_OK_ACKNOWLEDGE;
			} else
				if (acknowledgeMode.equalsIgnoreCase("client") || acknowledgeMode.equalsIgnoreCase("CLIENT_ACKNOWLEDGE")) {
					ackMode = Session.CLIENT_ACKNOWLEDGE;
				} else {
					// ignore all ack modes, to test no acking
					log.warn("["+name+"] invalid acknowledgemode:[" + acknowledgeMode + "] setting no acknowledge");
					ackMode = -1;
				}

	}
	/**
	 * String-version of {@link #getAckMode()}
	 */
	public String getAcknowledgeMode() {
		return getAcknowledgeModeAsString(getAckMode());
	}


	/**
	 * Controls whether messages are processed persistently. 
	 * 
	 * When set <code>true</code>, the JMS provider ensures that messages aren't lost when the application might crash.  
	 */
	public void setPersistent(boolean value) {
		persistent = value;
	}
	public boolean getPersistent() {
		return persistent;
	}

	/**
	 * SubscriberType should <b>DURABLE</b> or <b>TRANSIENT</b>
	 * Only applicable for topics <br/>
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
	public String getSubscriberType() {
		return subscriberType;
	}


	/**
	 * <code>forceMQCompliancy</code> is used to perform MQ specific sending.
	 * If the MQ destination is not a JMS receiver, format errors occur.
	 * To prevent this, settting <code>forceMQCompliancy</code>  to MQ will inform
	 * MQ that the replyto queue is not JMS compliant. Setting <code>forceMQCompliancy</code>
	 * to "JMS" will cause that on mq the destination is identified as jms-compliant.
	 * Other specifics information for different providers may be
	 * implemented. Defaults to "JMS".<br/>
	 */
	public void setForceMQCompliancy(String forceMQCompliancy) {
		if (forceMQCompliancy.equals("MQ")) {
			forceTargetClientMQ=true; 
			forceTargetClientJMS=false;
		} else { 
			if (forceMQCompliancy.equals("JMS")) {
				forceTargetClientMQ=false; 
				forceTargetClientJMS=true;
			} else {
				throw new IllegalArgumentException("forceMQCompliancy has a wrong value ["+forceMQCompliancy+"] should be 'JMS' or 'MQ'");
			}
		}
		this.forceMQCompliancy=forceMQCompliancy;
	}
	public String getForceMQCompliancy() {
		return forceMQCompliancy;
	}


	/**
	 * The JNDI-name of the connection factory to use to connect to a <i>queue</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions. 
	 */
	public void setQueueConnectionFactoryName(String name) {
		queueConnectionFactoryName=name;
	}
	public String getQueueConnectionFactoryName() {
		return queueConnectionFactoryName;
	}

	/**
	 * The JNDI-name of the connection factory to use to connect to a <i>queue</i> if {@link #isTransacted()} returns <code>true</code>.
	 * The corresponding connection factory should support XA transactions. 
	 * @deprecated please use 'setQueueConnectionFactoryName()' instead
	 */
	public void setQueueConnectionFactoryNameXA(String queueConnectionFactoryNameXA) {
		if (StringUtils.isNotEmpty(queueConnectionFactoryNameXA)) {
			throw new IllegalArgumentException(getLogPrefix()+"use of attribute 'queueConnectionFactoryNameXA' is no longer supported. The queueConnectionFactory can now only be specified using attribute 'queueConnectionFactoryName'");
		}
	}


	/**
	 * The JNDI-name of the connection factory to use to connect to a <i>topic</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions. 
	 */
	public void setTopicConnectionFactoryName(String topicConnectionFactoryName) {
		this.topicConnectionFactoryName = topicConnectionFactoryName;
	}
	public String getTopicConnectionFactoryName() {
		return topicConnectionFactoryName;
	}

	/**
	 * The JNDI-name of the connection factory to use to connect to a <i>topic</i> if {@link #isTransacted()} returns <code>true</code>.
	 * The corresponding connection factory should support XA transactions. 
	 * @deprecated please use 'setTopicConnectionFactoryName()' instead
	 */
	public void setTopicConnectionFactoryNameXA(String topicConnectionFactoryNameXA) {
		if (StringUtils.isNotEmpty(topicConnectionFactoryNameXA)) {
			throw new IllegalArgumentException(getLogPrefix()+"use of attribute 'topicConnectionFactoryNameXA' is no longer supported. The topicConnectionFactory can now only be specified using attribute 'topicConnectionFactoryName'");
		}
	}

	/**
	 * Controls the use of JMS transacted session.
	 * In versions prior to 4.1, this attribute was called plainly 'transacted'. The {@link #setTransacted(boolean) transacted} 
	 * attribute, however, is now in uses to indicate the use of XA-transactions. XA transactions can be used 
	 * in a pipeline to simultaneously (in one transaction) commit or rollback messages send to a number of queues, or
	 * even together with database actions.
	 * 
	 * @since 4.1
	 * 
	 * @deprecated This attribute has been added to provide the pre-4.1 transaction functionality to configurations that
	 * relied this specific functionality. New configurations should not use it. 
	 * 
	 */
	public void setJmsTransacted(boolean jmsTransacted) {
		this.jmsTransacted = jmsTransacted;
	}
	public boolean isJmsTransacted() {
		return jmsTransacted;
	}

	public void setCorrelationIdToHex(boolean correlationIdToHex) {
		this.correlationIdToHex = correlationIdToHex;
	}

	/**
	 * Controls whether messages are send under transaction control.
	 * If set <code>true</code>, messages are committed or rolled back under control of an XA-transaction.
	 */
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
	/**
	 * Set the time-to-live in milliseconds of a message
	 * @param exp time in milliseconds
	 */
	public void setMessageTimeToLive(long ttl){
		this.messageTimeToLive=ttl;
	}
	/**
	 * Get the  time-to-live in milliseconds of a message
	 * @param exp time in milliseconds
	 */
	public long getMessageTimeToLive(){
		return this.messageTimeToLive;
	}

	public boolean isCorrelationIdToHex() {
		return correlationIdToHex;
	}

	/**
	 * Indicates whether messages are send under transaction control.
	 * @see #setTransacted(boolean)
	 */
	public boolean isTransacted() {
		return transacted;
	}

    public boolean isUseTopicFunctions() {
        return useTopicFunctions;
    }
    
	public void setMessageSelector(String newMessageSelector) {
		this.messageSelector=newMessageSelector;
	}
	public String getMessageSelector() {
		return messageSelector;
	}
    

	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

}
