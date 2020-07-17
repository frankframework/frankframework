/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020 WeAreFrank!

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

import java.util.Date;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.Session;

import org.apache.commons.lang.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IKnowsDeliveryCount;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.ReceiverBase;
import nl.nn.adapterframework.util.CredentialFactory;
/**
 * JMSListener re-implemented as a pushing listener rather than a pulling listener.
 * The JMS messages have to come in from an external source: an MDB or a Spring
 * message container.
 *
 * This version of the <code>JmsListener</code> supports distributed transactions using the XA-protocol.
 * No special action is required to have the listener join the transaction.
 *
 *</p><p><b>Using jmsTransacted and acknowledgement</b><br/>
 * If jmsTransacted is set <code>true</code>, it should ensure that a message is received and processed on
 * a both or nothing basis. IBIS will commit the the message, otherwise perform rollback. However, using
 * jmsTransacted, IBIS does not bring transactions within the adapters under transaction control,
 * compromising the idea of atomic transactions. In the roll-back situation messages sent to other
 * destinations within the Pipeline are NOT rolled back if jmsTransacted is set <code>true</code>! In
 * the failure situation the message is therefore completely processed, and the roll back does not mean
 * that the processing is rolled back! To obtain the correct (transactional) behaviour, set
 * <code>transacted</code>="true" for the enclosing Receiver. Do not use jmsTransacted for any new situation.
 *
 *<p>
 * Setting {@link #setAcknowledgeMode(String) listener.acknowledgeMode} to "auto" means that messages are allways acknowledged (removed from
 * the queue, regardless of what the status of the Adapter is. "client" means that the message will only be removed from the queue
 * when the state of the Adapter equals the defined state for committing (specified by {@link #setCommitOnState(String) listener.commitOnState}).
 * The "dups" mode instructs the session to lazily acknowledge the delivery of the messages. This is likely to result in the
 * delivery of duplicate messages if JMS fails. It should be used by consumers who are tolerant in processing duplicate messages.
 * In cases where the client is tolerant of duplicate messages, some enhancement in performance can be achieved using this mode,
 * since a session has lower overhead in trying to prevent duplicate messages.
 * </p>
 * <p>The setting for {@link #setAcknowledgeMode(String) listener.acknowledgeMode} will only be processed if
 * the setting for {@link #setTransacted(boolean) listener.transacted} as well as for
 * {@link #setJmsTransacted(boolean) listener.jmsTransacted} is false.</p>
 *
 * <p>If {@link #setUseReplyTo(boolean) useReplyTo} is set and a replyTo-destination is
 * specified in the message, the JmsListener sends the result of the processing
 * in the pipeline to this destination. Otherwise the result is sent using the (optionally)
 * specified {@link #setSender(ISender) Sender}, that in turn sends the message to
 * whatever it is configured to.</p>
 *
 * <p><b>Notice:</b> the JmsListener is ONLY capable of processing
 * <code>javax.jms.TextMessage</code>s <br/><br/>
 * </p>
 *
 * @author  Tim van der Leeuw
 * @since   4.8
 */
public class PushingJmsListener extends JmsListenerBase implements IPortConnectedListener<javax.jms.Message>, IThreadCountControllable, IKnowsDeliveryCount<javax.jms.Message> {

