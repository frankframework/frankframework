/*
   Copyright 2013 Nationale-Nederlanden, 2020, 2023 WeAreFrank!

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
package nl.nn.adapterframework.extensions.ifsa.jms;


import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import com.ing.ifsa.IFSAConstants;
import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAQueue;
import com.ing.ifsa.IFSAQueueSender;
import com.ing.ifsa.IFSAServerQueueSender;
import com.ing.ifsa.IFSATextMessage;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.IbisException;
import nl.nn.adapterframework.extensions.ifsa.IfsaException;
import nl.nn.adapterframework.extensions.ifsa.IfsaMessageProtocolEnum;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.JtaUtil;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Base class for IFSA 2.0/2.2 functions.
 * <br/>
 * <p>Descender classes must set either Requester or Provider behaviour in their constructor.</p>
 *
 * N.B.
 * Starting from IFSA-jms version 2.2.10.055(beta) a feature was created to have separate service-queues for Request/Reply
 * and for Fire & Forget services. This allows applications to provide both types of services, each in its own transaction
 * mode. This options is not compatible with earlier versions of IFSA-jms. If an earlier version of IFSA-jms is deployed on
 * the server, this behaviour must be disabled by the following setting in DeploymentSpecifics.properties:
 *
 * <code>    ifsa.provider.useSelectors=false</code>
 *
 * @author Johan Verrips / Gerrit van Brakel
 * @since 4.2
 */
public class IfsaFacade implements IConfigurable, HasPhysicalDestination {
	private final @Getter(onMethod = @__(@Override)) String domain = "IFSA";
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private static final String USE_SELECTOR_FOR_PROVIDER_KEY="ifsa.provider.useSelectors";
	private static final int DEFAULT_PROVIDER_ACKNOWLEDGMODE_RR=Session.CLIENT_ACKNOWLEDGE;
	private static final int DEFAULT_PROVIDER_ACKNOWLEDGMODE_FF=Session.AUTO_ACKNOWLEDGE;
	private static final int DEFAULT_REQUESTER_ACKNOWLEDGMODE_RR=Session.AUTO_ACKNOWLEDGE;
	private static final int DEFAULT_REQUESTER_ACKNOWLEDGMODE_FF=Session.AUTO_ACKNOWLEDGE;

	private static Boolean useSelectorsStore=null;

	private int ackMode = -1;

	private String name;
	private String applicationId;
	private String serviceId;
	private String polishedServiceId=null;;
	private IfsaMessageProtocolEnum messageProtocol;

	private long timeOut = 20000; // when set less than zero the IFSA-expiry will be used

    private IFSAQueue queue;

	private IfsaMessagingSource messagingSource=null;

	private boolean requestor=false;
	private boolean provider=false;

	private String providerSelector=null;

	public IfsaFacade(boolean asProvider) {
		super();
		if (asProvider) {
			provider=true;
		}
		else
			requestor=true;
	}

	protected String getLogPrefix() {

		String objectType;
		String serviceInfo="";
		try {
			if (isRequestor()) {
				objectType = "IfsaRequester";
				serviceInfo = "of Application ["+getApplicationId()+"] "+(polishedServiceId!=null?"to Service ["+polishedServiceId+"] ":"");
			} else {
				objectType = "IfsaProvider";
				serviceInfo = "for Application ["+getApplicationId()+"] ";
			}
		} catch (IfsaException e) {
			log.debug("Exception determining objectType in getLogPrefix",e);
			objectType="Object";
			serviceInfo = "of Application ["+getApplicationId()+"]";
		}

		return objectType + "["+ getName()+ "] " + serviceInfo;
	}

