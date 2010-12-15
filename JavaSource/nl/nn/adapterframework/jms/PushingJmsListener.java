/*
 * $Log: PushingJmsListener.java,v $
 * Revision 1.20  2010-12-15 15:15:13  L190409
 * enable ackmode setting in configuration
 *
 * Revision 1.19  2010/01/28 15:00:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'Connection' classes to 'MessageSource'
 *
 * Revision 1.18  2010/01/20 08:23:58  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * bugfix - avoid NPE in getDeliveryCount
 *
 * Revision 1.17  2009/07/28 12:44:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enable SOAP over JMS
 *
 * Revision 1.16  2008/09/01 15:13:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made common baseclass for pushing and pulling jms listeners
 *
 * Revision 1.15  2008/08/27 16:15:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * introduced delivery count calculation
 *
 * Revision 1.14  2008/02/28 16:23:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * use PipeLineSession.setListenerParameters()
 *
 * Revision 1.13  2008/02/19 09:39:44  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.12  2008/02/08 09:48:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reintroduced rollback for states other than commitOnState
 *
 * Revision 1.11  2008/02/06 16:37:02  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * disabled use of commitOnState
 *
 * Revision 1.10  2008/01/29 12:20:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for thread number control
 *
 * Revision 1.9  2008/01/11 09:45:08  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * copy jmsTransacted to jmsConnector
 *
 * Revision 1.8  2008/01/03 15:51:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * rework port connected listener interfaces
 *
 * Revision 1.7  2007/11/23 14:22:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Remove code that confirms processing of message to JMS Session, because from the PushingJmsListener this is now always done by a container.
 *
 * Revision 1.6  2007/11/22 13:29:52  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * some more logging
 *
 * Revision 1.5  2007/11/22 09:08:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update from ejb-branch
 *
 * Revision 1.1.2.6  2007/11/15 10:35:24  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add methods new in implemented interface
 *
 * Revision 1.1.2.5  2007/11/06 13:15:10  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Move code putting properties into threadContext from 'getIdFromRawMessage' to 'populateThreadContext'
 *
 * Revision 1.1.2.4  2007/11/06 12:43:56  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Improve some JavaDoc
 *
 * Revision 1.1.2.3  2007/11/06 12:41:17  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add original raw message as parameter to method 'createThreadContext' of 'pushingJmsListener' in preparation of adding it to interface
 *
 * Revision 1.1.2.2  2007/11/06 12:29:54  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename parameter 'context' to 'threadContext', in keeping with other code
 *
 * Revision 1.1.2.1  2007/11/06 09:39:14  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Merge refactoring/renaming from HEAD
 *
 * Revision 1.4  2007/11/05 13:06:55  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Rename and redefine methods in interface IListenerConnector to remove 'jms' from names
 *
 * Revision 1.3  2007/11/05 12:26:51  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Implement new interface 'IPortConnectedListener'
 * * Rename property 'jmsConfigurator' to 'jmsConnector'
 *
 * Revision 1.2  2007/11/05 10:33:15  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Move interface 'IListenerConnector' from package 'configuration' to package 'core' in preparation of renaming it
 *
 * Revision 1.1  2007/10/16 09:52:35  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Change over JmsListener to a 'switch-class' to facilitate smoother switchover from older version to spring version
 *
 * Revision 1.25.4.7  2007/10/12 09:09:06  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Fix compilation-problems after code-merge
 *
 * Revision 1.25.4.6  2007/10/04 12:01:21  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Work on EJB version of IBIS
 *
 * Revision 1.25.4.5  2007/10/01 09:16:18  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Lazy creation of Session when not provided by caller
 *
 * Revision 1.25.4.4  2007/09/28 14:20:26  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add destroying of thread-context; allow session to be 'null' when populating thread-context
 *
 * Revision 1.25.4.3  2007/09/26 06:05:18  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * Add exception-propagation to new JMS Listener; increase robustness of JMS configuration
 *
 * Revision 1.25.4.2  2007/09/21 09:20:33  Tim van der Leeuw <tim.van.der.leeuw@ibissource.org>
 * * Remove UserTransaction from Adapter
 * * Remove InProcessStorage; refactor a lot of code in Receiver
 *
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
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.IThreadCountControllable;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.PipeLineSession;

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
 * @version Id
 */
public class PushingJmsListener extends JmsListenerBase implements IPortConnectedListener, IThreadCountControllable, IKnowsDeliveryCount {

	private String listenerPort;
	private String cacheMode; 
    
    private IListenerConnector jmsConnector;
    private IMessageHandler handler;
    private IReceiver receiver;
    private IbisExceptionListener exceptionListener;
    
    

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
		Destination destination=null;
        try {
			destination=getDestination();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"could not get Destination",e);
		}
        try {
			jmsConnector.configureEndpointConnection(this, getMessagingSource().getConnectionFactory(), destination, getExceptionListener(), getCacheMode(), getAckMode(), isJmsTransacted(), getMessageSelector());
		} catch (JmsException e) {
			throw new ConfigurationException(e);
		}
    }

    public void open() throws ListenerException {
        super.open();
        jmsConnector.start();
    }

    public void close() throws ListenerException {
        try {
            jmsConnector.stop();
			super.close();
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }
 

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException {
		String cid     = (String) threadContext.get(PipeLineSession.technicalCorrelationIdKey);
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
        
        	if (plr!=null && isJmsTransacted() && StringUtils.isNotEmpty(getCommitOnState()) && 
	        		!getCommitOnState().equals(plr.getState())) {
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
