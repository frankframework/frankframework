/*
   Copyright 2013 Nationale-Nederlanden

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
import javax.jms.Message;
import javax.jms.Session;

import nl.nn.adapterframework.configuration.ConfigurationException;
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

import org.apache.commons.lang.StringUtils;

/**
 * JMSListener re-implemented as a pushing listener rather than a pulling listener.
 * The JMS messages have to come in from an external source: an MDB or a Spring
 * message container.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.jms.JmsListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>either <code>QUEUE</code> or <code>TOPIC</code></td><td><code>QUEUE</code></td></tr>
 * <tr><td>{@link #setQueueConnectionFactoryName(String) queueConnectionFactoryName}</td><td>jndi-name of the queueConnectionFactory, used when <code>destinationType<code>=</code>QUEUE</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTopicConnectionFactoryName(String) topicConnectionFactoryName}</td><td>jndi-name of the topicConnectionFactory, used when <code>destinationType<code>=</code>TOPIC</code></td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageSelector(String) messageSelector}</td><td>When set, the value of this attribute is used as a selector to filter messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setJmsTransacted(boolean) jmsTransacted}</td><td><i>Deprecated</i> when true, sessions are explicitly committed (exit-state equals commitOnState) or rolled-back (other exit-states). Please do not use this mechanism, but control transactions using <code>transactionAttribute</code>s.</td><td>false</td></tr>
 * <tr><td>{@link #setCommitOnState(String) commitOnState}</td><td><i>Deprecated</i> exit state to control commit or rollback of jmsSession. Only used if <code>jmsTransacted</code> is set true.</td><td>"success"</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>"auto", "dups" or "client"</td><td>"auto"</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds</td><td>3000 [ms]</td></tr>
 * <tr><td>{@link #setUseReplyTo(boolean) useReplyTo}</td><td>&nbsp;</td><td>true</td></tr>
 * <tr><td>{@link #setReplyMessageTimeToLive(long) replyMessageTimeToLive}</td><td>time that replymessage will live</td><td>0 [ms]</td></tr>
 * <tr><td>{@link #setReplyMessageType(String) replyMessageType}</td><td>value of the JMSType field of the reply message</td><td>not set by application</td></tr>
 * <tr><td>{@link #setReplyDeliveryMode(String) replyDeliveryMode}</td><td>controls mode that reply messages are sent with: either 'persistent' or 'non_persistent'</td><td>not set by application</td></tr>
 * <tr><td>{@link #setReplyPriority(int) replyPriority}</td><td>sets the priority that is used to deliver the reply message. ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by Jms to 4</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForceMQCompliancy(String) forceMQCompliancy}</td><td>Possible values: 'MQ' or 'JMS'. Setting to 'MQ' informs the MQ-server that the replyto queue is not JMS compliant.</td><td>JMS</td></tr>
 * <tr><td>{@link #setForceMessageIdAsCorrelationId(boolean) forceMessageIdAsCorrelationId}</td><td>
 * forces that the CorrelationId that is received is ignored and replaced by the messageId that is received. Use this to create a new, globally unique correlationId to be used downstream. It also
 * forces that not the Correlation ID of the received message is used in a reply as CorrelationId, but the MessageId.</td><td>false</td></tr>
 * </table>
 *</p><p><b>Using transactions</b><br/>
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
 * @version $Id$
 */
public class PushingJmsListener extends JmsListenerBase implements IPortConnectedListener, IThreadCountControllable, IKnowsDeliveryCount {

	private String listenerPort;
	private String cacheMode;

