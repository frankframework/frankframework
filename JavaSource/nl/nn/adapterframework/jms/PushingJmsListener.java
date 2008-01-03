/*
 * $Log: PushingJmsListener.java,v $
 * Revision 1.8  2008-01-03 15:51:56  europe\L190409
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
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;
import javax.jms.TextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IListenerConnector;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPortConnectedListener;
import nl.nn.adapterframework.core.IReceiver;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

/**
 * JMSListener re-implemented as a pushing listener rather than a pulling listener.
 * The JMS messages have to come in from an external source: an MDB or a Spring
 * message container.
 * 
 * Configuration is same as JmsListener / PullingJmsListener.
 * 
 * @author  Tim van der Leeuw
 * @since   4.8
 * @version Id
 */
public class PushingJmsListener extends JMSFacade implements IPortConnectedListener {
    public static final String version="$RCSfile: PushingJmsListener.java,v $ $Revision: 1.8 $ $Date: 2008-01-03 15:51:56 $";

	private final static String THREAD_CONTEXT_SESSION_KEY="session";

    private long timeOut = 3000;
    private boolean useReplyTo=true;
    private String replyMessageType=null;
    private long replyMessageTimeToLive=0;
    private int replyPriority=-1;
    private String replyDeliveryMode=MODE_NON_PERSISTENT;
    private ISender sender;
    
    private boolean forceMessageIdAsCorrelationId=false;
 
    private String commitOnState="success";

	private String listenerPort;
	private String cacheMode = "CACHE_CONSUMER"; // set to CACHE_NONE if multiple JmsListeners appear in one chain, to avoid lock up / timeout in session creation
    
    private IListenerConnector jmsConnector;
    private IMessageHandler handler;
    private IReceiver receiver;
    private IbisExceptionListener exceptionListener;
    
    

    public void configure() throws ConfigurationException {
        super.configure();
        ISender sender = getSender();
        if (sender != null) {
            sender.configure();
        }
        if (jmsConnector==null) {
        	throw new ConfigurationException(getLogPrefix()+" has no jmsConnector. It should be configured via springContext.xml");
        }
		Destination destination=null;
        try {
			destination=getDestination();
		} catch (Exception e) {
			throw new ConfigurationException(getLogPrefix()+"could not get Destination",e);
		}
        try {
			jmsConnector.configureEndpointConnection(this, getConnection().getConnectionFactory(), destination, getExceptionListener(), getCacheMode(), false, getMessageSelector());
		} catch (JmsException e) {
			throw new ConfigurationException(e);
		}
    }

    public void open() throws ListenerException {
        // DO NOT open JMSFacade!
        jmsConnector.start();
    }

    public void close() throws ListenerException {
        try {
            // DO close JMSFacade - it might have been opened via other calls
            jmsConnector.stop();
            closeFacade();
			if (sender != null) {
				sender.close();
			}
        } catch (Exception ex) {
            throw new ListenerException(ex);
        }
    }
 

