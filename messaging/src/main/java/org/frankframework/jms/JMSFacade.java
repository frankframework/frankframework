/*
   Copyright 2013, 2015, 2018 Nationale-Nederlanden, 2020-2025 WeAreFrank!

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
package org.frankframework.jms;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import jakarta.annotation.Nonnull;
import jakarta.jms.BytesMessage;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.Destination;
import jakarta.jms.InvalidDestinationException;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.util.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.xml.sax.SAXException;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.FrankElement;
import org.frankframework.core.HasPhysicalDestination;
import org.frankframework.core.IXAEnabled;
import org.frankframework.core.IbisException;
import org.frankframework.core.IbisTransaction;
import org.frankframework.core.NameAware;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.doc.DocumentedEnum;
import org.frankframework.doc.EnumLabel;
import org.frankframework.jndi.JndiBase;
import org.frankframework.lifecycle.ConfigurableLifecycle;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.AppConstants;
import org.frankframework.util.ClassUtils;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.EnumUtils;
import org.frankframework.util.LogUtil;
import org.frankframework.util.XmlException;

/**
 * Provides functions for jms connections, queues and topics and acts as a facade
 * to hide for clients whether a <code>Queue</code> or <code>Topic</code> is used.
 * <br/>
 * The <code>destinationType</code> field specifies which type should be used.<br/>
 * This class sends messages with JMS.
 *
 * @author 	Gerrit van Brakel
 */
public class JMSFacade extends JndiBase implements ConfigurableLifecycle, FrankElement, NameAware, HasPhysicalDestination, IXAEnabled {
	protected Logger log = LogUtil.getLogger(this);
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;

	public static final String JMS_MESSAGECLASS_KEY = "jms.messageClass.default";

	private final @Getter String domain = "JMS";
	private final boolean createDestination = AppConstants.getInstance().getBoolean("jms.createDestination", true);
	private final MessageClass messageClassDefault = AppConstants.getInstance().getOrDefault(JMS_MESSAGECLASS_KEY, MessageClass.AUTO);
	private @Getter MessageClass messageClass = messageClassDefault;

	private boolean started = false;
	private @Getter boolean transacted = false;
	private @Getter SubscriberType subscriberType = SubscriberType.DURABLE;

	private @Getter AcknowledgeMode acknowledgeMode = AcknowledgeMode.AUTO_ACKNOWLEDGE;
	private @Getter boolean persistent;
	private @Getter long messageTimeToLive = 0;
	private @Getter String destinationName;
	private @Getter boolean useTopicFunctions = false;
	private @Getter String authAlias;
	private @Getter boolean lookupDestination = AppConstants.getInstance().getBoolean("jms.lookupDestination", true);

	private @Getter DestinationType destinationType = DestinationType.QUEUE; // QUEUE or TOPIC

	protected MessagingSource messagingSource;
	private final Map<String,Destination> destinations = new ConcurrentHashMap<>();

	private @Setter @Getter IConnectionFactoryFactory connectionFactoryFactory = null;
	private @Setter @Getter ConnectionFactory connectionFactory;

	private @Setter @Getter Map<String, String> proxiedDestinationNames;

	private @Getter String jndiContextPrefix;

	// ---------------------------------------------------------------------
	// Queue fields
	// ---------------------------------------------------------------------
	private @Getter String queueConnectionFactoryName;
	// ---------------------------------------------------------------------
	// Topic fields
	// ---------------------------------------------------------------------
	private @Getter String topicConnectionFactoryName;

	// the MessageSelector will provide filter functionality, as specified jakarta.jms.Message.
	private @Getter String messageSelector = null;

	private @Getter boolean correlationIdToHex = false;
	private @Getter String correlationIdToHexPrefix = "ID:";
	private @Getter int correlationIdMaxLength = -1;
	private @Getter @Setter PlatformTransactionManager txManager;
	private boolean skipCheckForTransactionManagerValidity = false;

	/**
	 * The JMS {@link jakarta.jms.Message} class for the outgoing message.
	 * Currently supported are {@link MessageClass#TEXT} for JMS {@link TextMessage},
	 * {@link MessageClass#BYTES} for JMS {@link BytesMessage}, or {@link MessageClass#AUTO} for auto-determination
	 * based on whether the input {@link Message} is binary or character.
	 * <p>
	 * Defaults to {@link MessageClass#AUTO}, unless the default is overridden in {@link AppConstants} with property {@code jms.messageClass.default}
	 * </p>
	 */
	public void setMessageClass(MessageClass messageClass) {
		this.messageClass = messageClass;
	}

