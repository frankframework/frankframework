/*
   Copyright 2013, 2015, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.jms;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
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

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;


/**
 * Provides functions for jms connections, queues and topics and acts as a facade
 * to hide for clients whether a <code>Queue</code> or <code>Topic</code> is used.
 * <br/>
 * The <code>destinationType</code> field specifies which
 * type should be used.<br/>
 * This class sends messages with JMS.
 *
 *
 * @author 	Gerrit van Brakel
 */
public class JMSFacade extends JNDIBase implements IConfigurable, INamedObject, HasPhysicalDestination, IXAEnabled {

	public static final String MODE_PERSISTENT     = "PERSISTENT";
	public static final String MODE_NON_PERSISTENT = "NON_PERSISTENT";

	private String name;

	private boolean createDestination = AppConstants.getInstance().getBoolean("jms.createDestination", false);
	private boolean useJms102 = AppConstants.getInstance().getBoolean("jms.useJms102", false);

	private boolean transacted = false;
	private boolean jmsTransacted = false;
	private String subscriberType = "DURABLE"; // DURABLE or TRANSIENT

	private int ackMode = Session.AUTO_ACKNOWLEDGE;
	private boolean persistent;
	private long messageTimeToLive = 0;
	private String destinationName;
	private boolean useTopicFunctions = false;
	private String authAlias;
	private boolean lookupDestination = true;

	private String destinationType = "QUEUE"; // QUEUE or TOPIC

	protected MessagingSource messagingSource;
	private Destination destination;

	private Map<String, ConnectionFactory> proxiedConnectionFactories;
	private Map<String, String> proxiedDestinationNames;

	// ---------------------------------------------------------------------
	// Queue fields
	// ---------------------------------------------------------------------
	private String queueConnectionFactoryName;
	// ---------------------------------------------------------------------
	// Topic fields
	// ---------------------------------------------------------------------
	private String topicConnectionFactoryName;

	// the MessageSelector will provide filter functionality, as specified
	// javax.jms.Message.
	private String messageSelector = null;

	private boolean correlationIdToHex = false;
	private String correlationIdToHexPrefix = "ID:";
	private int correlationIdMaxLength = -1;

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
		if (mode == 0) {
			return "not set by application";
		}
		if (mode == DeliveryMode.PERSISTENT) {
			return MODE_PERSISTENT;
		}
		if (mode == DeliveryMode.NON_PERSISTENT) {
			return MODE_NON_PERSISTENT;
		}
		return "unknown delivery mode [" + mode + "]";
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	public boolean useJms102() {
		return useJms102;
	}

	public void setProxiedConnectionFactories(Map<String, ConnectionFactory> proxiedConnectionFactories) {
		this.proxiedConnectionFactories = proxiedConnectionFactories;
	}

	public Map<String, ConnectionFactory> getProxiedConnectionFactories() {
		return proxiedConnectionFactories;
	}

	public void setProxiedDestinationNames(Map<String, String> proxiedDestinationNames) {
		this.proxiedDestinationNames = proxiedDestinationNames;
	}

	public Map<String, String> getProxiedDestinationNames() {
		return proxiedDestinationNames;
	}

	public String getConnectionFactoryName() throws JmsException {
		String result = useTopicFunctions ? getTopicConnectionFactoryName() : getQueueConnectionFactoryName();
		if (StringUtils.isEmpty(result)) {
			throw new JmsException(getLogPrefix()+"no "+(useTopicFunctions ?"topic":"queue")+"ConnectionFactoryName specified");
		}
		return result;
	}