	/**
	 * Checks if messageProtocol and serviceId (only for Requestors) are specified
	 */
	@Override
	public void configure() throws ConfigurationException {

		// perform some basic checks
		if (StringUtils.isEmpty(getApplicationId())) {
			throw new ConfigurationException(getLogPrefix()+"applicationId is not specified");
		}
		if (getMessageProtocolEnum() == null) {
			throw new ConfigurationException(getLogPrefix()+
				"invalid messageProtocol specified ["
					+ getMessageProtocolEnum()
					+ "], should be one of the following "
					+ IfsaMessageProtocolEnum.getNames());
		}
		try {
			if (getAckMode()<0) {
				if (getMessageProtocolEnum()==IfsaMessageProtocolEnum.FIRE_AND_FORGET) {
					if (isRequestor()) {
						setAckMode(DEFAULT_REQUESTER_ACKNOWLEDGMODE_FF);
					} else {
						setAckMode(DEFAULT_PROVIDER_ACKNOWLEDGMODE_FF);
					}
				} else if (getMessageProtocolEnum()==IfsaMessageProtocolEnum.REQUEST_REPLY) {
					if (isRequestor()) {
						setAckMode(DEFAULT_REQUESTER_ACKNOWLEDGMODE_RR);
					} else {
						setAckMode(DEFAULT_PROVIDER_ACKNOWLEDGMODE_RR);
					}
				} else {
					throw new ConfigurationException(getLogPrefix()+"illegal messageProtocol");
				}
			}
		} catch(IfsaException e) {
			throw new ConfigurationException(getLogPrefix()+"cannot set acknowledgemode",e);
		}
		// TODO: check if serviceId is specified, either as attribute or as parameter
//		try {
//			log.debug(getLogPrefix()+"opening connection for service, to obtain info about XA awareness");
//			getConnection();   // obtain and cache connection, then start it.
//			closeService();
//		} catch (IfsaException e) {
//			cleanUpAfterException();
//			throw new ConfigurationException(e);
//		}
	}

	protected void cleanUpAfterException() {
		try {
			closeService();
		} catch (IfsaException e) {
			log.warn("exception closing ifsaConnection after previous exception, current:",e);
		}
	}

	/**
	 * Prepares object for communication on the IFSA bus.
	 * Obtains a connection and a serviceQueue.
	 */
	public void openService() throws IfsaException {
		try {
			log.debug(getLogPrefix()+"opening connection for service");
			getMessagingSource();
			getServiceQueue(); // obtain and cache service queue
		} catch (IfsaException e) {
			cleanUpAfterException();
			throw e;
		}
	}

	/**
	 * Stops communication on the IFSA bus.
	 * Releases references to serviceQueue and connection.
	 */
	public void closeService() throws IfsaException {
	    try {
	        if (messagingSource != null) {
	            try {
					messagingSource.close();
				} catch (IbisException e) {
					if (e instanceof IfsaException) {
						throw (IfsaException)e;
					}
					throw new IfsaException(e);
	            }
                log.debug(getLogPrefix()+"closed connection for service");
	        }
	    } finally {
	    	// make sure all objects are reset, to be able to restart after IFSA parameters have changed (e.g. at iterative installation time)
	        queue = null;
			messagingSource = null;
	    }
	}


	/**
	 * Looks up the <code>serviceId</code> in the <code>IFSAContext</code>.<br/>
	 * <p>The method is knowledgable of Provider versus Requester processing.
	 * When the request concerns a Provider <code>lookupProviderInput</code> is used,
	 * when it concerns a Requester <code>lookupService(serviceId)</code> is used.
	 * This method distinguishes a server-input queue and a client-input queue
	 */
	protected IFSAQueue getServiceQueue() throws IfsaException {
		if (queue == null) {
			if (isRequestor()) {
				if (getServiceId() != null) {
					queue = getMessagingSource().lookupService(getServiceId());
					if (log.isDebugEnabled()) {
						log.info(getLogPrefix()+ "got Queue to send messages on "+getPhysicalDestinationName());
					}
				}
			} else {
				queue = getMessagingSource().lookupProviderInput();
				if (log.isDebugEnabled()) {
					log.info(getLogPrefix()+ "got Queue to receive messages from "+getPhysicalDestinationName());
				}
			}
		}
		return queue;
	}

	protected IfsaMessagingSource getMessagingSource() throws IfsaException {
		if (messagingSource == null) {
			synchronized (this) {
				if (messagingSource == null) {
					log.debug(getLogPrefix()+"instantiating IfsaConnectionFactory");
					IfsaMessagingSourceFactory ifsaConnectionFactory = new IfsaMessagingSourceFactory();
					try {
						log.debug(getLogPrefix()+"creating IfsaConnection");
						messagingSource = (IfsaMessagingSource)ifsaConnectionFactory.getConnection(getApplicationId());
					} catch (IbisException e) {
						if (e instanceof IfsaException) {
							throw (IfsaException)e;
						}
						throw new IfsaException(e);
					}
				}
			}
		}
		return messagingSource;
	}