	public enum AcknowledgeMode implements DocumentedEnum {
		@EnumLabel("none") NOT_SET(0),

		/** auto or auto_acknowledge: Specifies that the session is to automatically acknowledge consumer receipt of
		  * messages when message processing is complete. */
		@EnumLabel("auto") AUTO_ACKNOWLEDGE(Session.AUTO_ACKNOWLEDGE),

		/** client or client_acknowledge: Specifies that the consumer is to acknowledge all messages delivered in this session.
		 * The Frank application will acknowledge all messages processed correctly. The skipping of the acknowledgement of messages
		 * processed in error will cause them to be redelivered, thus providing an automatic retry. */
		@EnumLabel("client") CLIENT_ACKNOWLEDGE(Session.CLIENT_ACKNOWLEDGE),

		/** dups or dups_ok_acknowledge: Specifies that the session is to "lazily" acknowledge the
		  * delivery of messages to the consumer. "Lazy" means that the consumer can delay the acknowledgment
		  * of messages to the server until a convenient time; meanwhile the server might redeliver messages.
		  * This mode reduces the session overhead. If JMS fails, the consumer may receive duplicate messages. */
		@EnumLabel("dups") DUPS_OK_ACKNOWLEDGE(Session.DUPS_OK_ACKNOWLEDGE);
		private final @Getter int acknowledgeMode;

		AcknowledgeMode(int acknowledgeMode) {
			this.acknowledgeMode = acknowledgeMode;
		}
	}

	public enum DeliveryMode {
		NOT_SET(0),
		PERSISTENT(jakarta.jms.DeliveryMode.PERSISTENT),
		NON_PERSISTENT(jakarta.jms.DeliveryMode.NON_PERSISTENT);

		private final @Getter int deliveryMode;

		DeliveryMode(int deliveryMode) {
			this.deliveryMode = deliveryMode;
		}

		public static DeliveryMode parse(int deliveryMode) {
			return EnumUtils.parseFromField(DeliveryMode.class, "DeliveryMode", deliveryMode, DeliveryMode::getDeliveryMode);
		}
	}

	public enum SubscriberType {
		DURABLE,
		TRANSIENT
	}

	public enum DestinationType {
		QUEUE,
		TOPIC
	}

	/**
	 * The JMS {@link jakarta.jms.Message} class for the outgoing message.
	 * Currently supported are TEXT for JMS {@link TextMessage},
	 * BYTES for JMS {@link BytesMessage}, or AUTO for auto-determination
	 * based on whether the input {@link Message} is binary or character.
	 */
	public enum MessageClass {
		/**
		 * Automatically determine the type of the outgoing {@link jakarta.jms.Message} based
		 * on the value of {@link Message#isBinary()}.
		 */
		AUTO,
		/**
		 * Create the outgoing message as {@link TextMessage}.
		 */
		TEXT,
		/**
		 * Create the outgoing message as {@link BytesMessage}.
		 */
		BYTES
	}

	protected String getLogPrefix() {
		return "["+getName()+"] ";
	}

	@Override
	public void configure() throws ConfigurationException {
		if(connectionFactoryFactory == null) {
			throw new ConfigurationException("no connectionFactoryFactory set");
		}
		try {
			connectionFactory = connectionFactoryFactory.getConnectionFactory(getConnectionFactoryName(), getJndiEnv());
			if("com.amazon.sqs.javamessaging.SQSConnectionFactory".equals(connectionFactory.getClass().getCanonicalName()) && StringUtils.isNotBlank(getMessageSelector())) {
				throw new ConfigurationException("Amazon SQS does not support MessageSelectors");
			}
		} catch (NamingException | JmsException e) {
			throw new ConfigurationException("unable to use ConnectionFactory", e);
		}
	}

	public String getConnectionFactoryName() throws JmsException {
		String result = useTopicFunctions ? getTopicConnectionFactoryName() : getQueueConnectionFactoryName();
		if (StringUtils.isEmpty(result)) {
			throw new JmsException(getLogPrefix()+"no "+(useTopicFunctions ?"topic":"queue")+"ConnectionFactoryName specified");
		}
		return result;
	}

