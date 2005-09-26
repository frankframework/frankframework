/*
 * $Log: IfsaProviderListener.java,v $
 * Revision 1.14  2005-09-26 11:47:26  europe\L190409
 * Jms-commit only if not XA-transacted
 * ifsa-messageWrapper for (non-serializable) ifsa-messages
 *
 * Revision 1.13  2005/09/13 15:48:27  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed acknowledge mode back to AutoAcknowledge
 *
 * Revision 1.12  2005/07/28 07:31:54  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * change default acknowledge mode to CLIENT
 *
 * Revision 1.11  2005/06/20 09:14:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid excessive logging
 *
 * Revision 1.10  2005/06/13 15:08:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid excessive logging in debug mode
 *
 * Revision 1.9  2005/06/13 12:43:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for pooled sessions and for XA-support
 *
 * Revision 1.8  2005/02/17 09:45:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * increased logging
 *
 * Revision 1.7  2005/01/13 08:55:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Make threadContext-attributes available in PipeLineSession
 *
 * Revision 1.6  2004/09/22 07:03:36  Johan Verrips <johan.verrips@ibissource.org>
 * Added logstatements for closing receiver and session
 *
 * Revision 1.5  2004/09/22 06:48:08  Johan Verrips <johan.verrips@ibissource.org>
 * Changed loglevel in getStringFromRawMessage to warn
 *
 * Revision 1.4  2004/07/19 09:50:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * try to send exceptionmessage as reply when sending reply results in exception
 *
 * Revision 1.3  2004/07/15 07:43:04  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.2  2004/07/08 12:55:57  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * logging refinements
 *
 * Revision 1.1  2004/07/05 14:28:38  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * First version, converted from IfsaServiceListener
 *
 * Revision 1.4  2004/03/26 07:25:42  Johan Verrips <johan.verrips@ibissource.org>
 * Updated erorhandling
 *
 * Revision 1.3  2004/03/24 15:27:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * solved uncaught exception in error message
 *
 */
package nl.nn.adapterframework.extensions.ifsa;

import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.core.INamedObject;

import com.ing.ifsa.IFSAMessage;
import com.ing.ifsa.IFSAPoisonMessage;
import com.ing.ifsa.IFSAHeader;
import com.ing.ifsa.IFSAServiceName;
import com.ing.ifsa.IFSAServicesProvided;
import com.ing.ifsa.IFSATextMessage;

import java.util.HashMap;
import java.util.Date;
import java.util.Iterator;

import javax.jms.Message;
import javax.jms.QueueSession;
import javax.jms.QueueReceiver;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.TextMessage;
import javax.jms.JMSException;


import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Implementation of {@link IPullingListener} that acts as an IFSA-service.
 * 
 * There is no need or possibility to set the ServiceId as the Provider will receive all messages
 * for this Application on the same serviceQueue.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.ifsa.IfsaProviderListener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the object</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setApplicationId(String) applicationId}</td><td>the ApplicationID, in the form of "IFSA://<i>AppId</i>"</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of IFSA-Service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td>&nbsp;</td></tr>
 * </table>
 * @author Gerrit van Brakel
 * @since 4.2
 */
public class IfsaProviderListener extends IfsaFacade implements IPullingListener, INamedObject {
	public static final String version = "$RCSfile: IfsaProviderListener.java,v $ $Revision: 1.14 $ $Date: 2005-09-26 11:47:26 $";

    private final static String THREAD_CONTEXT_SESSION_KEY = "session";
    private final static String THREAD_CONTEXT_RECEIVER_KEY = "receiver";

    private String commitOnState;
    private long timeOut = 3000;

	public IfsaProviderListener() {
		super(true); //instantiate as a provider
	}

	public void open() throws ListenerException {
		try {
			openService();
			
			IFSAServicesProvided services = getServiceQueue().getIFSAServicesProvided();

			for (int i = 0; i < services.getNumberOfServices(); i++) {
				IFSAServiceName service = services.getService(i);
				
				String protocol=(service.IsFireAndForgetService() ? "Fire and Forget" : "Request/Reply");
				log.info(getLogPrefix()+"providing ServiceName ["+service.getServiceName()+"] ServiceGroup [" + service.getServiceGroup()+"] protocol [" + protocol+"] ServiceVersion [" + service.getServiceVersion()+"]");				
			}
		} catch (Exception e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}
	
	protected QueueSession getSession(HashMap threadContext) throws ListenerException {
		if (isSessionsArePooled()) {
			try {
				return createSession();
			} catch (IfsaException e) {
				throw new ListenerException(getLogPrefix()+"exception creating QueueSession", e);
			}
		} else {
			return (QueueSession) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
		}
	}
	
	protected void releaseSession(QueueSession session) throws ListenerException {
		if (isSessionsArePooled() && session != null) {
			try {
				session.close();
				// do not write to log, this occurs too often
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"exception closing QueueSession", e);
			}
		}
	}

