/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.naming.NamingException;

import org.apache.commons.lang3.StringUtils;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.configuration.SuppressKeys;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IRedeliveringListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLine.ExitState;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.receivers.RawMessageWrapper;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;

/**
 * Common baseclass for Pulling and Pushing JMS Listeners.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class JmsListenerBase extends JMSFacade implements HasSender, IWithParameters, IRedeliveringListener<javax.jms.Message> {

	private @Getter long timeOut = 1000; // Same default value as Spring: https://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/jms/listener/AbstractPollingMessageListenerContainer.html#setReceiveTimeout(long)
	private @Getter boolean useReplyTo=true;
	private @Getter String replyDestinationName;
	private @Getter String replyMessageType=null;
	private @Getter long replyMessageTimeToLive=0;
	private @Getter int replyPriority=-1;
	private @Getter DeliveryMode replyDeliveryMode=DeliveryMode.NON_PERSISTENT;
	private @Getter ISender sender;


	private @Getter boolean forceMessageIdAsCorrelationId=false;

	private @Getter boolean soap=false;
	private @Getter String replyEncodingStyleURI=null;
	private @Getter String replyNamespaceURI=null;
	private @Getter String replySoapAction=null;
	private @Getter String soapHeaderSessionKey="soapHeader";

	private SoapWrapper soapWrapper=null;

	private ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (isSoap()) {
			//ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			//String msg = getLogPrefix()+"the use of attribute soap=true has been deprecated. Please change to SoapWrapperPipe";
			//configWarnings.add(log, msg);
			soapWrapper=SoapWrapper.getInstance();
		}
		ISender sender = getSender();
		if (sender != null) {
			sender.configure();
		}

		if (paramList!=null) {
			paramList.configure();
		}
	}

	@Override
	public void open() throws ListenerException {
		try {
			super.open();
		} catch (Exception e) {
			throw new ListenerException("error opening listener [" + getName() + "]", e);
		}
		try {
			if (getSender() != null)
				getSender().open();
		} catch (SenderException e) {
			throw new ListenerException("error opening sender [" + getSender().getName() + "]", e);
		}
	}

	@Override
	public void close() {
		super.close();
		try {
			if (getSender() != null) {
				getSender().close();
			}
		} catch (Exception e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
		}
	}

	/**
	 * Fill in thread-context with things needed by the JMSListener code.
	 * This includes a Session. The Session object can be passed in
	 * externally.
	 *
	 * @param rawMessage - Original message received, can not be <code>null</code>
	 * @param threadContext - Thread context to be populated, can not be <code>null</code>
	 */
	public String getIdFromRawMessageWrapper(RawMessageWrapper<javax.jms.Message> rawMessage, Map<String, Object> threadContext) throws ListenerException {
		return getIdFromRawMessage(rawMessage.getRawMessage(), threadContext);
	}

	@Override
	public String getIdFromRawMessage(javax.jms.Message rawMessage, Map<String, Object> threadContext) throws ListenerException {
		String id = "unset";
		String cid = "unset";
		DeliveryMode mode = null;
		Date tsSent = null;
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
			tsSent = new Date(lTimeStamp);

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
				+ "] \n  Timestamp Sent=[" + DateUtils.format(tsSent)
				+ "] \n  ReplyTo=[" + ((replyTo==null)?"none" : replyTo.toString())
				+ "] \n Message=[" + rawMessage
				+ "]");
		}

		PipeLineSession.updateListenerParameters(threadContext, id, cid, null, tsSent);
		threadContext.put("timestamp",tsSent);
		threadContext.put("replyTo",replyTo);
		return id;
	}


	/**
	 * Extracts string from message obtained from getRawMessage. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return String  input message for adapter.
	 */
	public Message extractMessage(RawMessageWrapper<javax.jms.Message> rawMessage, Map<String,Object> threadContext) throws ListenerException {
		try {
			return extractMessage(rawMessage, threadContext, isSoap(), getSoapHeaderSessionKey(), soapWrapper);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public Message prepareReply(Message rawReply, Map<String,Object> threadContext) throws ListenerException {
		return prepareReply(rawReply, threadContext, null);
	}

	public Message prepareReply(Message rawReply, Map<String,Object> threadContext, String soapHeader) throws ListenerException {
		if (!isSoap()) {
			return rawReply;
		}
		Message replyMessage;
		if (soapHeader==null) {
			if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
				soapHeader=(String)threadContext.get(getSoapHeaderSessionKey());
			}
		}
		try {
			replyMessage = soapWrapper.putInEnvelope(rawReply, getReplyEncodingStyleURI(),getReplyNamespaceURI(),soapHeader);
		} catch (IOException e) {
			throw new ListenerException("cannot convert message",e);
		}
		if (log.isDebugEnabled()) log.debug("wrapped message [" + replyMessage + "]");
		return replyMessage;
	}

	public void afterMessageProcessed(PipeLineResult plr, RawMessageWrapper<javax.jms.Message> rawMessage, Map<String, Object> threadContext) throws ListenerException {
		String replyCid = null;

		if (!isForceMessageIdAsCorrelationId()) {
			replyCid = (String) threadContext.get(PipeLineSession.correlationIdKey);
		}
		if (StringUtils.isEmpty(replyCid)) {
			replyCid = (String) threadContext.get(PipeLineSession.messageIdKey);
		}

		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"in JmsListener.afterMessageProcessed()");
		// handle reply
		try {
			Destination replyTo = isUseReplyTo() ? (Destination) threadContext.get("replyTo") : null;
			if (replyTo==null && StringUtils.isNotEmpty(getReplyDestinationName())) {
				replyTo = getDestination(getReplyDestinationName());
			}

			if (replyTo != null) {

				log.debug(getLogPrefix()+"sending reply message with correlationID [" + replyCid + "], replyTo [" + replyTo.toString()+ "]");
				long timeToLive = getReplyMessageTimeToLive();
				boolean ignoreInvalidDestinationException = false;
				if (timeToLive == 0) {
					if (rawMessage instanceof javax.jms.Message) {
						javax.jms.Message messageReceived=(javax.jms.Message) rawMessage;
						long expiration=messageReceived.getJMSExpiration();
						if (expiration!=0) {
							timeToLive=expiration-new Date().getTime();
							if (timeToLive<=0) {
								log.warn(getLogPrefix()+"message ["+replyCid+"] expired ["+timeToLive+"]ms, sending response with 1 second time to live");
								timeToLive=1000;
								// In case of a temporary queue it might already
								// have disappeared.
								ignoreInvalidDestinationException = true;
							}
						}
					} else {
						log.warn(getLogPrefix()+"message with correlationID ["+replyCid+"] is not a JMS message, but ["+ rawMessage.getClass().getName()+"], cannot determine time to live ["+timeToLive+"]ms, sending response with 20 second time to live");
						timeToLive=1000;
						ignoreInvalidDestinationException = true;
					}
				}
				Map<String, Object> properties = getMessageProperties(threadContext);
				sendReply(plr, replyTo, replyCid, timeToLive, ignoreInvalidDestinationException, threadContext, properties);
			} else {
				if (getSender()==null) {
					log.info("["+getName()+"] no replyTo address found or not configured to use replyTo, and no sender, not sending the result.");
				} else {
					if (log.isDebugEnabled()) {
						log.debug("["+getName()+"] no replyTo address found or not configured to use replyTo, sending message on nested sender with correlationID [" + replyCid + "] [" + plr.getResult() + "]");
					}
					PipeLineSession pipeLineSession = new PipeLineSession();
					pipeLineSession.put(PipeLineSession.correlationIdKey,replyCid);
					getSender().sendMessageOrThrow(plr.getResult(), pipeLineSession);
				}
			}
		} catch (JMSException | SenderException | TimeoutException | NamingException | IOException | JmsException e) {
			throw new ListenerException(e);
		}

		// handle commit/rollback or acknowledge
		try {
			if (plr!=null && !isTransacted()) {
				if (isJmsTransacted()) {
					Session session = (Session)threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY); // session is/must be saved in threadcontext by JmsConnector
					if (session==null) {
						log.error(getLogPrefix()+"session is null, cannot commit or roll back session");
					} else {
						if (plr.getState()!=ExitState.SUCCESS) {
							log.warn(getLogPrefix()+"got exit state ["+plr.getState()+"], rolling back session");
							session.rollback();
						} else {
							session.commit();
						}
					}
				} else {
					if (rawMessage instanceof javax.jms.Message && getAcknowledgeModeEnum()==AcknowledgeMode.CLIENT_ACKNOWLEDGE) {
						if (plr.getState()!=ExitState.ERROR) { // SUCCESS and REJECTED will both be acknowledged
							log.debug(getLogPrefix()+"acknowledgeing message");
							((javax.jms.Message) rawMessage).acknowledge();
						} else {
							log.warn(getLogPrefix()+"got exit state ["+plr.getState()+"], skipping acknowledge");
						}
					}
				}
			}
		} catch (JMSException e) {
			throw new ListenerException(e);
		}
	}

	@Override
	public boolean messageWillBeRedeliveredOnExitStateError(Map<String, Object> context) {
		return isTransacted() || isJmsTransacted() || getAcknowledgeModeEnum()==AcknowledgeMode.CLIENT_ACKNOWLEDGE;
	}

	protected void sendReply(PipeLineResult plr, Destination replyTo, String replyCid, long timeToLive, boolean ignoreInvalidDestinationException, Map<String, Object> threadContext, Map<String, Object> properties) throws SenderException, ListenerException, NamingException, JMSException, IOException {
		Session session = (Session)threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY); // session is/must be saved in threadcontext by JmsConnector
		send(session, replyTo, replyCid, prepareReply(plr.getResult(),threadContext), getReplyMessageType(), timeToLive, getReplyDeliveryMode().getDeliveryMode(), getReplyPriority(), ignoreInvalidDestinationException, properties);
	}

	@Deprecated
	public void setSender(ISender newSender) {
		sender = newSender;
		ConfigurationWarnings.add(this, log, "["+getName()+"] has a nested Sender, which is deprecated. Please use attribute replyDestinationName or a Sender nested in Receiver instead", SuppressKeys.DEPRECATION_SUPPRESS_KEY, null);
	}

	/**
	 * Set additional message headers/properties on the JMS response, read after message has been processed!
	 * @param threadContext which has been build during the pipeline
	 * @return a map with headers to set to the JMS response
	 */
	protected Map<String, Object> getMessageProperties(Map<String, Object> threadContext) {
		Map<String, Object> properties = null;

		if (threadContext != null && paramList != null) {
			if(properties == null) {
				properties = new HashMap<String, Object>();
			}
			properties.putAll(evaluateParameters(threadContext));
		}

		return properties;
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
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
		Map<String, Object> result = new HashMap<String, Object>();
		if (threadContext != null && paramList != null) {
			for (Iterator<Parameter> parmIterator = paramList.iterator(); parmIterator.hasNext(); ) {
				Parameter param = parmIterator.next();
				Object value = param.getValue();

				if(StringUtils.isNotEmpty(param.getSessionKey())) {
					log.debug("trying to resolve sessionKey["+param.getSessionKey()+"]");
					Object resolvedValue = threadContext.get(param.getSessionKey());
					if(resolvedValue != null)
						value = resolvedValue;
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
	public void setForceMessageIdAsCorrelationId(boolean force){
		forceMessageIdAsCorrelationId = force;
	}

	@Deprecated
	public void setTimeOut(long newTimeOut) {
		timeOut = newTimeOut;
	}
	/**
	 * Receive timeout <i>in milliseconds</i> as specified by the JMS API, see https://docs.oracle.com/javaee/7/api/javax/jms/MessageConsumer.html#receive-long-
	 * @ff.default 1000
	 */
	public void setTimeout(long newTimeOut) {
		timeOut = newTimeOut;
	}


	/**
	 *
	 * @ff.default true
	 */
	public void setUseReplyTo(boolean newUseReplyTo) {
		useReplyTo = newUseReplyTo;
	}

	/**
	 * Name of the JMS destination (queue or topic) to use for sending replies. If <code>useReplyTo</code>=<code>true</code>,
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