	public void afterMessageProcessed(PipeLineResult plr, Object rawMessage, Map threadContext) throws ListenerException {
		String cid     = (String) threadContext.get("cid");
		Session session= (Session) threadContext.get(THREAD_CONTEXT_SESSION_KEY);

		try {
			Destination replyTo = (Destination) threadContext.get("replyTo");

			// handle reply
			if (getUseReplyTo() && (replyTo != null)) {

				log.debug("sending reply message with correlationID[" + cid + "], replyTo [" + replyTo.toString()+ "]");
				long timeToLive = getReplyMessageTimeToLive();
				if (timeToLive == 0) {
					Message messageSent=(Message)rawMessage;
					long expiration=messageSent.getJMSExpiration();
					if (expiration!=0) {
						timeToLive=expiration-new Date().getTime();
						if (timeToLive<=0) {
							log.warn("message ["+cid+"] expired ["+timeToLive+"]ms, sending response with 1 second time to live");
							timeToLive=1000;
						}
					}
				}
				send(session, replyTo, cid, plr.getResult(), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority()); 
			} else {
				if (sender==null) {
					log.info("["+getName()+"] has no sender, not sending the result.");
				} else {
					if (log.isDebugEnabled()) {
						log.debug(
							"["+getName()+"] no replyTo address found or not configured to use replyTo, using default destination" 
							+ "sending message with correlationID[" + cid + "] [" + plr.getResult() + "]");
					}
					sender.sendMessage(cid, plr.getResult());
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
   
    /**
     * Fill in thread-context with things needed by the JMSListener code.
     * This includes a Session. The Session object can be passed in
     * externally.
     * 
     * @param rawMessage - Original message received, can not be <code>null</code>
     * @param threadContext - Thread context to be populated, can not be <code>null</code>
     * @param session - JMS Session under which message was received; can be <code>null</code>
     */
	public String getIdFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
        TextMessage message = null;
		String cid = "unset";
        try {
            message = (TextMessage) rawMessage;
        } catch (ClassCastException e) {
            log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
            return null;
        }
        String mode = "unknown";
        String id = "unset";
        Date dTimeStamp = null;
        Destination replyTo=null;
        try {
            mode = deliveryModeToString(message.getJMSDeliveryMode());
        } catch (JMSException ignore) {
            log.debug("ignoring JMSException in getJMSDeliveryMode()", ignore);
        }
        // --------------------------
        // retrieve MessageID
        // --------------------------
        try {
            id = message.getJMSMessageID();
        } catch (JMSException ignore) {
            log.debug("ignoring JMSException in getJMSMessageID()", ignore);
        }
        // --------------------------
        // retrieve CorrelationID
        // --------------------------
        try {
            if (isForceMessageIdAsCorrelationId()){
                if (log.isDebugEnabled()) log.debug("forcing the messageID to be the correlationID");
                cid =id;
            }
            else {
                cid = message.getJMSCorrelationID();
                if (cid==null) {
                  cid = id;
                  log.debug("Setting correlation ID to MessageId");
                }
            }
        } catch (JMSException ignore) {
            log.debug("ignoring JMSException in getJMSCorrelationID()", ignore);
        }
        // --------------------------
        // retrieve TimeStamp
        // --------------------------
        try {
            long lTimeStamp = message.getJMSTimestamp();
            dTimeStamp = new Date(lTimeStamp);

        } catch (JMSException ignore) {
            log.debug("ignoring JMSException in getJMSTimestamp()", ignore);
        }
        // --------------------------
        // retrieve ReplyTo address
        // --------------------------
        try {
            replyTo = message.getJMSReplyTo();

        } catch (JMSException ignore) {
            log.debug("ignoring JMSException in getJMSReplyTo()", ignore);
        }

        log.info(
            "listener on ["
                + getDestinationName()
                + "] got message with JMSDeliveryMode=["
                + mode
                + "] \n  JMSMessageID=["
                + id
                + "] \n  JMSCorrelationID=["
                + cid
                + "] \n  Timestamp=["
                + dTimeStamp.toString()
                + "] \n  ReplyTo=["
                + ((replyTo==null)?"none" : replyTo.toString())
                + "] \n Message=["
                + message.toString()
                + "]");
    
        threadContext.put("id",id);
        threadContext.put("cid",cid);
        threadContext.put("timestamp",dTimeStamp);
        threadContext.put("replyTo",replyTo);
        try {
            if (getAckMode() == Session.CLIENT_ACKNOWLEDGE) {
                message.acknowledge();
                log.debug("Listener on [" + getDestinationName() + "] acknowledged message");
            }
        } catch (JMSException e) {
            log.error("Warning in ack", e);
        }
        return cid;
    }

  
    

	/**
	 * Extracts string from message obtained from {@link #getRawMessage(Map)}. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return String  input message for adapter.
	 */
	public String getStringFromRawMessage(Object rawMessage, Map threadContext) throws ListenerException {
        TextMessage message = null;
        try {
            message = (TextMessage) rawMessage;
        } catch (ClassCastException e) {
            log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
            return null;
        }
        try {
            return message.getText();
        } catch (JMSException e) {
            throw new ListenerException(e);
        }
    }

	public void setSender(ISender newSender) {
		sender = newSender;
			log.debug("["+getName()+"] ** registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    
	}
	public ISender getSender() {
		return sender;
	}

	/**
	 * By default, the JmsListener takes the Correlation ID (if present) as the ID that has to be put in the
	 * correlation id of the reply. When you set ForceMessageIdAsCorrelationId to <code>true</code>,
	 * the messageID set in the correlationID of the reply.
	 * @param force
	 */
	public void setForceMessageIdAsCorrelationId(boolean force){
	   forceMessageIdAsCorrelationId=force;
	}
	public boolean isForceMessageIdAsCorrelationId(){
	  return forceMessageIdAsCorrelationId;
	}

	/**
	 * Controls when the JmsListener will commit it's local transacted session, that is created when
	 * jmsTransacted = <code>true</code>. This is probably not what you want. 
	 * @deprecated consider using XA transactions, controled by the <code>transacted</code>-attribute, rather than
	 * local transactions controlled by the <code>jmsTransacted</code>-attribute.
	 */
	public void setCommitOnState(String newCommitOnState) {
		commitOnState = newCommitOnState;
	}
	public String getCommitOnState() {
		return commitOnState;
	}

	public void setTimeOut(long newTimeOut) {
		timeOut = newTimeOut;
	}
	public long getTimeOut() {
		return timeOut;
	}


	public void setUseReplyTo(boolean newUseReplyTo) {
		useReplyTo = newUseReplyTo;
	}
	public boolean getUseReplyTo() {
		return useReplyTo;
	}

	
	public void setReplyMessageType(String string) {
		replyMessageType = string;
	}
	public String getReplyMessageType() {
		return replyMessageType;
	}


	public void setReplyDeliveryMode(String string) {
		replyDeliveryMode = string;
	}
	public String getReplyDeliveryMode() {
		return replyDeliveryMode;
	}


	public void setReplyPriority(int i) {
		replyPriority = i;
	}
	public int getReplyPriority() {
		return replyPriority;
	}


	public void setReplyMessageTimeToLive(long l) {
		replyMessageTimeToLive = l;
	}
	public long getReplyMessageTimeToLive() {
		return replyMessageTimeToLive;
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

}