	protected QueueReceiver getReceiver(HashMap threadContext, QueueSession session) throws ListenerException {
		if (isSessionsArePooled()) {
			try {
				return getServiceReceiver(session);
			} catch (IfsaException e) {
				throw new ListenerException(getLogPrefix()+"exception creating QueueReceiver", e);
			}
		} else {
			return (QueueReceiver) threadContext.get(THREAD_CONTEXT_RECEIVER_KEY);
		}
	}
	
	protected void releaseReceiver(QueueReceiver receiver) throws ListenerException {
		if (isSessionsArePooled() && receiver != null) {
			try {
				receiver.close();
				// do not write to log, this occurs too often
			} catch (Exception e) {
				throw new ListenerException(getLogPrefix()+"exception closing QueueReceiver", e);
			}
		}
	}
	
	
	
	public HashMap openThread() throws ListenerException {
		HashMap threadContext = new HashMap();
	
		try {
			if (!isSessionsArePooled()) {
				QueueSession session = createSession();
				threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
	
				QueueReceiver receiver;
				receiver = getServiceReceiver(session);
				threadContext.put(THREAD_CONTEXT_RECEIVER_KEY, receiver);
			}
			return threadContext;
		} catch (IfsaException e) {
			throw new ListenerException(getLogPrefix()+"exception in openThread()", e);
		}
	}

	public void close() throws ListenerException {
		try {
			closeService();
		} catch (IfsaException e) {
			throw new ListenerException(getLogPrefix(),e);
		}
	}
	