	protected JmsMessagingSource getJmsMessagingSource() throws JmsException {
		return (JmsMessagingSource)getMessagingSource();
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
					JmsMessagingSourceFactory messagingSourceFactory = new JmsMessagingSourceFactory(this);
					try {
						String connectionFactoryName = getConnectionFactoryName();
						log.debug("creating MessagingSource");
						messagingSource = messagingSourceFactory.getMessagingSource(connectionFactoryName, getAuthAlias(), createDestination);
					} catch (IbisException e) {
						if (e instanceof JmsException exception) {
							throw exception;
						}
						throw new JmsException(e);
					}
				}
			}
		}
		return messagingSource;
	}

	// eol, required for the JmsMessagingSource
	@Autowired
	public void setJndiContextPrefix(String jndiContextPrefix) {
		this.jndiContextPrefix = jndiContextPrefix;
	}

	/**
	 * Returns a session on the connection for a topic or a queue
	 */
	protected Session createSession() throws JmsException {
		try {
			return getMessagingSource().createSession(false, getAcknowledgeMode().getAcknowledgeMode());
		} catch (JmsException e) {
			throw e;
		} catch (Exception e) {
			throw new JmsException(e);
		}
	}

	protected void closeSession(Session session) {
		try {
			getMessagingSource().releaseSession(session);
		} catch (Exception e) {
			log.warn("Exception releasing session", e);
		}
	}

	/**
	 * Obtains a connection and a serviceQueue.
	 */
	@Override
	public void start() {
		try {
			getMessagingSource(); // obtain and cache connection, then start it.
			if (StringUtils.isNotEmpty(getDestinationName())) {
				getDestination();
			}
		} catch (Exception e) {
			stop();
			throw new LifecycleException(e);
		}

		started = true;
	}

	/**
	 * Releases references to serviceQueue and connection.
	 */
	@Override
	public void stop() {
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
			super.stop();
		} finally {
			// make sure all objects are reset, to be able to restart after IFSA parameters have changed (e.g. at iterative installation time)
			destinations.clear();
			messagingSource = null;

			started = false;
		}
	}

	@Override
	public boolean isRunning() {
		return started;
	}

	@Nonnull
	public jakarta.jms.Message createMessage(Session session, String correlationID, Message message) throws JMSException, IOException {
		return createMessage(session, correlationID, message, messageClassDefault);
	}

	@Nonnull
	public jakarta.jms.Message createMessage(Session session, String correlationID, Message message, MessageClass messageClass) throws JMSException, IOException {
		switch (messageClass) {
			case TEXT:
				return createTextMessage(session, correlationID, message);
			case BYTES:
				return createBytesMessage(session, correlationID, message);
			case AUTO:
				return message.isBinary() ? createBytesMessage(session, correlationID, message) : createTextMessage(session, correlationID, message);
			default:
				throw new IllegalArgumentException("Unsupported messageClass value: [" + messageClass + "]");
		}
	}

	@Nonnull
	protected jakarta.jms.Message createBytesMessage(final Session session, final String correlationID, final Message message) throws JMSException, IOException {
		BytesMessage bytesMessage = session.createBytesMessage();
		setMessageCorrelationID(bytesMessage, correlationID);
		bytesMessage.writeBytes(message.asByteArray());
		return bytesMessage;
	}

	@Nonnull
	protected TextMessage createTextMessage(final Session session, final String correlationID, final Message message) throws JMSException, IOException {
		TextMessage textMessage = session.createTextMessage();
		setMessageCorrelationID(textMessage, correlationID);
		textMessage.setText(message.asString());
		return textMessage;
	}

	public void setMessageCorrelationID(jakarta.jms.Message message, String correlationID)
			throws JMSException {
		if (null == correlationID) {
			return;
		}
		if (correlationIdMaxLength >= 0) {
			int cidlen;
			if (correlationID.startsWith(correlationIdToHexPrefix)) {
				cidlen = correlationID.length() - correlationIdToHexPrefix.length();
			} else {
				cidlen = correlationID.length();
			}
			if (cidlen > correlationIdMaxLength) {
				correlationID = correlationIdToHexPrefix + correlationID.substring(correlationID.length() - correlationIdMaxLength);
				log.debug("correlationId shortened to [{}]", correlationID);
			}
		}
		if (correlationIdToHex && correlationID.startsWith(correlationIdToHexPrefix)) {
			StringBuilder hexCorrelationID = new StringBuilder(correlationIdToHexPrefix);
			int i;
			for (i = correlationIdToHexPrefix.length(); i < correlationID.length(); i++) {
				int c = correlationID.charAt(i);
				hexCorrelationID.append(Integer.toHexString(c));
			}
			correlationID = hexCorrelationID.toString();
			log.debug("correlationId changed, based on hexidecimal values, to [{}]", correlationID);
		}
		message.setJMSCorrelationID(correlationID);
	}

	public Destination getDestination() throws JmsException {
		if (StringUtils.isEmpty(getDestinationName())) {
			throw new JmsException("no (default) destinationName specified");
		}
		return getDestination(getDestinationName());
	}

	public Destination getDestination(String destinationName) {
		return destinations.computeIfAbsent(destinationName, this::computeDestination);
	}

	@SneakyThrows({JMSException.class, JmsException.class, NamingException.class})
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
	public MessageConsumer getMessageConsumerForCorrelationId(Session session, Destination destination, String correlationId) throws JMSException {
		if (correlationId==null) {
			return getMessageConsumer(session, destination, null);
		}
		return getMessageConsumer(session, destination, "JMSCorrelationID='" + correlationId + "'");
	}

	/**
	 * Create a MessageConsumer. In this overloaded function the selector is taken into account.
	 * This ensures that listeners (or other extensions of this class) do not influence how the selector
	 * is used: when a correlationID should be in the filter the  <code>getMessageConsumerForCorrelationId</code>
	 * should be used, otherwise the <code>getMessageConsumer</code> function which has no attribute for
	 * <code>selector</code>. When a MessageSelector is set, it will be used when no correlation id is required.
	 * @param session the Session
	 * @param destination the Destination
	 * @param selector the MessageSelector
	 * @return MessageConsumer
	 */
	public MessageConsumer getMessageConsumer(Session session, Destination destination, String selector) throws JMSException {
		if (useTopicFunctions) {
			return getTopicSubscriber(session, (Topic)destination, selector);
		}
		return session.createConsumer(destination, selector);
	}
	/**
	 * Create a MessageConsumer, on a specific session and for a specific destination.
	 * This functions hides wether we work via Topics or Queues and whether a messageSelector is set.
	 * @param session the Session
	 * @param destination the Destination
	 * @return the MessageConsumer
	 */
	public MessageConsumer getMessageConsumer(Session session, Destination destination) throws JMSException {
		return getMessageConsumer(session, destination, getMessageSelector());
	}

	public MessageProducer getMessageProducer(Session session, Destination destination) throws JMSException {
		MessageProducer mp = session.createProducer(destination);
		if (getMessageTimeToLive() > 0)
			mp.setTimeToLive(getMessageTimeToLive());
		return mp;
	}

	public String getPhysicalDestinationShortName() {
		try {
			return getPhysicalDestinationShortName(false);
		} catch (JmsException e) {
			log.warn("[{}] got exception in getPhysicalDestinationShortName", getName(), e);
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
			log.warn("[{}] got exception in getPhysicalDestinationShortName", getName(), e);
		}
		return result;
	}

	@Override
	public String getPhysicalDestinationName() {
		StringBuilder builder = new StringBuilder(getDestinationType().toString());
		builder.append("(").append(getDestinationName()).append(") [").append(getPhysicalDestinationShortName()).append("]");
		if (StringUtils.isNotEmpty(getMessageSelector())) {
			builder.append(" selector [").append(getMessageSelector()).append("]");
		}

		builder.append(" on (");
		builder.append(destinationType == DestinationType.QUEUE ? getQueueConnectionFactoryName() : getTopicConnectionFactoryName());
		builder.append(")");

		return builder.toString();
	}

	private MessageConsumer getTopicSubscriber(Session session, Topic topic, String selector) throws JMSException {
		MessageConsumer messageConsumer;
		switch (subscriberType) {
		case DURABLE:
			messageConsumer = session.createDurableSubscriber(topic, destinationName, selector, false);
			if (log.isDebugEnabled()) log.debug("[{}] got durable subscriber for topic [{}] with selector [{}]", getName(), destinationName, selector);
			break;
		case TRANSIENT:
			messageConsumer = session.createConsumer(topic, selector, false);
			if (log.isDebugEnabled()) log.debug("[{}] got transient subscriber for topic [{}] with selector [{}]", getName(), destinationName, selector);
			break;
		default:
			throw new IllegalStateException("Unexpected subscriberType ["+subscriberType+"]");
		}
		return messageConsumer;
	}

	public String send(Session session, Destination dest, String correlationId, Message message, String messageType, long timeToLive, int deliveryMode, int priority, boolean ignoreInvalidDestinationException, Map<String, Object> properties) throws JMSException, SenderException, IOException {
		jakarta.jms.Message msg = createMessage(session, correlationId, message, messageClass);
		try (MessageProducer mp = session.createProducer(dest)) {
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
				for (Map.Entry<String, Object> entry: properties.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();
					if (value instanceof Message message1) {
						value = message1.asString();
					}
					msg.setObjectProperty(key, value);
				}
			}

			return send(mp, msg, ignoreInvalidDestinationException);
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue [{}] doesn't exist", dest);
				return null;
			}
			throw e;
		}
	}

	/**
	 * Send a message
	 * @param messageProducer
	 * @param message
	 * @return messageID of sent message
	 */
	public String send(MessageProducer messageProducer, jakarta.jms.Message message) throws JMSException {
		return send(messageProducer, message, false);
	}
	public String send(MessageProducer messageProducer, jakarta.jms.Message message, boolean ignoreInvalidDestinationException) throws JMSException {
		logMessageDetails(message, messageProducer);
		try {
			messageProducer.send(message);
			return message.getJMSMessageID();
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue [{}] doesn't exist", messageProducer.getDestination());
				return null;
			}
			throw e;
		}
	}

	@SuppressWarnings("java:S3457") // Ignore {} usage inside logging
	protected void logMessageDetails(jakarta.jms.Message message, MessageProducer messageProducer) throws JMSException {
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix() + "sender on [" + getDestinationName()
					+ "] JMSDeliveryMode=[" + message.getJMSDeliveryMode()
					+ "] JMSMessageID=[" + message.getJMSMessageID()
					+ "] JMSCorrelationID=[" + message.getJMSCorrelationID()
					+ "] JMSTimestamp=[" + DateFormatUtils.format(message.getJMSTimestamp())
					+ "] JMSExpiration=[" + message.getJMSExpiration()
					+ "] JMSPriority=[" + message.getJMSPriority()
					+ "] JMSType=[" + message.getJMSType()
					+ "] JMSReplyTo=[" + message.getJMSReplyTo()
					+ "] Message=[" + message
					+ "]");
		} else if (log.isInfoEnabled()){
			log.info("[" + getName()
					+ (messageProducer != null ? "] message destination [" + messageProducer.getDestination() : "")
					+ "] JMSDeliveryMode=[" + message.getJMSDeliveryMode()
					+ "] JMSMessageID=[" + message.getJMSMessageID()
					+ "] JMSCorrelationID=[" + message.getJMSCorrelationID()
					+ "] JMSReplyTo=[" + message.getJMSReplyTo() + "]");
		}
	}

	/**
	 * Send a message
	 * @param session
	 * @param dest destination
	 * @param message
	 * @return message ID of the sent message
	 */
	public String send(Session session, Destination dest, jakarta.jms.Message message)
		throws JMSException {
		return send(session, dest, message, false);
	}

	public String send(Session session, Destination dest, jakarta.jms.Message message, boolean ignoreInvalidDestinationException) throws JMSException {
		try (MessageProducer mp = session.createProducer(dest)) {
			logMessageDetails(message, mp);
			mp.send(message);
			return message.getJMSMessageID();
		} catch (InvalidDestinationException e) {
			if (ignoreInvalidDestinationException) {
				log.warn("queue [{}] doesn't exist", dest);
				return null;
			}
			throw e;
		}
	}

	protected String sendByQueue(QueueSession session, Queue destination, jakarta.jms.Message message) throws JMSException {
		try (QueueSender tqs = session.createSender(destination)) {
			tqs.send(message);
			return message.getJMSMessageID();
		}
	}

	protected String sendByTopic(TopicSession session, Topic destination, jakarta.jms.Message message) throws JMSException {
		try (TopicPublisher tps = session.createPublisher(destination)) {
			tps.publish(message);
			return message.getJMSMessageID();
		}
	}

	public boolean isSessionsArePooled() {
		try {
			return isTransacted() || getMessagingSource().sessionsArePooled();
		} catch (JmsException e) {
			log.error("could not get session",e);
			return false;
		}
	}

	public MessageContext getContext(jakarta.jms.Message message) throws JMSException {
		MessageContext result = new MessageContext();
		result.withName(message.getJMSMessageID());
		result.withModificationTime(message.getJMSTimestamp());
		@SuppressWarnings("unchecked")
		Enumeration<String> names = message.getPropertyNames();
		while (names.hasMoreElements()) {
			String name = names.nextElement();
			if (message.getObjectProperty(name) instanceof Serializable value) {
				result.put(name, value);
			}
		}
		return result;
	}

	/**
	 * Extracts string from message obtained from getRawMessage(Map). May also extract
	 * other parameters from the message and put those in the threadContext.
	 * <br/><br/>
	 * Supports only
	 * {@link jakarta.jms.TextMessage}s and {@link jakarta.jms.BytesMessage}.<br/><br/>
	 */
	public Message extractMessage(jakarta.jms.Message jmsMessage, Map<String,Object> context, boolean soap, String soapHeaderSessionKey, SoapWrapper soapWrapper) throws JMSException, SAXException, TransformerException, IOException, XmlException {
		Message message;

		if (jmsMessage instanceof TextMessage textMessage) {
			message = new Message(textMessage.getText(), getContext(jmsMessage));
		} else if (jmsMessage instanceof BytesMessage bytesMsg) {
			InputStream input = new BytesMessageInputStream(bytesMsg);
			message = new Message(new BufferedInputStream(input), getContext(jmsMessage));
		} else if (jmsMessage == null) {
			message = Message.nullMessage();
		} else {
			message = Message.asMessage(jmsMessage);
		}
		if (!soap) {
			return message;
		}

		// Only for SOAP messages we do a bit of extra work
		Message messageText = extractMessageBody(message, context, soapWrapper);
		if (StringUtils.isNotEmpty(soapHeaderSessionKey)) {
			String soapHeader;
			if (context instanceof PipeLineSession session) {
				soapHeader = soapWrapper.getHeader(message, session);
			} else {
				soapHeader = soapWrapper.getHeader(message, null);
			}

			context.put(soapHeaderSessionKey, soapHeader);
		}
		return messageText;
	}

	protected Message extractMessageBody(Message message, Map<String, Object> context, SoapWrapper soapWrapper) throws SAXException, TransformerException, IOException, XmlException {
		if (context instanceof PipeLineSession session) {
			return soapWrapper.getBody(message, false, session, null);
		}
		return soapWrapper.getBody(message, false, null, null);
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
			sb.append("[topicName=").append(destinationName).append("]");
			sb.append("[topicConnectionFactoryName=").append(topicConnectionFactoryName).append("]");
		} else {
			sb.append("[queueName=").append(destinationName).append("]");
			sb.append("[queueConnectionFactoryName=").append(queueConnectionFactoryName).append("]");
		}
		sb.append("[ackMode=").append(getAcknowledgeMode()).append("]");
		sb.append("[persistent=").append(isPersistent()).append("]");
		sb.append("[transacted=").append(transacted).append("]");
		return sb.toString();
	}

	/** Name of the JMS destination (queue or topic) to use */
	public void setDestinationName(String destinationName) {
		this.destinationName = destinationName;
	}

	/**
	 * Type of the messageing destination.
	 * This function also sets the <code>useTopicFunctions</code> field,
	 * that controls whether Topic functions are used or Queue functions.
	 * @ff.default QUEUE
	 */
	public void setDestinationType(DestinationType destinationType) {
		this.destinationType=destinationType;
		useTopicFunctions = this.destinationType==DestinationType.TOPIC;
	}

	/**
	 * If not transacted, the way the application informs the JMS provider that it has successfully received a message.
	 * @ff.default auto
	 */
	public void setAcknowledgeMode(AcknowledgeMode acknowledgeMode) {
		this.acknowledgeMode = acknowledgeMode;
	}

	/**
	 * Controls whether messages are processed persistently.
	 *
	 * When set <code>true</code>, the JMS provider ensures that messages aren't lost when the application might crash.
	 */
	@Deprecated(forRemoval = true, since = "7.6.0")
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
	 * Used when {@link #setDestinationType destinationType} = {@link DestinationType#QUEUE QUEUE}.
	 * The JNDI-name of the queueConnectionFactory to use to connect to a <code>queue</code> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	public void setQueueConnectionFactoryName(String name) {
		queueConnectionFactoryName=name;
	}

	/**
	 * Used when {@link #setDestinationType destinationType} = {@link DestinationType#TOPIC TOPIC}.
	 * The JNDI-name of the connection factory to use to connect to a <i>topic</i> if {@link #isTransacted()} returns <code>false</code>.
	 * The corresponding connection factory should be configured not to support XA transactions.
	 */
	public void setTopicConnectionFactoryName(String topicConnectionFactoryName) {
		this.topicConnectionFactoryName = topicConnectionFactoryName;
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

	/**
	 * The name of this FrankElement
	 */
	@Override
	public void setName(String name) {
		this.name=name;
	}

}
