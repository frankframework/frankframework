/*
   Copyright 2013, 2015, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.jms.BytesMessage;
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

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Supplier;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import nl.nn.adapterframework.core.IbisTransaction;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.DocumentedEnum;
import nl.nn.adapterframework.doc.EnumLabel;
import nl.nn.adapterframework.jndi.JndiBase;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageContext;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.ClassUtils;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.EnumUtils;


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

	private final @Getter(onMethod = @__(@Override)) String domain = "JMS";
	private boolean createDestination = AppConstants.getInstance().getBoolean("jms.createDestination", false);
	private boolean useJms102 = AppConstants.getInstance().getBoolean("jms.useJms102", false);

	private @Getter boolean transacted = false;
	private @Getter boolean jmsTransacted = false;
	private @Getter SubscriberType subscriberType = SubscriberType.DURABLE;

	private AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO_ACKNOWLEDGE;
	private @Getter boolean persistent;
	private @Getter long messageTimeToLive = 0;
	private @Getter String destinationName;
	private @Getter boolean useTopicFunctions = false;
	private @Getter String authAlias;
	private @Getter boolean lookupDestination = AppConstants.getInstance().getBoolean("jms.lookupDestination", true);

	private @Getter DestinationType destinationType = DestinationType.QUEUE; // QUEUE or TOPIC

	protected MessagingSource messagingSource;
	private Map<String,Destination> destinations = new ConcurrentHashMap<>();

	private @Setter @Getter IConnectionFactoryFactory connectionFactoryFactory = null;
	private @Setter @Getter Map<String, String> proxiedDestinationNames;

	// ---------------------------------------------------------------------
	// Queue fields
	// ---------------------------------------------------------------------
	private @Getter String queueConnectionFactoryName;
	// ---------------------------------------------------------------------
	// Topic fields
	// ---------------------------------------------------------------------
	private @Getter String topicConnectionFactoryName;

	// the MessageSelector will provide filter functionality, as specified
	// javax.jms.Message.
	private @Getter String messageSelector = null;

	private @Getter boolean correlationIdToHex = false;
	private @Getter String correlationIdToHexPrefix = "ID:";
	private @Getter int correlationIdMaxLength = -1;
	private @Getter @Setter PlatformTransactionManager txManager;
	private boolean skipCheckForTransactionManagerValidity=false;

	public enum AcknowledgeMode implements DocumentedEnum {
		@EnumLabel("none") NOT_SET(0),

		/** auto or auto_acknowledge: Specifies that the session is to automatically acknowledge consumer receipt of
		  * messages when message processing is complete. */
		@EnumLabel("auto") AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),

		/** client or client_acknowledge: Specifies that the consumer is to acknowledge all messages delivered in this session. */
		@EnumLabel("client") CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE),

		/** dups or dups_ok_acknowledge: Specifies that the session is to "lazily" acknowledge the
		  * delivery of messages to the consumer. "Lazy" means that the consumer can delay the acknowledgment
		  * of messages to the server until a convenient time; meanwhile the server might redeliver messages.
		  * This mode reduces the session overhead. If JMS fails, the consumer may receive duplicate messages. */
		@EnumLabel("dups") DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE);
		private @Getter int acknowledgeMode;

		private AcknowledgeMode(int acknowledgeMode) {
			this.acknowledgeMode = acknowledgeMode;
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
			return EnumUtils.parseFromField(DeliveryMode.class, "DeliveryMode", deliveryMode, d -> d.getDeliveryMode());
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
			return getMessagingSource().createSession(isJmsTransacted(), getAcknowledgeModeEnum().getAcknowledgeMode());
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
					log.trace("Closing messaging source - will synchronize (lock) on {}", messagingSource::toString);
					messagingSource.close();
				} catch (IbisException e) {
					log.warn("{} caught exception closing messaging source", (Supplier<?>) this::getLogPrefix, e);
				} finally {
					log.trace("Messaging source closed - lock on {} released", messagingSource::toString);
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
				}
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
			if (!useTopicFunctions || isPersistent()) {
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
		if (correlationId==null) {
			return getMessageConsumer(session, destination, null);
		}
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
			}
			return getTopicSubscriber(session, (Topic)destination, selector);
		}
		if (useJms102()) {
			return getQueueReceiver((QueueSession)session, (Queue)destination, selector);
		}
		return session.createConsumer(destination, selector);
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
		if (StringUtils.isEmpty(getDestinationName()) && !throwException) {
			return null;
		}
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
			}
			log.warn("["+getName()+"] got exception in getPhysicalDestinationShortName", e);
		}
		return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder builder = new StringBuilder(getDestinationType().toString());
		builder.append("("+getDestinationName()+") ["+getPhysicalDestinationShortName()+"]");
		if (StringUtils.isNotEmpty(getMessageSelector())) {
			builder.append(" selector ["+getMessageSelector()+"]");
		}

		builder.append(" on (");
		builder.append(destinationType == DestinationType.QUEUE ? getQueueConnectionFactoryName() : getTopicConnectionFactoryName());
		builder.append(")");

		return builder.toString();
	}

	/**
	 * Gets a queueReceiver value
	 * @see QueueReceiver
	 */
	private QueueReceiver getQueueReceiver(QueueSession session, Queue destination, String selector) throws JMSException {
		return session.createReceiver(destination, selector);
	}

	/**
	  * Gets the queueSender for a specific queue, not the one in <code>destination</code>
	  * @see QueueSender
	  * @return The queueReceiver value
	  */
	private QueueSender getQueueSender(QueueSession session, Queue destination) throws JMSException {
		return session.createSender(destination);
	}

	/**
	 * Gets a topicPublisher for a specified topic
	 */
	private TopicPublisher getTopicPublisher(TopicSession session, Topic topic) throws JMSException {
		return session.createPublisher(topic);
	}

	private TopicSubscriber getTopicSubscriber(TopicSession session, Topic topic, String selector) throws JMSException {
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

	private MessageConsumer getTopicSubscriber(Session session, Topic topic, String selector) throws JMSException {
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
			}
			throw e;
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
				if (value instanceof Message) {
					value = ((Message)value).asString();
				}
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
			}
			messageProducer.send(message);
			return message.getJMSMessageID();
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue ["+messageProducer.getDestination()+"] doesn't exist");
				return null;
			}
			throw e;
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
				}
				return sendByQueue((QueueSession)session, (Queue)dest, message);
			}
			MessageProducer mp = session.createProducer(dest);
			mp.send(message);
			mp.close();
			return message.getJMSMessageID();
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue ["+dest+"] doesn't exist");
				return null;
			}
			throw e;
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

	public MessageContext getContext(javax.jms.Message message) throws JMSException {
		MessageContext result = new MessageContext();
		result.withName(message.getJMSMessageID());
		result.withModificationTime(message.getJMSTimestamp());
		Enumeration<String> names=message.getPropertyNames();
		while(names.hasMoreElements()) {
			String name=names.nextElement();
			result.put(name,message.getObjectProperty(name));
		}
		return result;
	}

	/**
	 * Extracts string from message obtained from getRawMessage(Map). May also extract
	 * other parameters from the message and put those in the threadContext.
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
			message = new Message(((TextMessage)rawMessage).getText(),getContext((TextMessage)rawMessage));
		} else if (rawMessage instanceof BytesMessage) {
			BytesMessage bytesMsg = (BytesMessage)rawMessage;
			InputStream input = new InputStream() {

				@Override
				public int read() throws IOException {
					try {
						return bytesMsg.readByte();
					} catch (JMSException e) {
						throw new IOException("Cannot read JMS message", e);
					}
				}

				@Override
				public int read(byte[] b) throws IOException {
					try {
						return bytesMsg.readBytes(b);
					} catch (JMSException e) {
						throw new IOException("Cannot read JMS message", e);
					}
				}

				@Override
				public int read(byte[] b, int off, int len) throws IOException {
					try {
						byte[] readbuf = new byte[len];
						int result = bytesMsg.readBytes(readbuf);
						if (result>0) {
							System.arraycopy(readbuf, 0, b, off, result);
						}
						return result;
					} catch (JMSException e) {
						throw new IOException("Cannot read JMS message", e);
					}
				}
			};
			message = new Message(new BufferedInputStream(input),getContext((BytesMessage)rawMessage));
		} else if (rawMessage == null) {
			message = Message.nullMessage();
		} else {
			message = Message.asMessage(rawMessage);
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

	public void checkTransactionManagerValidity() {
		if (!skipCheckForTransactionManagerValidity && !IbisTransaction.isDistributedTransactionsSupported(txManager)) {
			if (TransactionSynchronizationManager.isSynchronizationActive()) {
				skipCheckForTransactionManagerValidity = true;
				ConfigurationWarnings.add(this, log, ClassUtils.nameOf(this)+" used in transaction, but no JTA transaction manager found. JMS will not participate in transaction!");
			}
		} else {
			skipCheckForTransactionManagerValidity = true;
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		if (useTopicFunctions) {
			sb.append("[topicName=" + destinationName + "]");
			sb.append("[topicConnectionFactoryName=" + topicConnectionFactoryName + "]");
		} else {
			sb.append("[queueName=" + destinationName + "]");
			sb.append("[queueConnectionFactoryName=" + queueConnectionFactoryName + "]");
		}
		// sb.append("[physicalDestinationName="+getPhysicalDestinationName()+"]");
		sb.append("[ackMode=" + getAcknowledgeModeEnum() + "]");
		sb.append("[persistent=" + isPersistent() + "]");
		sb.append("[transacted=" + transacted + "]");
		return sb.toString();
	}

	/**
	 * The name of the destination, this may be a <code>queue</code> or <code>topic</code> name.
	 */
	/** Name of the JMS destination (queue or topic) to use */
	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	/**
	 * should be <code>QUEUE</code> or <code>TOPIC</code><br/>
	 * This function also sets the <code>useTopicFunctions</code> field,
	 * that controls wether Topic functions are used or Queue functions.
	 */

	/**
	 * Type of the messageing destination
	 * @ff.default QUEUE
	 */
	public void setDestinationType(DestinationType destinationType) {
		this.destinationType=destinationType;
		useTopicFunctions = this.destinationType==DestinationType.TOPIC;
	}

	/**
	 * Sets the JMS-acknowledge mode. This controls for non transacted listeners the way messages are acknowledged.
	 * See the jms-documentation.
	 */
	@Deprecated
	@ConfigurationWarning("please use attribute acknowledgeMode instead")
	public void setAckMode(int ackMode) {
		this.acknowledgeMode = EnumUtils.parseFromField(AcknowledgeMode.class, "ackMode", ackMode, a -> a.getAcknowledgeMode());
	}


	/**
	 * If not transacted, the way the application informs the JMS provider that it has successfully received a message.
	 * @ff.default auto
	 */
	public void setAcknowledgeMode(String acknowledgeMode) {
		try {
			this.acknowledgeMode = EnumUtils.parse(AcknowledgeMode.class, acknowledgeMode, true);
		} catch (IllegalArgumentException e) {
			ConfigurationWarnings.add(this, log, "invalid acknowledgemode:[" + acknowledgeMode + "] setting no acknowledge", e);
			this.acknowledgeMode = AcknowledgeMode.NOT_SET;
		}
	}
	public AcknowledgeMode getAcknowledgeModeEnum() {
		return acknowledgeMode;
	}

	/**
	 * Controls whether messages are processed persistently.
	 *
	 * When set <code>true</code>, the JMS provider ensures that messages aren't lost when the application might crash.
	 */
	@Deprecated
	public void setPersistent(boolean value) {
		persistent = value;
	}

	/**
	 * Only applicable for topics
	 * @ff.default DURABLE
	 */
	public void setSubscriberType(SubscriberType subscriberType) {
		this.subscriberType = subscriberType;
	}

	/**
	 * Used when {@link #setDestinationType DestinationType} = {@link DestinationType#QUEUE QUEUE}.
	 * The JNDI-name of the queueConnectionFactory to use to connect to a <code>queue</code> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	public void setQueueConnectionFactoryName(String name) {
		queueConnectionFactoryName=name;
	}

	/**
	 * Used when {@link #setDestinationType DestinationType} = {@link DestinationType#TOPIC TOPIC}.
	 * The JNDI-name of the connection factory to use to connect to a <i>topic</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	public void setTopicConnectionFactoryName(String topicConnectionFactoryName) {
		this.topicConnectionFactoryName = topicConnectionFactoryName;
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

	/**
	 * Controls whether messages are send under transaction control.
	 * If set <code>true</code>, messages are committed or rolled back under control of an XA-transaction.
	 * @ff.default false
	 */
	public void setTransacted(boolean transacted) {
		this.transacted = transacted;
	}

	/**
	 * Transform the value of the correlationid to a hexadecimal value if it starts with id: (preserving the id: part).
	 * Useful when sending messages to MQ which expects this value to be in hexadecimal format when it starts with id:, otherwise generating the error: MQJMS1044: String is not a valid hexadecimal number
	 * @ff.default false
	 */
	public void setCorrelationIdToHex(boolean correlationIdToHex) {
		this.correlationIdToHex = correlationIdToHex;
	}


	/**
	 * Prefix to check before executing correlationIdToHex. If empty (and correlationIdToHex equals true) all correlationid's are transformed, this is useful in case you want the entire correlationId to be transformed (for example when the receiving party doesn't allow characters like a colon to be present in the correlationId).
	 * @ff.default id:
	 */
	public void setCorrelationIdToHexPrefix(String correlationIdToHexPrefix) {
		this.correlationIdToHexPrefix = correlationIdToHexPrefix;
	}


	/**
	 * The time <i>in milliseconds</i> it takes for the message to expire. If the message is not consumed before, it will be lost. Must be a positive value for request/reply type of messages, 0 disables the expiry timeout
	 * @ff.default 0
	 */
	public void setMessageTimeToLive(long ttl){
		this.messageTimeToLive=ttl;
	}

	/**
	 * If set (>=0) and the length of the correlationId exceeds this maximum length, the correlationId is trimmed from the left side of a string to this maximum length
	 * @ff.default -1
	 */
	public void setCorrelationIdMaxLength(int i) {
		correlationIdMaxLength = i;
	}


	/**
	 * If set, the value of this attribute is used as a selector to filter messages.
	 * @ff.default 0 (unlimited)
	 */
	public void setMessageSelector(String newMessageSelector) {
		this.messageSelector=newMessageSelector;
	}

	/** Alias used to obtain credentials for authentication to JMS server */
	public void setAuthAlias(String string) {
		authAlias = string;
	}

	/**
	 * If set <code>false</code>, the destinationName is used directly instead of performing a JNDI lookup
	 * @ff.default true
	 */
	public void setLookupDestination(boolean b) {
		lookupDestination = b;
	}
}
