/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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

import static org.frankframework.functional.FunctionalUtil.logValue;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import jakarta.annotation.Nonnull;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Session;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.configuration.SuppressKeys;
import org.frankframework.core.HasSender;
import org.frankframework.core.IListenerConnector;
import org.frankframework.core.IPullingListener;
import org.frankframework.core.IRedeliveringListener;
import org.frankframework.core.ISender;
import org.frankframework.core.IWithParameters;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.TimeoutException;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.parameters.IParameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.soap.SoapWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

/**
 * Common baseclass for Pulling and Pushing JMS Listeners.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public abstract class AbstractJmsListener extends JMSFacade implements HasSender, IWithParameters, IRedeliveringListener<jakarta.jms.Message> {

	private @Getter long timeout = 1000; // Same default value as Spring: https://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/jms/listener/AbstractPollingMessageListenerContainer.html#setReceiveTimeout(long)
	private @Getter boolean useReplyTo = true;
	private @Getter String replyDestinationName;
	private @Getter String replyMessageType = null;
	private @Getter long replyMessageTimeToLive = 0;
	private @Getter int replyPriority = -1;
	private @Getter DeliveryMode replyDeliveryMode = DeliveryMode.NON_PERSISTENT;
	private @Getter ISender sender;

	private @Getter Boolean forceMessageIdAsCorrelationId = null;

	private @Getter boolean soap = false;
	private @Getter String replyEncodingStyleURI = null;
	private @Getter String replyNamespaceURI = null;
	private @Getter String replySoapAction = null;
	private @Getter String soapHeaderSessionKey = "soapHeader";

	private SoapWrapper soapWrapper = null;

	private ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (isSoap()) {
			soapWrapper = SoapWrapper.getInstance();
		}
		ISender sender = getSender();
		if (sender != null) {
			sender.configure();
		}

		if (paramList != null) {
			paramList.configure();
		}
		if (forceMessageIdAsCorrelationId == null) {
			forceMessageIdAsCorrelationId = false;
		}
	}

	@Override
	public void start() {
		super.start();

		if (getSender() != null) {
			getSender().start();
		}
	}

	@Override
	public void stop() {
		super.stop();
		try {
			if (getSender() != null) {
				getSender().stop();
			}
		} catch (LifecycleException e) {
			log.warn("{}caught exception stopping listener", getLogPrefix(), e);
		}
	}

	/**
	 * Fill in thread-context with things needed by the JMSListener code.
	 * This includes a Session. The Session object can be passed in
	 * externally.
	 *
	 * @param rawMessage - Original message received, can not be <code>null</code>
	 * @return A {@link Map} with the properties of the JMS {@link jakarta.jms.Message}.
	 */
	public Map<String, Object> extractMessageProperties(jakarta.jms.Message rawMessage) {
		Map<String, Object> messageProperties = new HashMap<>();
		String id = "unset";
		String cid = "unset";
		DeliveryMode mode = null;
		Instant tsSent = null;
		Destination replyTo=null;
		try {
			mode = DeliveryMode.parse(rawMessage.getJMSDeliveryMode());
		} catch (JMSException e) {
			log.debug("ignoring JMSException in getJMSDeliveryMode()", e);
		}
		// --------------------------
		// retrieve MessageID
		// --------------------------
		try {
			id = rawMessage.getJMSMessageID();
		} catch (JMSException e) {
			log.debug("ignoring JMSException in getJMSMessageID()", e);
		}
		// --------------------------
		// retrieve CorrelationID
		// --------------------------
		try {
			cid = rawMessage.getJMSCorrelationID();
		} catch (JMSException e) {
			log.debug("ignoring JMSException in getJMSCorrelationID()", e);
		}
		// --------------------------
		// retrieve TimeStamp
		// --------------------------
		try {
			long lTimeStamp = rawMessage.getJMSTimestamp();
			tsSent = Instant.ofEpochMilli(lTimeStamp);
		} catch (JMSException e) {
			log.debug("ignoring JMSException in getJMSTimestamp()", e);
		}
		// --------------------------
		// retrieve ReplyTo address
		// --------------------------
		try {
			replyTo = rawMessage.getJMSReplyTo();
		} catch (JMSException e) {
			log.debug("ignoring JMSException in getJMSReplyTo()", e);
		}

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"listener on ["+ getDestinationName()
				+ "] got message with JMSDeliveryMode=[" + mode
				+ "] \n  JMSMessageID=[" + id
				+ "] \n  JMSCorrelationID=[" + cid
				+ "] \n  Timestamp Sent=[" + (tsSent!=null ? DateFormatUtils.format(tsSent) : null)
				+ "] \n  ReplyTo=[" + (replyTo==null?"none" : replyTo.toString())
				+ "] \n Message=[" + rawMessage
				+ "]");
		}

		PipeLineSession.updateListenerParameters(messageProperties, id, cid, null, tsSent);
		messageProperties.put("timestamp",tsSent);
		messageProperties.put("replyTo",replyTo);
		return messageProperties;
	}


	/**
	 * Extracts data from message obtained from {@link IPullingListener#getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the context.
	 *
	 * @param rawMessage The {@link RawMessageWrapper} from which to extract the {@link Message}.
	 * @param context Context to populate. Either a {@link PipeLineSession} or a {@link Map threadContext} depending on caller.
	 * @return String  input {@link Message} for adapter.
	 */
	@Override
	public Message extractMessage(@Nonnull RawMessageWrapper<jakarta.jms.Message> rawMessage, @Nonnull Map<String, Object> context) throws ListenerException {
		try {
			return extractMessage(rawMessage.getRawMessage(), context, isSoap(), getSoapHeaderSessionKey(), soapWrapper);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public Message prepareReply(Message rawReply, Map<String,Object> threadContext) throws ListenerException {
		return prepareReply(rawReply, threadContext, null);
	}

	public Message prepareReply(Message rawReply, Map<String, Object> threadContext, String soapHeader) throws ListenerException {
		if (!isSoap()) {
			return rawReply;
		}
		Message replyMessage;
		if (soapHeader == null) {
			if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
				soapHeader = (String) threadContext.get(getSoapHeaderSessionKey());
			}
		}
		try {
			replyMessage = soapWrapper.putInEnvelope(rawReply, getReplyEncodingStyleURI(), getReplyNamespaceURI(), soapHeader);
		} catch (IOException e) {
			throw new ListenerException("cannot convert message", e);
		}
		if (log.isDebugEnabled()) log.debug("wrapped message [{}]", replyMessage);
		return replyMessage;
	}

	@Override
	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<jakarta.jms.Message> rawMessageWrapper, PipeLineSession session) throws ListenerException {
		String replyCid = null;

		if (Boolean.FALSE.equals(forceMessageIdAsCorrelationId)) {
			replyCid = session.getCorrelationId();
		}
		if (StringUtils.isEmpty(replyCid)) {
			replyCid = session.getMessageId();
		}

		log.debug("{} in JmsListener.afterMessageProcessed()", this::getLogPrefix);
		// handle reply
		try {
			Destination replyTo = isUseReplyTo() ? (Destination) session.get("replyTo") : null;
			if (replyTo==null && StringUtils.isNotEmpty(getReplyDestinationName())) {
				replyTo = getDestination(getReplyDestinationName());
			}

			if (replyTo != null) {

				log.debug("{} sending reply message with correlationID [{}], replyTo [{}]", this::getLogPrefix, logValue(replyCid), logValue(replyTo));
				long timeToLive = getReplyMessageTimeToLive();
				boolean ignoreInvalidDestinationException = false;
				if (timeToLive == 0) {
					//noinspection DataFlowIssue
					if (rawMessageWrapper.getRawMessage() instanceof jakarta.jms.Message) {
						jakarta.jms.Message messageReceived = rawMessageWrapper.getRawMessage();
						long expiration = messageReceived.getJMSExpiration();
						if (expiration != 0) {
							timeToLive = expiration - new Date().getTime();
							if (timeToLive <= 0) {
								log.warn("{} message [{}] expired [{}]ms, sending response with 1 second time to live", this::getLogPrefix, logValue(replyCid), logValue(timeToLive));
								timeToLive = 1000;
								// In case of a temporary queue it might already
								// have disappeared.
								ignoreInvalidDestinationException = true;
							}
						}
					} else {
						log.warn(getLogPrefix() + "{} message with correlationID [{}] is not a JMS message, but [{}], cannot determine time to live [{}]ms, sending response with 20 second time to live", this::getLogPrefix, logValue(replyCid), ()->rawMessageWrapper.getRawMessage().getClass().getName(), logValue(timeToLive));
						timeToLive = 1000;
						ignoreInvalidDestinationException = true;
					}
				}
				Map<String, Object> properties = getMessageProperties(session);
				sendReply(plr, replyTo, replyCid, timeToLive, ignoreInvalidDestinationException, session, properties);
			} else {
				if (getSender() == null) {
					log.info("[{}] no replyTo address found or not configured to use replyTo, and no sender, not sending the result.", getName());
				} else {
					if (log.isDebugEnabled()) {
						log.debug("[{}] no replyTo address found or not configured to use replyTo, sending message on nested sender with correlationID [{}] [{}]", getName(), replyCid, plr.getResult());
					}
					try (PipeLineSession pipeLineSession = new PipeLineSession()) {
						pipeLineSession.put(PipeLineSession.CORRELATION_ID_KEY, replyCid);
						getSender().sendMessageOrThrow(plr.getResult(), pipeLineSession).close();
					}
				}
			}
		} catch (JMSException | SenderException | TimeoutException | IOException e) {
			throw new ListenerException(e);
		}

		// handle commit/rollback or acknowledge
		try {
			if (plr != null && !isTransacted()) {
				if (isJmsTransacted()) {
					Session queueSession = (Session) session.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY); // session is/must be saved in threadcontext by JmsConnector
					if (queueSession == null) {
						log.error("{}session is null, cannot commit or roll back session", getLogPrefix());
					} else {
						if (plr.getState() != ExitState.SUCCESS) {
							log.warn("{}got exit state [{}], rolling back session", getLogPrefix(), plr.getState());
							queueSession.rollback();
						} else {
							queueSession.commit();
						}
					}
				} else {
					if (rawMessageWrapper.getRawMessage() != null && getAcknowledgeMode() == AcknowledgeMode.CLIENT_ACKNOWLEDGE) {
						if (plr.getState() != ExitState.ERROR) { // SUCCESS and REJECTED will both be acknowledged
							log.debug("{}acknowledging message", getLogPrefix());
							rawMessageWrapper.getRawMessage().acknowledge();
						} else {
							log.warn("{}got exit state [{}], skipping acknowledge", getLogPrefix(), plr.getState());
						}
					}
				}
			}
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public boolean messageWillBeRedeliveredOnExitStateError() {
		return isTransacted() || isJmsTransacted() || getAcknowledgeMode() == AcknowledgeMode.CLIENT_ACKNOWLEDGE;
	}

	protected void sendReply(PipeLineResult plr, Destination replyTo, String replyCid, long timeToLive, boolean ignoreInvalidDestinationException, PipeLineSession pipeLineSession, Map<String, Object> properties) throws ListenerException, JMSException, IOException, SenderException {
		Session session = (Session) pipeLineSession.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY); // session is/must be saved in PipeLineSession by JmsConnector
		send(session, replyTo, replyCid, prepareReply(plr.getResult(), pipeLineSession), getReplyMessageType(), timeToLive, getReplyDeliveryMode().getDeliveryMode(), getReplyPriority(), ignoreInvalidDestinationException, properties);
	}

	@Deprecated(forRemoval = true, since = "7.9.0")
	public void setSender(ISender newSender) {
		sender = newSender;
		ConfigurationWarnings.add(this, log, "["+getName()+"] has a nested Sender, which is deprecated. Please use attribute replyDestinationName or a Sender nested in Receiver instead", SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
	}

	/**
	 * Set additional message headers/properties on the JMS response, read after message has been processed!
	 *
	 * @param session which has been built during the pipeline
	 * @return a map with headers to set to the JMS response, or {@code null} if there was
	 * no session or no parameters.
	 */
	protected Map<String, Object> getMessageProperties(PipeLineSession session) {

		if (session != null && paramList != null) {
			return new HashMap<>(evaluateParameters(session));
		}

		return null;
	}

	@Override
	public void addParameter(IParameter p) {
		if (paramList == null) {
			paramList = new ParameterList();
		}
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	/**
	 * Retrieve JMS properties from the threadContext
	 * @param threadContext used throughout the pipeline
	 * @return a map with JMS headers to set
	 */
	private Map<String, Object> evaluateParameters(Map<String, Object> threadContext) {
		Map<String, Object> result = new HashMap<>();
		if (threadContext != null && paramList != null) {
			for (IParameter param : paramList) {
				Object value = param.getValue();

				if (StringUtils.isNotEmpty(param.getSessionKey())) {
					log.debug("trying to resolve sessionKey[{}]", param.getSessionKey());
					Object resolvedValue = threadContext.get(param.getSessionKey());
					if (resolvedValue != null) {
						value = resolvedValue;
					}
				}

				result.put(param.getName(), value);
			}
		}
		return result;
	}

	/**
	 * By default, the JmsListener takes the Correlation-ID (if present) as the ID that has to be used as Correlation-ID of the reply.
	 * When set to <code>true</code>, the messageID is used as Correlation-ID of the reply.
	 * @ff.default false
	 */
	public void setForceMessageIdAsCorrelationId(Boolean force){
		forceMessageIdAsCorrelationId = force;
	}

	/**
	 * Receive timeout <i>in milliseconds</i> as specified by the JMS API, see https://docs.oracle.com/javaee/7/api/javax/jms/MessageConsumer.html#receive-long-
	 * @ff.default 1000
	 */
	public void setTimeout(long newTimeout) {
		timeout = newTimeout;
	}


	/**
	 * Flag if reply-to queue from the request message should be used or not.
	 *
	 * @ff.default true
	 */
	public void setUseReplyTo(boolean newUseReplyTo) {
		useReplyTo = newUseReplyTo;
	}

	/**
	 * Name of the JMS destination (queue or topic) to use for sending replies. If <code>useReplyTo=true</code>,
	 * the sender specified reply destination takes precedence over this one.
	 */
	public void setReplyDestinationName(String destinationName) {
		this.replyDestinationName = destinationName;
	}

	/**
	 * Value of the JMSType field of the reply message
	 * @ff.default not set by application
	 */
	public void setReplyMessageType(String string) {
		replyMessageType = string;
	}


	/**
	 * Controls mode that reply messages are sent with
	 * @ff.default NON_PERSISTENT
	 */
	public void setReplyDeliveryMode(DeliveryMode replyDeliveryMode) {
		this.replyDeliveryMode = replyDeliveryMode;
	}


	/**
	 * Sets the priority that is used to deliver the reply message. Ranges from 0 to 9. Effectively the default priority is set by JMS to 4, <code>-1</code> means not set and thus uses the JMS default
	 * @ff.default -1
	 */
	public void setReplyPriority(int i) {
		replyPriority = i;
	}


	/**
	 * Time <i>in milliseconds</i> after which the reply-message will expire
	 * @ff.default 0
	 */
	public void setReplyMessageTimeToLive(long l) {
		replyMessageTimeToLive = l;
	}

	/**
	 * If <code>true</code>, messages sent are put in a SOAP envelope
	 * @ff.default false
	 */
	public void setSoap(boolean b) {
		soap = b;
	}

	public void setReplyEncodingStyleURI(String string) {
		replyEncodingStyleURI = string;
	}

	public void setReplyNamespaceURI(String string) {
		replyNamespaceURI = string;
	}

	public void setReplySoapAction(String string) {
		replySoapAction = string;
	}

	/**
	 * sessionKey to store the SOAP header of the incoming message
	 * @ff.default soapHeader
	 */
	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}

}