	public void closeThread(HashMap threadContext) throws ListenerException {
	
		if (!isSessionsArePooled()) {
			QueueReceiver receiver = (QueueReceiver) threadContext.remove(THREAD_CONTEXT_RECEIVER_KEY);
			releaseReceiver(receiver);
	
			QueueSession session = (QueueSession) threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
			releaseSession(session);
		}
	}


	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, HashMap threadContext)
    throws ListenerException {
	
	    String cid = (String) threadContext.get("cid");
	    
		    
		if (isJmsTransacted() && !isTransacted()) {
			QueueSession session = (QueueSession) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
	    
		    /* 
		     * Message are only committed in the Fire & Forget scenario when the outcome
		     * of the adapter equals the getCommitOnResult value
		     */
	    	log.debug("PipeLineResult : "+plr.toString());
	    	log.debug(getCommitOnState());
	    	
	        if (getCommitOnState().equals(plr.getState())) {
				try {
					session.commit();
				} catch (JMSException e) {
					log.error(getLogPrefix()+"got error committing the received message", e);
				}
	        } else {
	            log.warn(getLogPrefix()+"message with correlationID ["
	                    + cid
	                    + " message ["
	                    + getStringFromRawMessage(rawMessage, threadContext)
	                    + "]"
	                    + " is NOT committed. The result-state of the adapter is ["
	                    + plr.getState()
	                    + "] while the state for committing is set to ["
	                    + getCommitOnState()
	                    + "]");
	        }
	        if (isSessionsArePooled()) {
				threadContext.remove(THREAD_CONTEXT_SESSION_KEY);
				releaseSession(session);
	        }
	    }
	    // on request-reply send the reply. 
	    if (getMessageProtocolEnum().equals(IfsaMessageProtocolEnum.REQUEST_REPLY)) {
			QueueSession session = getSession(threadContext);
	        try {
	            sendReply(session, (Message) rawMessage, plr.getResult());
	        } catch (IfsaException e) {
	        	try {
					sendReply(session, (Message) rawMessage, "<exception>"+e.getMessage()+"</exception>");
	        	} catch (IfsaException e2) {
	        		log.warn(getLogPrefix()+"exception sending errormessage as reply",e2);
	        	}
	            throw new ListenerException(getLogPrefix()+"Exception on sending result", e);
	        } finally {
				releaseSession(session);
	        }
	    }
	}
	

	protected String getIdFromWrapper(IfsaMessageWrapper wrapper, HashMap threadContext)  {
		for (Iterator it=wrapper.getContext().keySet().iterator(); it.hasNext();) {
			String key = (String)it.next();
			Object value = wrapper.getContext().get(key);
			log.debug(getLogPrefix()+"setting variable ["+key+"] to ["+value+"]");
			threadContext.put(key, value);
		}
		return wrapper.getId();
	}
	protected String getStringFromWrapper(IfsaMessageWrapper wrapper, HashMap threadContext)  {
		return wrapper.getText();
	}

	

	
	/**
	 * Extracts ID-string from message obtained from {@link #getRawMessage(HashMap)}. 
	 * Puts also the following parameters  in the threadContext:
	 * <ul>
	 * <li>id</li>
	 * <li>cid</li>
	 * <li>timestamp</li>
	 * <li>replyTo</li>
	 * <li>messageText</li>
	 * <li>serviceDestination</li>
	 * </ul>
	 * @return ID-string of message for adapter.
	 */
	public String getIdFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
	
		TextMessage message = null;
	 
	 	if (rawMessage instanceof IfsaMessageWrapper) {
	 		return getIdFromWrapper((IfsaMessageWrapper)rawMessage,threadContext);
	 	}
	 
	    try {
	        message = (TextMessage) rawMessage;
	    } catch (ClassCastException e) {
	        log.error(getLogPrefix()+
	            "message received was not of type TextMessage, but ["
	                + rawMessage.getClass().getName()
	                + "]",
	            e);
	        return null;
	    }
	    String mode = "unknown";
	    String id = "unset";
	    String cid = "unset";
	    Date dTimeStamp = null;
	    Destination replyTo = null;
	    String messageText = null;
		String fullIfsaServiceName = null;
	    IFSAServiceName requestedService = null;
	    String ifsaServiceName=null, ifsaGroup=null, ifsaOccurrence=null, ifsaVersion=null;
	    try {
	        if (message.getJMSDeliveryMode() == DeliveryMode.NON_PERSISTENT) {
	            mode = "NON_PERSISTENT";
	        } else
	            if (message.getJMSDeliveryMode() == DeliveryMode.PERSISTENT) {
	                mode = "PERSISTENT";
	            }
	    } catch (JMSException ignore) {
	    }
	    // --------------------------
	    // retrieve MessageID
	    // --------------------------
	    try {
	        id = message.getJMSMessageID();
	    } catch (JMSException ignore) {
	    }
	    // --------------------------
	    // retrieve CorrelationID
	    // --------------------------
	    try {
	        cid = message.getJMSCorrelationID();
	        if (cid == null) {
	            cid = id;
	            log.debug("Setting correlation ID to MessageId");
	        }
	    } catch (JMSException ignore) {
	    }
	    // --------------------------
	    // retrieve TimeStamp
	    // --------------------------
	    try {
	        long lTimeStamp = message.getJMSTimestamp();
	        dTimeStamp = new Date(lTimeStamp);
	
	    } catch (JMSException ignore) {
	    }
	    // --------------------------
	    // retrieve ReplyTo address
	    // --------------------------
	    try {
	        replyTo = message.getJMSReplyTo();
	
	    } catch (JMSException ignore) {
	    }
	    // --------------------------
	    // retrieve message text
	    // --------------------------
	    try {
	        messageText = message.getText();
	    } catch (JMSException ignore) {
	    }
	    // --------------------------
	    // retrieve ifsaServiceDestination
	    // --------------------------
	    try {
			fullIfsaServiceName = ((IFSAMessage) message).getServiceString();
			requestedService = ((IFSAMessage) message).getService();
			
			ifsaServiceName = requestedService.getServiceName();
			ifsaGroup = requestedService.getServiceGroup();
			ifsaOccurrence = requestedService.getServiceOccurance();
			ifsaVersion = requestedService.getServiceVersion();
			
	    } catch (JMSException e) {
	        log.error(getLogPrefix() + "got error getting serviceparameter", e);
	    }