	/**
	 *  Create a session on the connection to the service
	 */
	protected QueueSession createSession() throws IfsaException {
		try {
			int mode = getAckMode();
			if (isRequestor() && messagingSource.canUseIfsaModeSessions()) {
				mode += IFSAConstants.QueueSession.IFSA_MODE; // let requestor receive IFSATimeOutMessages
			}
			return (QueueSession) messagingSource.createSession(isJmsTransacted(), mode);
		} catch (IbisException e) {
			if (e instanceof IfsaException) {
				throw (IfsaException)e;
			}
			throw new IfsaException(e);
		}
	}

	protected void closeSession(Session session) {
		try {
			getMessagingSource().releaseSession(session);
		} catch (IfsaException e) {
			log.warn("Exception releasing session", e);
		}
	}


	protected QueueSender createSender(QueueSession session, Queue queue)
	    throws IfsaException {

	    try {
	        QueueSender queueSender = session.createSender(queue);
	        if (log.isDebugEnabled()) {
	            log.debug(getLogPrefix()+ "got queueSender ["
	                            + ToStringBuilder.reflectionToString((IFSAQueueSender) queueSender)+ "]");
	        }
	        return queueSender;
	    } catch (Exception e) {
	        throw new IfsaException(e);
	    }
	}

	protected synchronized String getProviderSelector() {
		if (providerSelector==null && useSelectorsForProviders()) {
			try {
				providerSelector=""; // set default, also to avoid re-evaluation time and time again for lower ifsa-versions.
				if (messageProtocol == IfsaMessageProtocolEnum.REQUEST_REPLY) {
					providerSelector=IFSAConstants.QueueReceiver.SELECTOR_RR;
				}
				if (messageProtocol == IfsaMessageProtocolEnum.FIRE_AND_FORGET) {
					providerSelector=IFSAConstants.QueueReceiver.SELECTOR_FF;
				}
			} catch (Throwable t) {
				log.debug(getLogPrefix()+"exception determining selector, probably lower ifsa version, ignoring");
			}
		}
		return providerSelector;
	}

	/**
	 * Gets the queueReceiver, by utilizing the <code>getInputQueue()</code> method.<br/>
	 * For serverside getQueueReceiver() the creating of the QueueReceiver is done
	 * without the <code>selector</code> information, as this is not allowed
	 * by IFSA.<br/>
	 * For a clientconnection, the receiver is done with the <code>getClientReplyQueue</code>
	 * @see javax.jms.QueueReceiver
	 */
	protected QueueReceiver getServiceReceiver(
		QueueSession session)
		throws IfsaException {

		try {
			QueueReceiver queueReceiver;

			if (isProvider()) {
				String selector = getProviderSelector();
				if (StringUtils.isEmpty(selector)) {
					queueReceiver = session.createReceiver(getServiceQueue());
				} else {
					//log.debug(getLogPrefix()+"using selector ["+selector+"]");
					try {
						queueReceiver = session.createReceiver(getServiceQueue(), selector);
					} catch (JMSException e) {
						log.warn("caught exception, probably due to use of selector ["+selector+"], falling back to non-selected mode",e);
						queueReceiver = session.createReceiver(getServiceQueue());
					}
				}
			} else {
				throw new IfsaException("cannot obtain ServiceReceiver: Requestor cannot act as Provider");
			}
			if (log.isDebugEnabled() && !isSessionsArePooled()) {
				log.debug(getLogPrefix()+ "got receiver for queue ["
						+ queueReceiver.getQueue().getQueueName()
						+ "] "+ ToStringBuilder.reflectionToString(queueReceiver));
			}
			return queueReceiver;
		} catch (JMSException e) {
			throw new IfsaException(e);
		}
	}

	public long getExpiry() throws IfsaException {
		return getExpiry((IFSAQueue) getServiceQueue());
	}

	public long getExpiry(IFSAQueue queue) throws IfsaException {
		long expiry = getTimeOut();
		if (expiry>=0) {
			return expiry;
		}
		try {
			return queue.getExpiry();
		} catch (JMSException e) {
			throw new IfsaException("error retrieving timeOut value", e);
		}
	}

    public String getMessageProtocol() {
		if (messageProtocol==null) {
			return null;
		} else {
			return messageProtocol.getLabel();
		}
    }
    public IfsaMessageProtocolEnum getMessageProtocolEnum() {
        return messageProtocol;
    }