	private String listenerPort;
	private String cacheMode;
	private IListenerConnector<javax.jms.Message> jmsConnector;
	private IMessageHandler<javax.jms.Message> handler;
	private IReceiver<javax.jms.Message> receiver;
	private IbisExceptionListener exceptionListener;
	private long pollGuardInterval = Long.MIN_VALUE;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (jmsConnector==null) {
			throw new ConfigurationException(getLogPrefix()+" has no jmsConnector. It should be configured via springContext.xml");
		}
		if (StringUtils.isNotEmpty(getCacheMode())) {
			if (!getCacheMode().equals("CACHE_NONE") &&
				!getCacheMode().equals("CACHE_CONNECTION") &&
				!getCacheMode().equals("CACHE_SESSION") &&
				!getCacheMode().equals("CACHE_CONSUMER")) {
					throw new ConfigurationException(getLogPrefix()+"cacheMode ["+getCacheMode()+"] must be one of CACHE_NONE, CACHE_CONNECTION, CACHE_SESSION or CACHE_CONSUMER");
				}
		}
		Destination destination;
		try {
			destination = getDestination();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"could not get Destination",e);
		}
		if (getPollGuardInterval() == Long.MIN_VALUE) {
			setPollGuardInterval(getTimeOut() * 10);
		}
		if (getPollGuardInterval() <= getTimeOut()) {
			ConfigurationWarnings.add(this, log, "The pollGuardInterval ["+getPollGuardInterval()+"] should be larger than the receive timeout ["+getTimeOut()+"]");
		}
		CredentialFactory credentialFactory=null;
		if (StringUtils.isNotEmpty(getAuthAlias())) {
			credentialFactory=new CredentialFactory(getAuthAlias(), null, null);
		}
		try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), credentialFactory,
					destination, getExceptionListener(), getCacheMode(), getAckMode(),
					isJmsTransacted(), getMessageSelector(), getTimeOut(), getPollGuardInterval());
		} catch (JmsException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public void open() throws ListenerException {
		super.open();
		jmsConnector.start();
	}

	@Override
	public void close() {
		try {
			jmsConnector.stop();
		} catch (Exception e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
		} finally {
			super.close();
		}
	}


	@Override
	public void afterMessageProcessed(PipeLineResult plr, Object rawMessageOrWrapper, Map<String, Object> threadContext) throws ListenerException {
		String cid     = (String) threadContext.get(IPipeLineSession.technicalCorrelationIdKey);
		Session session= (Session) threadContext.get(IListenerConnector.THREAD_CONTEXT_SESSION_KEY); // session is/must be saved in threadcontext by JmsConnector

		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"in PushingJmsListener.afterMessageProcessed()");
		try {
			Destination replyTo = (Destination) threadContext.get("replyTo");

			// handle reply
			if (isUseReplyTo() && (replyTo != null)) {

				log.debug(getLogPrefix()+"sending reply message with correlationID[" + cid + "], replyTo [" + replyTo.toString()+ "]");
				long timeToLive = getReplyMessageTimeToLive();
				boolean ignoreInvalidDestinationException = false;
				if (timeToLive == 0) {
					if (rawMessageOrWrapper instanceof javax.jms.Message) {
						javax.jms.Message messageReceived=(javax.jms.Message)rawMessageOrWrapper;
						long expiration=messageReceived.getJMSExpiration();
						if (expiration!=0) {
							timeToLive=expiration-new Date().getTime();
							if (timeToLive<=0) {
								log.warn(getLogPrefix()+"message ["+cid+"] expired ["+timeToLive+"]ms, sending response with 1 second time to live");
								timeToLive=1000;
								// In case of a temporary queue it might already
								// have disappeared.
								ignoreInvalidDestinationException = true;
							}
						}
					} else {
						log.warn(getLogPrefix()+"message with correlationID ["+cid+"] is not a JMS message, but ["+rawMessageOrWrapper.getClass().getName()+"], cannot determine time to live ["+timeToLive+"]ms, sending response with 20 second time to live");
						timeToLive=1000;
						ignoreInvalidDestinationException = true;
					}
				}
				Map<String, Object> properties = getMessageProperties(threadContext);
				send(session, replyTo, cid, prepareReply(plr.getResult(),threadContext).asString(), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority(), ignoreInvalidDestinationException, properties);
			} else {
				if (getSender()==null) {
					log.info("["+getName()+"] has no sender, not sending the result.");
				} else {
					if (log.isDebugEnabled()) {
						log.debug("["+getName()+"] no replyTo address found or not configured to use replyTo, using default destination sending message with correlationID[" + cid + "] [" + plr.getResult() + "]");
					}
					PipeLineSessionBase pipeLineSession = new PipeLineSessionBase();
					pipeLineSession.put(IPipeLineSession.messageIdKey,cid);
					getSender().sendMessage(plr.getResult(), pipeLineSession);
				}
			}

			// TODO Do we still need this? Should we commit too? See
			// PullingJmsListener.afterMessageProcessed() too (which does a
			// commit, but no rollback).
			if (plr!=null && !isTransacted() && isJmsTransacted()
					&& StringUtils.isNotEmpty(getCommitOnState())
					&& !getCommitOnState().equals(plr.getState())) {
				if (session==null) {
					log.error(getLogPrefix()+"session is null, cannot roll back session");
				} else {
					log.warn(getLogPrefix()+"got exit state ["+plr.getState()+"], rolling back session");
					session.rollback();
				}
			}
		} catch (Exception e) {
			if (e instanceof ListenerException) {
				throw (ListenerException)e;
			} else {
				throw new ListenerException(e);
			}
		}
	}

	public void setJmsConnector(IListenerConnector<javax.jms.Message> configurator) {
		jmsConnector = configurator;
	}
	public IListenerConnector<javax.jms.Message> getJmsConnector() {
		return jmsConnector;
	}

	@Override
	public IListenerConnector<javax.jms.Message> getListenerPortConnector() {
		return jmsConnector;
	}

	@Override
	public void setExceptionListener(IbisExceptionListener listener) {
		this.exceptionListener = listener;
	}
	@Override
	public IbisExceptionListener getExceptionListener() {
		return exceptionListener;
	}

	@Override
	public void setHandler(IMessageHandler<javax.jms.Message> handler) {
		this.handler = handler;
	}
	@Override
	public IMessageHandler<javax.jms.Message> getHandler() {
		return handler;
	}



	/**
	 * Name of the WebSphere listener port that this JMS Listener binds to.
	 * Optional.
	 *
	 * This property is only used in EJB Deployment mode and has no effect
	 * otherwise.
	 *
	 * @param listenerPort Name of the listener port, as configured in the
	 *                     application server.
	 */
	public void setListenerPort(String listenerPort) {
		this.listenerPort = listenerPort;
	}

	/**
	 * Name of the WebSphere listener port that this JMS Listener binds to. Optional.
	 *
	 * This property is only used in EJB Deployment mode and has no effect otherwise.	 *
	 * @return The name of the WebSphere Listener Port, as configured in the
	 * application server.
	 */
	@Override
	public String getListenerPort() {
		return listenerPort;
	}


	@Override
	public void setReceiver(IReceiver<javax.jms.Message> receiver) {
		this.receiver = receiver;
	}
	@Override
	public IReceiver<javax.jms.Message> getReceiver() {
		return receiver;
	}

	public ReceiverBase<javax.jms.Message> getReceiverBase() {
		if (receiver instanceof ReceiverBase) {
			ReceiverBase<javax.jms.Message> rb = (ReceiverBase<javax.jms.Message>) receiver;
			return rb;
		}
		return null;
	}

	public void setCacheMode(String string) {
		cacheMode = string;
	}
	public String getCacheMode() {
		return cacheMode;
	}

	@Override
	public boolean isThreadCountReadable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountReadable();
		}
		return false;
	}

	@Override
	public boolean isThreadCountControllable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountControllable();
		}
		return false;
	}

	@Override
	public int getCurrentThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getCurrentThreadCount();
		}
		return -1;
	}

	@Override
	public int getMaxThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getMaxThreadCount();
		}
		return -1;
	}

	@Override
	public void increaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.increaseThreadCount();
		}
	}

	@Override
	public void decreaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.decreaseThreadCount();
		}
	}

	@Override
	public int getDeliveryCount(javax.jms.Message rawMessage) {
		try {
			javax.jms.Message message=rawMessage;
			// Note: Tibco doesn't set the JMSXDeliveryCount for messages
			// delivered for the first time (when JMSRedelivered is set to
			// false). Hence when set is has a value of 2 or higher. When not
			// set a NumberFormatException is thrown.
			int value = message.getIntProperty("JMSXDeliveryCount");
			if (log.isDebugEnabled()) log.debug("determined delivery count ["+value+"]");
			return value;
		} catch (NumberFormatException nfe) {
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"NumberFormatException in determination of DeliveryCount");
			return -1;
		} catch (Exception e) {
			log.error(getLogPrefix()+"exception in determination of DeliveryCount", e);
			return -1;
		}
	}

	@IbisDoc({"interval in milliseconds for the poll guard to check whether a successful poll was done by the receive (https://docs.oracle.com/javaee/7/api/javax/jms/messageconsumer.html#receive-long-) since last check. when polling has stopped this will be logged and the listener will be stopped and started in an attempt to workaround problems with polling. polling might stop due to bugs in the jms driver/implementation which should be fixed by the supplier. as the poll time includes reading and processing of the message no successful poll might be registered since the last check when message processing takes a long time, hence while messages are being processed the check on last successful poll will be skipped. set to -1 to disable", "ten times the specified timeout"})
	public void setPollGuardInterval(long pollGuardInterval) {
		this.pollGuardInterval = pollGuardInterval;
	}

	public long getPollGuardInterval() {
		return pollGuardInterval;
	}

}