/*
		// acknowledge the message when necessary
		try {
			if (!isTransacted() && !isJmsTransacted()) {
				message.acknowledge();
				log.debug(getLogPrefix()+"acknowledged received message");
			}
		} catch (JMSException e) {
			log.error(getLogPrefix()+"exception in ack ", e);
		}
*/
		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+ "got message for [" + fullIfsaServiceName
					+ "] with JMSDeliveryMode=[" + mode
					+ "] \n  JMSMessageID=[" + id
					+ "] \n  JMSCorrelationID=["+ cid
					+ "] \n  ifsaServiceName=["+ ifsaServiceName
					+ "] \n  ifsaGroup=["+ ifsaGroup
					+ "] \n  ifsaOccurrence=["+ ifsaOccurrence
					+ "] \n  ifsaVersion=["+ ifsaVersion
					+ "] \n  Timestamp=[" + dTimeStamp.toString()
					+ "] \n  ReplyTo=[" + ((replyTo == null) ? "none" : replyTo.toString())
					+ "] \n  Message=[" + message.toString()
					+ "]");
		}
	
	    threadContext.put("id", id);
	    threadContext.put("cid", cid);
	    threadContext.put("timestamp", dTimeStamp);
	    threadContext.put("replyTo", replyTo);
	    threadContext.put("messageText", messageText);
	    threadContext.put("fullIfsaServiceName", fullIfsaServiceName);
	    threadContext.put("ifsaServiceName", ifsaServiceName);
	    threadContext.put("ifsaGroup", ifsaGroup);
	    threadContext.put("ifsaOccurrence", ifsaOccurrence);
	    threadContext.put("ifsaVersion", ifsaVersion);
	    return id;
	}
	
	private boolean sessionNeedsToBeSavedForAfterProcessMessage(Object result)
	{
		return isJmsTransacted() &&
			   	!isTransacted() && 
				isSessionsArePooled()&&
				result != null && 
				!(result instanceof IFSAPoisonMessage) ;
	}
	
	/**
	 * Retrieves messages to be processed by the server, implementing an IFSA-service, but does no processing on it.
	 */
	public Object getRawMessage(HashMap threadContext) throws ListenerException {
		Object result=null;
		QueueSession session=null;
		QueueReceiver receiver=null;
	    try {	
			session = getSession(threadContext);
			try {	
				receiver = getReceiver(threadContext, session);
		        result = receiver.receive(getTimeOut());
			} catch (JMSException e) {
				throw new ListenerException(getLogPrefix(),e);
		    } finally {
		    	releaseReceiver(receiver);
			}
		} finally {
			if (sessionNeedsToBeSavedForAfterProcessMessage(result)) {
				threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
			} else {
				releaseSession(session);
			}
	    }
	    
	    if (result instanceof IFSAPoisonMessage) {
	        IFSAHeader header = ((IFSAPoisonMessage) result).getIFSAHeader();
	        String source;
	        try {
	        	source = header.getIFSA_Source();
	        } catch (Exception e) {
	        	source = "unknown due to exeption:"+e.getMessage();
	        }
	        log.error(getLogPrefix()+ "received IFSAPoisonMessage "
	                + "source ["
	                + source
	                + "]"
	                + "content ["
	                + ToStringBuilder.reflectionToString((IFSAPoisonMessage) result)
	                + "]");
	    }
	    if (isTransacted() && result instanceof IFSATextMessage) {
	    	result = new IfsaMessageWrapper(result, this);
	    }
	    return result;
	}
	/**
	 * Extracts string from message obtained from {@link #getRawMessage(HashMap)}. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return input message for adapter.
	 */
	public String getStringFromRawMessage(Object rawMessage, HashMap threadContext) throws ListenerException {
		if (rawMessage instanceof IfsaMessageWrapper) {
			return getStringFromWrapper((IfsaMessageWrapper)rawMessage,threadContext);
		}

	    TextMessage message = null;
	    try {
	        message = (TextMessage) rawMessage;
	    } catch (ClassCastException e) {
	        log.warn(getLogPrefix()+ "message received  was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
	        return null;
	    }
	    try {
	    	return message.getText();
	    } catch (JMSException e) {
		    throw new ListenerException(getLogPrefix(),e);
	    }
	}
	
	public String getCommitOnState() {
		return commitOnState;
	}
	public void setCommitOnState(String newCommitOnState) {
		commitOnState = newCommitOnState;
	}

	public long getTimeOut() {
		return timeOut;
	}
	public void setTimeOut(long newTimeOut) {
		timeOut = newTimeOut;
	}
}