	/**
	 * Gets the queueReceiver, by utilizing the <code>getInputQueue()</code> method.<br/>
	 * For serverside getQueueReceiver() the creating of the QueueReceiver is done
	 * without the <code>selector</code> information, as this is not allowed
	 * by IFSA.<br/>
	 * For a clientconnection, the receiver is done with the <code>getClientReplyQueue</code>
	 */
	public QueueReceiver getReplyReceiver(QueueSession session, Message sentMessage)
	    throws IfsaException {

	    if (isProvider()) {
	        throw new IfsaException("cannot get ReplyReceiver: Provider cannot act as Requestor");
	    }

		return getMessagingSource().getReplyReceiver(session, sentMessage);
	}

	public void closeReplyReceiver(QueueReceiver receiver) throws IfsaException {
		log.debug(getLogPrefix()+"closing replyreceiver");
		getMessagingSource().closeReplyReceiver(receiver);
	}

	/**
	 * Indicates whether the object at hand represents a Client (returns <code>True</code>) or
	 * a Server (returns <code>False</code>).
	 */
	public boolean isRequestor() throws IfsaException {

		if (requestor && provider) {
	        throw new IfsaException("cannot be both Requestor and Provider");
		}
		if (!requestor && !provider) {
	        throw new IfsaException("not configured as Requestor or Provider");
		}
		return requestor;
	}
	/**
	 * Indicates whether the object at hand represents a Client (returns <code>False</code>) or
	 * a Server (returns <code>True</code>).
	 *
	 * @see #isRequestor()
	 */
	public boolean isProvider() throws IfsaException {
		return ! isRequestor();
	}
    /**
     * Sends a message,and if transacted, the queueSession is committed.
     * <p>This method is intended for <b>clients</b>, as <b>server</b>s
     * will use the <code>sendReply</code>.
     * @return the correlationID of the sent message
     */
    public TextMessage sendMessage(QueueSession session, QueueSender sender, String message, Map udzMap, String bifName, byte btcData[])
        throws IfsaException {

	    try {
			if (!isRequestor()) {
				throw new IfsaException(getLogPrefix()+ "Provider cannot use sendMessage, should use sendReply");
			}
	        IFSATextMessage msg = (IFSATextMessage)session.createTextMessage();
	        msg.setText(message);
			if (udzMap != null && msg instanceof IFSAMessage) {
				// Handle UDZs
				log.debug(getLogPrefix()+"add UDZ map to IFSAMessage");
				// process the udzMap
				Map udzObject = msg.getOutgoingUDZObject();
				udzObject.putAll(udzMap);
			}
			String replyToQueueName="-";
	        //Client side
	        if (messageProtocol == IfsaMessageProtocolEnum.REQUEST_REPLY) {
	            // set reply-to address
	            Queue replyTo=getMessagingSource().getClientReplyQueue(session);
	            msg.setJMSReplyTo(replyTo);
	            replyToQueueName=replyTo.getQueueName();
	        }
	        if (messageProtocol == IfsaMessageProtocolEnum.FIRE_AND_FORGET) {
	         	// not applicable
	        }
			if (StringUtils.isNotEmpty(bifName)) {
				msg.setBifName(bifName);
			}
			if (btcData!=null && btcData.length>0) {
				msg.setBtcData(btcData);
			}

			if (log.isDebugEnabled()) {
				log.debug(getLogPrefix()
						+ " messageProtocol ["
						+ messageProtocol
						+ "] replyToQueueName ["
						+ replyToQueueName
						+ "] sending message ["
						+ message
						+ "]");
			} else {
				if (log.isInfoEnabled()) {
					log.info(getLogPrefix()
							+ " messageProtocol ["
							+ messageProtocol
							+ "] replyToQueueName ["
							+ replyToQueueName
							+ "] sending message");
				}
			}

	        // send the message
	        sender.send(msg);

	        // perform commit
	        if (isJmsTransacted() && !(messagingSource.isXaEnabledForSure() && JtaUtil.inTransaction())) {
	            session.commit();
	            log.debug(getLogPrefix()+ "committing (send) transaction");
	        }

	        return msg;

	 	} catch (Exception e) {
			throw new IfsaException(e);
		}
	}

	/**
	 * Intended for server-side reponse sending and implies that the received
	 * message *always* contains a reply-to address.
	 */
	public void sendReply(QueueSession session, Message received_message, String response) throws IfsaException {
		QueueSender tqs=null;
	    try {
	        TextMessage answer = session.createTextMessage();
	        answer.setText(response);
			Queue replyQueue = (Queue)received_message.getJMSReplyTo();
	        tqs = session.createSender(replyQueue );
	        if (log.isDebugEnabled()) log.debug(getLogPrefix()+ "sending reply to ["+ received_message.getJMSReplyTo()+ "]");
	        ((IFSAServerQueueSender) tqs).sendReply(received_message, answer);
	    } catch (Throwable t) {
	        throw new IfsaException(t);
	    } finally {
	    	if (tqs!=null) {
				try {
					tqs.close();
				} catch (JMSException e) {
					log.warn(getLogPrefix()+ "exception closing reply queue sender",e);
				}
	    	}
	    }
	}