	public Object getManagedConnectionFactory() throws JmsException {
		return getMessagingSource().getManagedConnectionFactory();
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
		return new JmsMessagingSourceFactory(this);
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
						messagingSource = messagingSourceFactory.getMessagingSource(connectionFactoryName, getAuthAlias(), createDestination, useJms102);
					} catch (IbisException e) {
						if (e instanceof JmsException) {
							throw (JmsException) e;
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

	/**
	 * Obtains a connection and a serviceQueue.
	 */
	public void open() throws Exception {
		try {
			getMessagingSource();   // obtain and cache connection, then start it.
			destination = getDestination();
		} catch (Exception e) {
			close();
			throw e;
		}
	}

	/**
	 * Releases references to serviceQueue and connection.
	 */
	@Override
	public void close() {
		try {
			if (messagingSource != null) {
				try {
					messagingSource.close();
				} catch (IbisException e) {
					log.warn(getLogPrefix() + "caught exception closing messaging source", e);
				}
				log.debug("closed connection");
			}
		} finally {
			// make sure all objects are reset, to be able to restart after IFSA parameters have changed (e.g. at iterative installation time)
			destination = null;
			messagingSource = null;
		}
	}

	public javax.jms.Message createMessage(Session session, String correlationID, String message) throws NamingException, JMSException {
		TextMessage textMessage = null;
		textMessage = session.createTextMessage();
		setMessageCorrelationID(textMessage, correlationID);
		textMessage.setText(message);
		return textMessage;
	}

	public void setMessageCorrelationID(javax.jms.Message message, String correlationID)
			throws JMSException {
		if (null != correlationID) {
			if (correlationIdMaxLength>=0) {
				int cidlen;
				if (correlationID.startsWith(correlationIdToHexPrefix)) {
					cidlen = correlationID.length()-correlationIdToHexPrefix.length();
				} else {
					cidlen = correlationID.length();
				}
				if (cidlen>correlationIdMaxLength) {
					correlationID = correlationIdToHexPrefix+correlationID.substring(correlationID.length()-correlationIdMaxLength);
					if (log.isDebugEnabled()) log.debug("correlationId shortened to ["+correlationID+"]");
				}
			}
			if (correlationIdToHex && correlationID.startsWith(correlationIdToHexPrefix)) {
				String hexCorrelationID = correlationIdToHexPrefix;
				int i;
				for (i=correlationIdToHexPrefix.length();i<correlationID.length();i++) {
					int c=correlationID.charAt(i);
					hexCorrelationID+=Integer.toHexString(c);
				};
				correlationID = hexCorrelationID;
				if (log.isDebugEnabled()) log.debug("correlationId changed, based on hexidecimal values, to ["+correlationID+"]");
			}
			message.setJMSCorrelationID(correlationID);
		}
	}
	
	public Destination getDestination() throws NamingException, JMSException, JmsException {
		if (destination == null) {
			String destinationName = getDestinationName();
			if (StringUtils.isEmpty(destinationName)) {
				throw new NamingException("no destinationName specified");
			}
			if (isLookupDestination()) {
				if (!useTopicFunctions || getPersistent()) {
					destination = getDestination(destinationName);
				} else {
					TopicSession session = null;
					try {
						session = (TopicSession) createSession();
						destination = session.createTopic(destinationName);
					} finally {
						closeSession(session);
					}
				}
			} else {
				destination = getJmsMessagingSource().createDestination(destinationName);
			}
			if (destination == null) {
				throw new NamingException("cannot get Destination from [" + destinationName + "]");
			}
		}
		return destination;
	}

	/**
	 * Utilitiy function to retrieve a Destination from a jndi.
	 */
	public Destination getDestination(String destinationName) throws JmsException, NamingException {
		return getJmsMessagingSource().lookupDestination(destinationName);
	}

	/**
	 * Gets a MessageConsumer object for either Topics or Queues.
	 * @return a MessageConsumer with the right filter (messageSelector)
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
	 * <code>selector</code>. When a MessageSelector is set, it will be used when no correlation id is required.
	 * @param session the Session
	 * @param destination the Destination
	 * @param selector the MessageSelector
	 * @return MessageConsumer
	 */
	public MessageConsumer getMessageConsumer(Session session, Destination destination, String selector) throws NamingException, JMSException {
		if (useTopicFunctions) {
			if (useJms102()) {
				return getTopicSubscriber((TopicSession)session, (Topic)destination, selector);
			} else {
				return getTopicSubscriber(session, (Topic)destination, selector);
			}
		} else {
			if (useJms102()) {
				return getQueueReceiver((QueueSession)session, (Queue)destination, selector);
			} else {
				return session.createConsumer(destination, selector);
			}
		}
	}
	/**
	 * Create a MessageConsumer, on a specific session and for a specific destination.
	 * This functions hides wether we work via Topics or Queues and wether or not a
	 * messageSelector is set.
	 * @param session the Session
	 * @param destination the Destination
	 * @return the MessageConsumer
	 */
	public MessageConsumer getMessageConsumer(Session session, Destination destination) throws NamingException, JMSException {
		return getMessageConsumer(session, destination, getMessageSelector());
	}

	public MessageProducer getMessageProducer(Session session,
			Destination destination) throws NamingException, JMSException {
		MessageProducer mp;
		if (useJms102()) {
			if (useTopicFunctions) {
				mp = getTopicPublisher((TopicSession)session, (Topic)destination);
			} else {
				mp = getQueueSender((QueueSession)session, (Queue)destination);
			}
		} else {
			mp = session.createProducer(destination);
		}
		if (getMessageTimeToLive()>0)
			mp.setTimeToLive(getMessageTimeToLive());
		return mp;
	}

	public String getPhysicalDestinationShortName() {
		try {
			return getPhysicalDestinationShortName(false);
		} catch (JmsException e) {
			log.warn("[" + name + "] got exception in getPhysicalDestinationShortName", e);
			return null;
		}
	}

	public String getPhysicalDestinationShortName(boolean throwException) throws JmsException {
		String result = null;
		try {
			Destination d = getDestination();
			if (d != null) {
				if (useTopicFunctions)
					result = ((Topic) d).getTopicName();
				else
					result = ((Queue) d).getQueueName();
			}
		} catch (Exception e) {
			if (throwException) {
				throw new JmsException(e);
			} else {
				log.warn("[" + name + "] got exception in getPhysicalDestinationShortName", e);
			}
		}
		return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		String result = getDestinationType()+"("+getDestinationName()+") ["+getPhysicalDestinationShortName()+"]";
		if (StringUtils.isNotEmpty(getMessageSelector())) {
			result+=" selector ["+getMessageSelector()+"]";
		}
		JmsRealm jmsRealm=null;
		if (getJmsRealmName()!=null) {
			jmsRealm=JmsRealmFactory.getInstance().getJmsRealm(getJmsRealmName());
		}
	    if (jmsRealm==null) {
	    	log.warn("Could not find jmsRealm ["+getJmsRealmName()+"]");
	    } else {
			result+=" on ("+jmsRealm.retrieveConnectionFactoryName()+")";
		}
		return result;
	}

	/**
	 * Gets a queueReceiver value
	 * @see QueueReceiver
	 */
	private QueueReceiver getQueueReceiver(QueueSession session, Queue destination, String selector) throws NamingException, JMSException {
		QueueReceiver queueReceiver = session.createReceiver(destination, selector);
		return queueReceiver;
	}
	
	/**
	  * Gets the queueSender for a specific queue, not the one in <code>destination</code>
	  * @see QueueSender
	  * @return The queueReceiver value
	  */
	private QueueSender getQueueSender(QueueSession session, Queue destination) throws NamingException, JMSException {
		return session.createSender(destination);
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
			topicSubscriber = session.createDurableSubscriber(topic, destinationName, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + name + "] got durable subscriber for topic [" + destinationName + "] with selector [" + selector + "]");

		} else {
			topicSubscriber = session.createSubscriber(topic, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + name + "] got transient subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
		}

		return topicSubscriber;
	}

	private MessageConsumer getTopicSubscriber(Session session, Topic topic, String selector) throws NamingException, JMSException {
		MessageConsumer messageConsumer;
		if (subscriberType.equalsIgnoreCase("DURABLE")) {
			messageConsumer = session.createDurableSubscriber(topic, destinationName, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + name  + "] got durable subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
		} else {
			messageConsumer = session.createConsumer(topic, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + name + "] got transient subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
		}
		return messageConsumer;
	}


	// TODO: send functions could benefit from have Message messages instead of String
	public String send(Session session, Destination dest, String correlationId, String message, String messageType, long timeToLive, int deliveryMode, int priority) throws NamingException, JMSException, SenderException {
		return send(session, dest, correlationId, message, messageType, timeToLive, deliveryMode, priority, false);
	}
	public String send(Session session, Destination dest, String correlationId, String message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException) throws NamingException, JMSException, SenderException {
		return send(session, dest, correlationId, message, messageType, timeToLive, deliveryMode, priority, ignoreInvalidDestinationException, null);
	}
	public String send(Session session, Destination dest, String correlationId, String message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException, Map<String, Object> properties) throws NamingException, JMSException, SenderException {
		javax.jms.Message msg = createMessage(session, correlationId, message);
		MessageProducer mp;
		try {
			if (useJms102()) {
				if ((session instanceof TopicSession) && (dest instanceof Topic)) {
					mp = getTopicPublisher((TopicSession)session, (Topic)dest);
				} else {
					if ((session instanceof QueueSession) && (dest instanceof Queue)) {
						mp = getQueueSender((QueueSession)session, (Queue)dest);
					} else {
						throw new SenderException("classes of Session ["+session.getClass().getName()+"] and Destination ["+dest.getClass().getName()+"] do not match (Queue vs Topic)");
					}
				}
			} else {
				mp = session.createProducer(dest);
			}
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue ["+dest+"] doesn't exist");
				return null;
			} else {
				throw e;
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
		if (properties!=null) {
			for (Iterator<String> it = properties.keySet().iterator(); it.hasNext();) {
				String key = it.next();
				Object value = properties.get(key);
				log.debug("setting property ["+name+"] to value ["+value+"]");
				msg.setObjectProperty(key, value);
			}
		}
		String result = send(mp, msg, ignoreInvalidDestinationException);
		mp.close();
		return result;
	}

	/**
	 * Send a message
	 * @param messageProducer
	 * @param message
	 * @return messageID of the sent message
	 */
	public String send(MessageProducer messageProducer, javax.jms.Message message) throws NamingException, JMSException {
		return send(messageProducer, message, false);
	}
	public String send(MessageProducer messageProducer, javax.jms.Message message, boolean ignoreInvalidDestinationException) throws NamingException, JMSException {
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"sender on ["+ getDestinationName() 
				+ "] will send message with JMSDeliveryMode=[" + message.getJMSDeliveryMode()
				+ "] \n  JMSMessageID=[" + message.getJMSMessageID()
				+ "] \n  JMSCorrelationID=[" + message.getJMSCorrelationID()
				+ "] \n  JMSTimestamp=[" + DateUtils.format(message.getJMSTimestamp()) 
				+ "] \n  JMSExpiration=[" + message.getJMSExpiration()
				+ "] \n  JMSPriority=[" + message.getJMSPriority()
				+ "] \n Message=[" + message.toString()
				+ "]");
		}
		try {
			if (useJms102()) {
				if (messageProducer instanceof TopicPublisher) {
					((TopicPublisher) messageProducer).publish(message);
				} else {
					((QueueSender) messageProducer).send(message);
				}
				return message.getJMSMessageID();
			} else {
				messageProducer.send(message);
				return message.getJMSMessageID();
			}
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue ["+messageProducer.getDestination()+"] doesn't exist");
				return null;
			} else {
				throw e;
			}
		}
	}
	
	/**
	 * Send a message
	 * @param session
	 * @param dest destination
	 * @param message
	 * @return message ID of the sent message
	 */
	public String send(Session session, Destination dest, javax.jms.Message message)
		throws NamingException, JMSException {
		return send(session, dest, message, false);
	}
	public String send(Session session, Destination dest, javax.jms.Message message, boolean ignoreInvalidDestinationException)
			throws NamingException, JMSException {
		try {
			if (useJms102()) {
				if (dest instanceof Topic) {
					return sendByTopic((TopicSession)session, (Topic)dest, message);
				} else {
					return sendByQueue((QueueSession)session, (Queue)dest, message);
				}
			} else {
				MessageProducer mp = session.createProducer(dest);
				mp.send(message);
				mp.close();
				return message.getJMSMessageID();
			}
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue ["+dest+"] doesn't exist");
				return null;
			} else {
				throw e;
			}
		}
	}

	protected String sendByQueue(QueueSession session, Queue destination, javax.jms.Message message) throws NamingException, JMSException {
		QueueSender tqs = session.createSender(destination);
		tqs.send(message);
		tqs.close();
		return message.getJMSMessageID();
	}

	protected String sendByTopic(TopicSession session, Topic destination, javax.jms.Message message) throws NamingException, JMSException {
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
	 * Extracts string from message obtained from getRawMessage(Map). May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return String  input message for adapter.
	 */
	public Message extractMessage(Object rawMessage, Map<String,Object> context, boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper) throws JMSException, SAXException, TransformerException, IOException {
//		TextMessage message = null;
		String rawMessageText;
/*
		try {
			message = (TextMessage) rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
			return null;
		}
		rawMessageText= message.getText();
*/
		if (rawMessage instanceof IMessageWrapper) {
			rawMessageText = ((IMessageWrapper)rawMessage).getMessage().asString();
		} else if (rawMessage instanceof TextMessage) {
			rawMessageText = ((TextMessage)rawMessage).getText();
		} else {
			rawMessageText = (String)rawMessage;
		}
		if (!soap) {
			return new Message(rawMessageText);
		}
		String messageText=extractMessageBody(rawMessageText, context, soapWrapper);
		if (StringUtils.isNotEmpty(soapHeaderSessionKey)) {
			String soapHeader=soapWrapper.getHeader(rawMessageText);
			context.put(soapHeaderSessionKey,soapHeader);
		}
		return new Message(messageText);
	}

	protected String extractMessageBody(String rawMessageText, Map<String,Object> context, SoapWrapper soapWrapper) throws SAXException, TransformerException, IOException {
		return soapWrapper.getBody(rawMessageText);
	}

	@Override
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


	@Override
	@IbisDoc({"name of the listener", ""})
	public void setName(String newName) {
		name = newName;
	}
	@Override
	public String getName() {
		return name;
	}

	/**
	 * The name of the destination, this may be a <code>queue</code> or <code>topic</code> name.
	 */
	@IbisDoc({"name of the jms destination (queue or topic) to use", ""})
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
	@IbisDoc({"either <code>queue</code> or <code>topic</code>", "<code>queue</code>"})
	public void setDestinationType(String type) {
		this.destinationType = type;
		if (destinationType.equalsIgnoreCase("TOPIC")) {
			useTopicFunctions = true;
		} else {
			useTopicFunctions = false;
		}
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
	@IbisDoc({"", "auto_acknowledge"})
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
	@IbisDoc({"rather useless attribute, and not the same as <code>deliverymode</code>. you probably want to use that.", ""})
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
	 * The JNDI-name of the connection factory to use to connect to a <i>queue</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	@IbisDoc({"jndi-name of the queueconnectionfactory, used when <code>destinationtype<code>=</code>queue</code>", ""})
	public void setQueueConnectionFactoryName(String name) {
		queueConnectionFactoryName=name;
	}
	public String getQueueConnectionFactoryName() {
		return queueConnectionFactoryName;
	}


	/**
	 * The JNDI-name of the connection factory to use to connect to a <i>topic</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	@IbisDoc({"jndi-name of the topicconnectionfactory, used when <code>destinationtype<code>=</code>topic</code>", ""})
	public void setTopicConnectionFactoryName(String topicConnectionFactoryName) {
		this.topicConnectionFactoryName = topicConnectionFactoryName;
	}
	public String getTopicConnectionFactoryName() {
		return topicConnectionFactoryName;
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
	@Deprecated
	public void setJmsTransacted(boolean jmsTransacted) {
		this.jmsTransacted = jmsTransacted;
	}
	public boolean isJmsTransacted() {
		return jmsTransacted;
	}

	@IbisDoc({"transform the value of the correlationid to a hexadecimal value if it starts with id: (preserving the id: part). useful when sending messages to mq which expects this value to be in hexadecimal format when it starts with id:, otherwise generating the error: mqjms1044: string is not a valid hexadecimal number", "false"})
	public void setCorrelationIdToHex(boolean correlationIdToHex) {
		this.correlationIdToHex = correlationIdToHex;
	}

	@IbisDoc({"prefix to check before executing correlationidtohex. when empty (and correlationidtohex equals true) all correlationid's are transformed, this is useful in case you want the entire correlationid to be transformed (for example when the receiving party doesn't allow characters like a colon to be present in the correlationid).", "id:"})
	public void setCorrelationIdToHexPrefix(String correlationIdToHexPrefix) {
		this.correlationIdToHexPrefix = correlationIdToHexPrefix;
	}

	/**
	 * Controls whether messages are send under transaction control.
	 * If set <code>true</code>, messages are committed or rolled back under control of an XA-transaction.
	 */
	@IbisDoc({"", "false"})
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
	/**
	 * Set the time-to-live in milliseconds of a message
	 * @param ttl exp time in milliseconds
	 */
	@IbisDoc({"the time (in milliseconds) it takes for the message to expire. if the message is not consumed before, it will be lost. make sure to set it to a positive value for request/repy type of messages.", "0 (unlimited)"})
	public void setMessageTimeToLive(long ttl){
		this.messageTimeToLive=ttl;
	}
	/**
	 * Get the  time-to-live in milliseconds of a message
	 */
	public long getMessageTimeToLive(){
		return this.messageTimeToLive;
	}

	public boolean isCorrelationIdToHex() {
		return correlationIdToHex;
	}

	@IbisDoc({"if set (>=0) and the length of the correlationid exceeds this maximum length, the correlationid is trimmed from the left side of a string to this maximum length", "-1"})
	public void setCorrelationIdMaxLength(int i) {
		correlationIdMaxLength = i;
	}
	public int getCorrelationIdMaxLength() {
		return correlationIdMaxLength;
	}

	/**
	 * Indicates whether messages are send under transaction control.
	 * @see #setTransacted(boolean)
	 */
	@Override
	public boolean isTransacted() {
		return transacted;
	}

    public boolean isUseTopicFunctions() {
        return useTopicFunctions;
    }

	@IbisDoc({"when set, the value of this attribute is used as a selector to filter messages.", "0 (unlimited)"})
	public void setMessageSelector(String newMessageSelector) {
		this.messageSelector=newMessageSelector;
	}
	public String getMessageSelector() {
		return messageSelector;
	}


	@IbisDoc({"alias used to obtain credentials for authentication to jms server", ""})
	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"when set <code>false</code>, the destinationname is used directly instead of performing a jndi lookup", "true"})
	public void setLookupDestination(boolean b) {
		lookupDestination = b;
	}
	public boolean isLookupDestination() {
		return lookupDestination;
	}
}