    private IListenerConnector jmsConnector;
    private IMessageHandler handler;
    private IReceiver receiver;
    private IbisExceptionListener exceptionListener;


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
        try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), destination, getExceptionListener(), getCacheMode(), getAckMode(), isJmsTransacted(), getMessageSelector());
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
    public void close() throws ListenerException {
        try {
            jmsConnector.stop();
			super.close();
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException {
		String cid     = (String) threadContext.get(IPipeLineSession.technicalCorrelationIdKey);
		Session session= (Session) threadContext.get(jmsConnector.THREAD_CONTEXT_SESSION_KEY); // session is/must be saved in threadcontext by JmsConnector

		if (log.isDebugEnabled()) log.debug(getLogPrefix()+"in PushingJmsListener.afterMessageProcessed()");
		try {
			Destination replyTo = (Destination) threadContext.get("replyTo");

			// handle reply
			if (isUseReplyTo() && (replyTo != null)) {

				log.debug("sending reply message with correlationID[" + cid + "], replyTo [" + replyTo.toString()+ "]");
				long timeToLive = getReplyMessageTimeToLive();
				if (timeToLive == 0) {
					Message messageReceived=(Message)rawMessage;
					long expiration=messageReceived.getJMSExpiration();
					if (expiration!=0) {
						timeToLive=expiration-new Date().getTime();
						if (timeToLive<=0) {
							log.warn("message ["+cid+"] expired ["+timeToLive+"]ms, sending response with 1 second time to live");
							timeToLive=1000;
						}
					}
				}
				send(session, replyTo, cid, prepareReply(plr.getResult(),threadContext), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority());
			} else {
				if (getSender()==null) {
					log.info("["+getName()+"] has no sender, not sending the result.");
				} else {
					if (log.isDebugEnabled()) {
						log.debug(
							"["+getName()+"] no replyTo address found or not configured to use replyTo, using default destination"
							+ "sending message with correlationID[" + cid + "] [" + plr.getResult() + "]");
					}
					getSender().sendMessage(cid, plr.getResult());
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



    public void setJmsConnector(IListenerConnector configurator) {
		jmsConnector = configurator;
	}
    public IListenerConnector getJmsConnector() {
        return jmsConnector;
    }
	public IListenerConnector getListenerPortConnector() {
		return jmsConnector;
	}

	public void setExceptionListener(IbisExceptionListener listener) {
		this.exceptionListener = listener;
	}
    public IbisExceptionListener getExceptionListener() {
        return exceptionListener;
    }

	public void setHandler(IMessageHandler handler) {
		this.handler = handler;
	}
    public IMessageHandler getHandler() {
        return handler;
    }




    /**
     * Name of the WebSphere listener port that this JMS Listener binds to. Optional.
     *
     * This property is only used in EJB Deployment mode and has no effect otherwise.
     * If it is not set in EJB Deployment Mode, then the listener port name is
     * constructed by the {@link nl.nn.adapterframework.ejb.EjbListenerPortConnector} from
     * the Listener name, Adapter name and the Receiver name.
     *
     * @param listenerPort Name of the listener port, as configured in the application
     * server.
     */
    public void setListenerPort(String listenerPort) {
        this.listenerPort = listenerPort;
    }
	/**
	 * Name of the WebSphere listener port that this JMS Listener binds to. Optional.
	 *
	 * This property is only used in EJB Deployment mode and has no effect otherwise.
	 * If it is not set in EJB Deployment Mode, then the listener port name is
	 * constructed by the {@link nl.nn.adapterframework.ejb.EjbListenerPortConnector} from
	 * the Listener name, Adapter name and the Receiver name.
	 *
	 * @return The name of the WebSphere Listener Port, as configured in the
	 * application server.
	 */
	public String getListenerPort() {
		return listenerPort;
	}


    public void setReceiver(IReceiver receiver) {
        this.receiver = receiver;
    }
	public IReceiver getReceiver() {
		return receiver;
	}


	public void setCacheMode(String string) {
		cacheMode = string;
	}
	public String getCacheMode() {
		return cacheMode;
	}

	public boolean isThreadCountReadable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountReadable();
		}
		return false;
	}

	public boolean isThreadCountControllable() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.isThreadCountControllable();
		}
		return false;
	}

	public int getCurrentThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getCurrentThreadCount();
		}
		return -1;
	}

	public int getMaxThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			return tcc.getMaxThreadCount();
		}
		return -1;
	}

	public void increaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.increaseThreadCount();
		}
	}

	public void decreaseThreadCount() {
		if (jmsConnector instanceof IThreadCountControllable) {
			IThreadCountControllable tcc = (IThreadCountControllable)jmsConnector;

			tcc.decreaseThreadCount();
		}
	}

	public int getDeliveryCount(Object rawMessage) {
		try {
			Message message=(Message)rawMessage;
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

}
