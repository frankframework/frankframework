/*
   Copyright 2013, 2015, 2018 Nationale-Nederlanden, 2020, 2021 WeAreFrank!

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
import java.util.concurrent.ConcurrentHashMap;

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

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IMessageWrapper;
import nl.nn.adapterframework.core.IXAEnabled;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.jndi.JndiBase;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.Misc;


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
public class JMSFacade extends JndiBase implements HasPhysicalDestination, IXAEnabled {

	private boolean createDestination = AppConstants.getInstance().getBoolean("jms.createDestination", false);
	private boolean useJms102 = AppConstants.getInstance().getBoolean("jms.useJms102", false);

	private boolean transacted = false;
	private boolean jmsTransacted = false;
	private SubscriberType subscriberType = SubscriberType.DURABLE;

	private AcknowledgeMode ackMode = AcknowledgeMode.AUTO_ACKNOWLEDGE;
	private boolean persistent;
	private long messageTimeToLive = 0;
	private String destinationName;
	private boolean useTopicFunctions = false;
	private String authAlias;
	private boolean lookupDestination = true;

	private DestinationType destinationType = DestinationType.QUEUE; // QUEUE or TOPIC

	protected MessagingSource messagingSource;
	private Map<String,Destination> destinations = new ConcurrentHashMap<>();

	private @Setter @Getter IConnectionFactoryFactory connectionFactoryFactory = null;
	private @Setter @Getter Map<String, String> proxiedDestinationNames;

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

	public enum AcknowledgeMode {
		NOT_SET(0, ""),
		AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE, "auto"),
		CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE, "client"),
		DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE, "dups");
		
		private @Getter int acknowledgeMode;
		private @Getter String shortName;
		private AcknowledgeMode(int acknowledgeMode, String shortName) {
			this.acknowledgeMode = acknowledgeMode;
			this.shortName=shortName;
		}
	}
	
	public enum DeliveryMode {
		NOT_SET(0),
		PERSISTENT(javax.jms.DeliveryMode.PERSISTENT),
		NON_PERSISTENT(javax.jms.DeliveryMode.NON_PERSISTENT);
		
		private @Getter int deliveryMode;
		private DeliveryMode(int deliveryMode) {
			this.deliveryMode = deliveryMode;
		}
		
		public static DeliveryMode parse(int deliveryMode) {
			return Misc.parseFromField(DeliveryMode.class, deliveryMode, d -> d.getDeliveryMode());
		}
	}

	public enum SubscriberType {
		DURABLE,
		TRANSIENT;
	}

	public enum DestinationType {
		QUEUE,
		TOPIC;
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	public boolean useJms102() {
		return useJms102;
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
			return getMessagingSource().createSession(isJmsTransacted(), getAckModeEnum().getAcknowledgeMode());
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

	/**
	 * Obtains a connection and a serviceQueue.
	 */
	public void open() throws Exception {
		try {
			getMessagingSource(); // obtain and cache connection, then start it.
			if (StringUtils.isNotEmpty(getDestinationName())) {
				getDestination();
			}
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
			destinations.clear();
			messagingSource = null;
		}
	}

	public javax.jms.Message createMessage(Session session, String correlationID, Message message) throws NamingException, JMSException, IOException {
		TextMessage textMessage = null;
		textMessage = session.createTextMessage();
		setMessageCorrelationID(textMessage, correlationID);
		textMessage.setText(message.asString());
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
		if (StringUtils.isEmpty(getDestinationName())) {
			throw new JmsException("no (default) destinationName specified");
		}
		return getDestination(getDestinationName());
	}
	
	public Destination getDestination(String destinationName) throws NamingException, JMSException, JmsException {
		return destinations.computeIfAbsent(destinationName, name -> computeDestination(name));
	}
	
	@SneakyThrows
	private Destination computeDestination(String destinationName) {
		Destination result;
		if (StringUtils.isEmpty(destinationName)) {
			throw new NamingException("no destinationName specified");
		}
		if (isLookupDestination()) {
			if (!useTopicFunctions || getPersistent()) {
				result = getJmsMessagingSource().lookupDestination(destinationName);
			} else {
				TopicSession session = null;
				try {
					session = (TopicSession) createSession();
					result = session.createTopic(destinationName);
				} finally {
					closeSession(session);
				}
			}
		} else {
			result = getJmsMessagingSource().createDestination(destinationName);
		}
		if (result == null) {
			throw new NamingException("cannot get Destination from [" + destinationName + "]");
		}
		return result;
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

	public MessageProducer getMessageProducer(Session session, Destination destination) throws NamingException, JMSException {
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
			log.warn("[" + getName() + "] got exception in getPhysicalDestinationShortName", e);
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
				log.warn("[" + getName() + "] got exception in getPhysicalDestinationShortName", e);
			}
		}
		return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		String result = getDestinationTypeEnum()+"("+getDestinationName()+") ["+getPhysicalDestinationShortName()+"]";
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
		switch (subscriberType) {
		case DURABLE:
			topicSubscriber = session.createDurableSubscriber(topic, destinationName, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + getName() + "] got durable subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
			break;
		case TRANSIENT:
			topicSubscriber = session.createSubscriber(topic, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + getName() + "] got transient subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
			break;
		default:
			throw new IllegalStateException("Unexpected subscriberType ["+subscriberType+"]");
		}
		return topicSubscriber;
	}

	private MessageConsumer getTopicSubscriber(Session session, Topic topic, String selector) throws NamingException, JMSException {
		MessageConsumer messageConsumer;
		switch (subscriberType) {
		case DURABLE:
			messageConsumer = session.createDurableSubscriber(topic, destinationName, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + getName()  + "] got durable subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
			break;
		case TRANSIENT:
			messageConsumer = session.createConsumer(topic, selector, false);
			if (log.isDebugEnabled()) log.debug("[" + getName() + "] got transient subscriber for topic [" + destinationName + "] with selector [" + selector + "]");
			break;
		default:
			throw new IllegalStateException("Unexpected subscriberType ["+subscriberType+"]");
		}
		return messageConsumer;
	}

	public String send(Session session, Destination dest, String correlationId, Message message, String messageType, long timeToLive, int deliveryMode, int priority) throws NamingException, JMSException, SenderException, IOException {
		return send(session, dest, correlationId, message, messageType, timeToLive, deliveryMode, priority, false);
	}
	public String send(Session session, Destination dest, String correlationId, Message message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException) throws NamingException, JMSException, SenderException, IOException {
		return send(session, dest, correlationId, message, messageType, timeToLive, deliveryMode, priority, ignoreInvalidDestinationException, null);
	}
	public String send(Session session, Destination dest, String correlationId, Message message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException, Map<String, Object> properties) throws NamingException, JMSException, SenderException, IOException {
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
				log.debug("setting property ["+getName()+"] to value ["+value+"]");
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
	public String send(Session session, Destination dest, javax.jms.Message message, boolean ignoreInvalidDestinationException) throws NamingException, JMSException {
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
		Message message;
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
			message = ((IMessageWrapper)rawMessage).getMessage();
		} else if (rawMessage instanceof TextMessage) {
			message = new Message(((TextMessage)rawMessage).getText());
		} else {
			message = new Message((String)rawMessage);
		}
		if (!soap) {
			return message;
		}
		message.preserve();
		Message messageText=extractMessageBody(message, context, soapWrapper);
		if (StringUtils.isNotEmpty(soapHeaderSessionKey)) {
			String soapHeader=soapWrapper.getHeader(message);
			context.put(soapHeaderSessionKey,soapHeader);
		}
		return messageText;
	}

	protected Message extractMessageBody(Message message, Map<String,Object> context, SoapWrapper soapWrapper) throws SAXException, TransformerException, IOException {
		return soapWrapper.getBody(message);
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
        sb.append("[ackMode=" + ackMode + "]");
        sb.append("[persistent=" + getPersistent() + "]");
        sb.append("[transacted=" + transacted + "]");
        return sb.toString();
    }

	/**
	 * The name of the destination, this may be a <code>queue</code> or <code>topic</code> name.
	 */
	@IbisDoc({"1", "Name of the JMS destination (queue or topic) to use", ""})
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
	@IbisDoc({"2", "Either <code>queue</code> or <code>topic</code>", "<code>queue</code>"})
	public void setDestinationType(String destinationType) {
		this.destinationType = Misc.parse(DestinationType.class, "destinationType", destinationType);
		useTopicFunctions = this.destinationType==DestinationType.TOPIC;
	}
	public void setDestinationTypeEnum(DestinationType destinationType) {
		this.destinationType=destinationType;
	}
	public DestinationType getDestinationTypeEnum() {
		return destinationType;
	}

	public boolean isUseTopicFunctions() {
		return useTopicFunctions;
	}

	/**
	 * Sets the JMS-acknowledge mode. This controls for non transacted listeners the way messages are acknowledged.
	 * See the jms-documentation.
	 */
	@Deprecated
	@ConfigurationWarning("please use attribute acknowledgeMode instead")
	public void setAckMode(int ackMode) {
		this.ackMode = Misc.parseFromField(AcknowledgeMode.class, "ackMode", ackMode, a -> a.getAcknowledgeMode());
	}

	public AcknowledgeMode getAckModeEnum() {
		return ackMode;
	}


	@IbisDoc({"3", "Acknowledge mode, can be one of ('auto' or 'auto_acknowledge'), ('dups' or 'dups_ok_acknowledge') or ('client' or 'client_acknowledge')", "auto_acknowledge",})
	public void setAcknowledgeMode(String acknowledgeMode) {
		try {
			ackMode = Misc.parseFromField(AcknowledgeMode.class, "acknowledgeMode", acknowledgeMode, a -> a.getShortName());
		} catch (IllegalArgumentException e1) {
			try {
				ackMode = Misc.parse(AcknowledgeMode.class, "acknowledgeMode", acknowledgeMode);
			} catch (IllegalArgumentException e2) {
				e1.addSuppressed(e2);
				ConfigurationWarnings.add(this, log, "invalid acknowledgemode:[" + acknowledgeMode + "] setting no acknowledge", e1);
				ackMode = AcknowledgeMode.NOT_SET;
			}
		}
	}

	/**
	 * Controls whether messages are processed persistently.
	 *
	 * When set <code>true</code>, the JMS provider ensures that messages aren't lost when the application might crash.
	 */
	@IbisDoc({"4", "Normally, if (lookupDestination=true AND destinationType=TOPIC) then the topic is created, instead of looked up. By setting persistent=true in that case, the topic will be looked up.", "false"})
	@Deprecated
	public void setPersistent(boolean value) {
		persistent = value;
	}
	public boolean getPersistent() {
		return persistent;
	}

	@IbisDoc({"5", "SubscriberType, should <b>DURABLE</b> or <b>TRANSIENT</b>. Only applicable for topics ", "DURABLE"})
	public void setSubscriberType(String subscriberType) {
		this.subscriberType = Misc.parse(SubscriberType.class, "subscriberType", subscriberType);
	}
	public SubscriberType getSubscriberTypeEnum() {
		return subscriberType;
	}

	/**
	 * The JNDI-name of the connection factory to use to connect to a <i>queue</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	@IbisDoc({"6", "JNDI-name of the queueConnectionFactory, used when <code>destinationType<code>=</code>QUEUE</code>", ""})
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
	@IbisDoc({"7", "JNDI-name of the topicConnectionFactory, used when <code>destinationType<code>=</code>TOPIC</code>", ""})
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

	/**
	 * Controls whether messages are send under transaction control.
	 * If set <code>true</code>, messages are committed or rolled back under control of an XA-transaction.
	 */
	@IbisDoc({"8", "", "false"})
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}
	@Override
	public boolean isTransacted() {
		return transacted;
	}

	@IbisDoc({"9", "Transform the value of the correlationid to a hexadecimal value if it starts with id: (preserving the id: part). "+ 
			"Useful when sending messages to MQ which expects this value to be in hexadecimal format when it starts with id:, otherwise generating the error: MQJMS1044: String is not a valid hexadecimal number", "false"})
	public void setCorrelationIdToHex(boolean correlationIdToHex) {
		this.correlationIdToHex = correlationIdToHex;
	}
	public boolean isCorrelationIdToHex() {
		return correlationIdToHex;
	}


	@IbisDoc({"10", "Prefix to check before executing correlationIdToHex. If empty (and correlationIdToHex equals true) all correlationid's are transformed, this is useful in case you want the entire correlationId to be transformed (for example when the receiving party doesn't allow characters like a colon to be present in the correlationId).", "id:"})
	public void setCorrelationIdToHexPrefix(String correlationIdToHexPrefix) {
		this.correlationIdToHexPrefix = correlationIdToHexPrefix;
	}


	@IbisDoc({"11", "The time (in milliseconds) it takes for the message to expire. If the message is not consumed before, it will be lost. Mmake sure to set it to a positive value for request/repy type of messages.", "0 (unlimited)"})
	public void setMessageTimeToLive(long ttl){
		this.messageTimeToLive=ttl;
	}
	public long getMessageTimeToLive(){
		return this.messageTimeToLive;
	}

	@IbisDoc({"12", "If set (>=0) and the length of the correlationId exceeds this maximum length, the correlationId is trimmed from the left side of a string to this maximum length", "-1"})
	public void setCorrelationIdMaxLength(int i) {
		correlationIdMaxLength = i;
	}
	public int getCorrelationIdMaxLength() {
		return correlationIdMaxLength;
	}


	@IbisDoc({"13", "If set, the value of this attribute is used as a selector to filter messages.", "0 (unlimited)"})
	public void setMessageSelector(String newMessageSelector) {
		this.messageSelector=newMessageSelector;
	}
	public String getMessageSelector() {
		return messageSelector;
	}

	@IbisDoc({"14", "Alias used to obtain credentials for authentication to JMS server", ""})
	public void setAuthAlias(String string) {
		authAlias = string;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({"15", "If set <code>false</code>, the destinationName is used directly instead of performing a JNDI lookup", "true"})
	public void setLookupDestination(boolean b) {
		lookupDestination = b;
	}
	public boolean isLookupDestination() {
		return lookupDestination;
	}
}