    /**
     * Protocol of the IFSA-Service to be called.
     * When the protocol equals to <code>FF</code>, transacted is set to true.
     * @see IfsaMessageProtocolEnum
     */
    public void setMessageProtocol(String newMessageProtocol) {
        messageProtocol = IfsaMessageProtocolEnum.getEnum(newMessageProtocol);
        log.debug(getLogPrefix()+"message protocol set to "+messageProtocol.getLabel());
    }

	public boolean isSessionsArePooled() {
		try {
			return getMessagingSource().sessionsArePooled();
		} catch (IfsaException e) {
			log.error(getLogPrefix()+"could not get session",e);
			return false;
		}
	}

    /**
     * controls whether sessions are created in JMS transacted mode. JMS transacted sessions
     * are required by IFSA for FF, although they result in log messages about active transactions
     * that should be present.
     */
    protected boolean isJmsTransacted() {
		return getMessageProtocolEnum() == IfsaMessageProtocolEnum.FIRE_AND_FORGET;
    }

	@Override
	public String toString() {
	    String result = super.toString();
	    ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("applicationId", applicationId);
	    ts.append("serviceId", serviceId);
	    if (messageProtocol != null) {
			ts.append("messageProtocol", messageProtocol.getLabel());
//			ts.append("transacted", isTransacted());
			ts.append("jmsTransacted", isJmsTransacted());
	    }
	    else
	        ts.append("messageProtocol", "null!");

	    result += ts.toString();
	    return result;

	}

	@Override
	public String getPhysicalDestinationName() {

		String result = null;

		try {
			if (isRequestor()) {
				result = getServiceId();
			} else {
				result = getApplicationId();
			}
			log.debug("obtaining connection and servicequeue for "+result);
			if (getMessagingSource()!=null && getServiceQueue() != null) {
				result += " ["+ getServiceQueue().getQueueName()+"]";
			}
		} catch (Throwable t) {
			log.warn(getLogPrefix()+"got exception in getPhysicalDestinationName", t);
		}
		try {
			result+=" on "+getMessagingSource().getPhysicalName();
		} catch (Exception e) {
			log.warn("[" + name + "] got exception in messagingSource.getPhysicalName", e);
		}
		return result;
	}


	/**
	 * set the IFSA service Id, for requesters only
	 * @param newServiceId the name of the service, e.g. IFSA://SERVICE/CLAIMINFORMATIONMANAGEMENT/NLDFLT/FINDCLAIM:01
	 */
	public void setServiceId(String newServiceId) {
		serviceId = newServiceId;
	}

	public String getServiceId() {
		if (polishedServiceId==null && serviceId!=null) {
			try {
				IfsaMessagingSource messagingSource = getMessagingSource();
				polishedServiceId = messagingSource.polishServiceId(serviceId);
			} catch (IfsaException e) {
				log.warn("could not obtain connection, no polishing of serviceId",e);
				polishedServiceId = serviceId;
			}
		}
		return polishedServiceId;
	}

	/** the ApplicationID, in the form of "IFSA://<i>AppId</i>" */
	public void setApplicationId(String newApplicationId) {
		applicationId = newApplicationId;
	}
	public String getApplicationId() {
		return applicationId;
	}

	protected synchronized boolean useSelectorsForProviders() {
		if (useSelectorsStore==null) {
			boolean pooled=AppConstants.getInstance().getBoolean(USE_SELECTOR_FOR_PROVIDER_KEY, true);
			useSelectorsStore = new Boolean(pooled);
		}
		return useSelectorsStore.booleanValue();
	}

	@Override
	public void setName(String newName) {
		name = newName;
	}
	@Override
	public String getName() {
		return name;
	}

	/** The receive timeout in milliseconds. To use the timeout defined as IFSA expiry, set this value to -1
	 * @ff.default 20000 */
	public void setTimeOut(long timeOut) {
		this.timeOut = timeOut;
	}
	public long getTimeOut() {
		return timeOut;
	}

	public void setAckMode(int ackMode) {
		this.ackMode = ackMode;
	}
	public int getAckMode() {
		return ackMode;
	}

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
}
