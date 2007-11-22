/*
 * Created on 18-sep-07
 * 
 * $Log: PushingJmsListener.java,v $
 * Revision 1.5  2007-11-22 09:08:56  europe\L190409
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
import javax.naming.NamingException;

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
 * @author Tim van der Leeuw
 * @since 4.8
 * 
 * JMSListener re-implemented as a pushing listener rather than a pulling listener.
 * The JMS messages have to come in from an external source: an MDB or a Spring
 * message container.
 * 
 * Configuration is same as JmsListener / PullingJmsListener.
 * 
 */
public class PushingJmsListener extends JMSFacade implements IPortConnectedListener {
    public static final String version="$RCSfile: PushingJmsListener.java,v $ $Revision: 1.5 $ $Date: 2007-11-22 09:08:56 $";

    private final static String THREAD_CONTEXT_SESSION_KEY="session";
    private final static String THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY="isSessionOwner";
    
    private long timeOut = 3000;
    private boolean useReplyTo=true;
    private String replyMessageType=null;
    private long replyMessageTimeToLive=0;
    private int replyPriority=-1;
    private String replyDeliveryMode=MODE_NON_PERSISTENT;
    private ISender sender;
    private String listenerPort;
    
    private boolean forceMessageIdAsCorrelationId=false;
 
    private String commitOnState="success";

    
    private IListenerConnector jmsConnector;
    private IMessageHandler handler;
    private IReceiver receiver;
    private IbisExceptionListener exceptionListener;
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPushingListener#setHandler(nl.nn.adapterframework.core.IMessageHandler)
     */
    public void setHandler(IMessageHandler handler) {
        this.handler = handler;
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IPushingListener#setExceptionListener(nl.nn.adapterframework.core.IbisExceptionListener)
     */
    public void setExceptionListener(IbisExceptionListener listener) {
        this.exceptionListener = listener;
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#configure()
     */
    public void configure() throws ConfigurationException {
        super.configure();
        ISender sender = getSender();
        if (sender != null) {
            sender.configure();
        }
        jmsConnector.configureEndpointConnection(this);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#open()
     */
    public void open() throws ListenerException {
        // DO NOT open JMSFacade!
        jmsConnector.start();
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#close()
     */
    public void close() throws ListenerException {
        try {
            // DO close JMSFacade - it might have been opened via other calls
            jmsConnector.stop();
            closeFacade();
        } catch (JmsException ex) {
            throw new ListenerException(ex);
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
    public void populateThreadContext(Object rawMessage, Map threadContext, Session session) throws ListenerException {
        if (session != null) {
            threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
            threadContext.put(THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY, Boolean.FALSE);
        }
        TextMessage message = null;
        try {
            message = (TextMessage) rawMessage;
        } catch (ClassCastException e) {
            log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
            return;
        }
        String mode = "unknown";
        String cid = "unset";
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
    }
    
    /**
     * Perform any required cleanups on the thread context, such as closing
     * a JMS Session if the session is owned by the JMS Listener.
     * 
     * @param threadContext
     */
    public void destroyThreadContext(Map threadContext) {
        // Do we have a session in the thread-context, and do we need to close it?
        if (threadContext.containsKey(THREAD_CONTEXT_SESSION_KEY)) {
            Boolean isSessionOwner = (Boolean) threadContext.get(THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY);
            if (isSessionOwner.booleanValue()) {
                Session session = (Session) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
                super.closeSession(session);
            }
        }
        
        // No other cleanups yet
    }
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#getIdFromRawMessage(java.lang.Object, java.util.Map)
     */
    public String getIdFromRawMessage(Object rawMessage, Map threadContext)
        throws ListenerException {
        TextMessage message = null;
        try {
            message = (TextMessage) rawMessage;
        } catch (ClassCastException e) {
            log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
            return null;
        }
        String cid = "unset";
        String id = "unset";
        
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
        return cid;
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#getStringFromRawMessage(java.lang.Object, java.util.Map)
     */
    public String getStringFromRawMessage(Object rawMessage, Map threadContext)
        throws ListenerException {
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

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#afterMessageProcessed(nl.nn.adapterframework.core.PipeLineResult, java.lang.Object, java.util.Map)
     */
    public void afterMessageProcessed(
        PipeLineResult pipeLineResult,
        Object rawMessage,
        Map threadContext)
        throws ListenerException {
        String cid = (String) threadContext.get("cid");

        try {
            Destination replyTo = (Destination) threadContext.get("replyTo");

            // handle reply
            if (getUseReplyTo() && (replyTo != null)) {
                Session session=null;
            

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
                if (threadContext!=null) {
                    session = getSessionFromThreadContext(threadContext);
                }
                send(session, replyTo, cid, pipeLineResult.getResult(), getReplyMessageType(), timeToLive, stringToDeliveryMode(getReplyDeliveryMode()), getReplyPriority()); 
            } else {
                if (sender==null) {
                    log.info("["+getName()+"] has no sender, not sending the result.");
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug(
                            "["+getName()+"] no replyTo address found or not configured to use replyTo, using default destination" 
                            + "sending message with correlationID[" + cid + "] [" + pipeLineResult.getResult() + "]");
                    }
                    sender.sendMessage(cid, pipeLineResult.getResult());
                }
            }
        
            // handle transaction details
            if (!isTransacted()) {
                if (isJmsTransacted()) {
                    // the following if transacted using transacted sessions, instead of XA-enabled sessions.
                    Session session;
                    session = getSessionFromThreadContext(threadContext);
                    if (session == null) {
                        log.warn("Listener ["+getName()+"] message ["+ (String)threadContext.get("id") +"] has no session to commit or rollback");
                    } else {
                        String successState = getCommitOnState();
                        if (successState!=null && successState.equals(pipeLineResult.getState())) {
                            session.commit();
                        } else {
                            log.warn("Listener ["+getName()+"] message ["+ (String)threadContext.get("id") +"] not committed nor rolled back either");
                            //TODO: enable rollback, or remove support for JmsTransacted altogether (XA-transactions should do it all)
                            // session.rollback();
                        }
                    }
                } else {
                    // TODO: dit weghalen. Het hoort hier niet, en zit ook al in getIdFromRawMessage. Daar hoort het ook niet, overigens...
                    if (getAckMode() == Session.CLIENT_ACKNOWLEDGE) {
                        log.debug("["+getName()+"] acknowledges message with id ["+cid+"]");
                        ((TextMessage)rawMessage).acknowledge();
                    }
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

    public Destination getDestination() {
        Destination d = getJmsConnector().getDestination();
        return d;
    }

    public Destination getDestination(String destinationName) throws JmsException, NamingException {
        throw new UnsupportedOperationException("PushingJmsListener does not support operation getDestination(destinationName) inherited from parent-class JMSFacade");
    }
    
    
    /**
     * @return
     */
    public IListenerConnector getJmsConnector() {
        return jmsConnector;
    }

    /**
     * @param configurator
     */
    public void setJmsConnector(IListenerConnector configurator) {
        jmsConnector = configurator;
    }

    /**
     * @return
     */
    public IbisExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    /**
     * @return
     */
    public IMessageHandler getHandler() {
        return handler;
    }

    /**
     * @return
     */
    public String getCommitOnState() {
        return commitOnState;
    }

    /**
     * @return
     */
    public boolean isForceMessageIdAsCorrelationId() {
        return forceMessageIdAsCorrelationId;
    }

    /**
     * @return
     */
    public String getReplyDeliveryMode() {
        return replyDeliveryMode;
    }

    /**
     * @return
     */
    public long getReplyMessageTimeToLive() {
        return replyMessageTimeToLive;
    }

    /**
     * @return
     */
    public String getReplyMessageType() {
        return replyMessageType;
    }

    /**
     * @return
     */
    public int getReplyPriority() {
        return replyPriority;
    }

    /**
     * @return
     */
    public ISender getSender() {
        return sender;
    }

    /**
     * @return
     */
    public long getTimeOut() {
        return timeOut;
    }

    /**
     * @return
     */
    public boolean getUseReplyTo() {
        return useReplyTo;
    }

    /**
     * @param string
     */
    public void setCommitOnState(String string) {
        commitOnState = string;
    }

    /**
     * @param b
     */
    public void setForceMessageIdAsCorrelationId(boolean b) {
        forceMessageIdAsCorrelationId = b;
    }

    /**
     * @param string
     */
    public void setReplyDeliveryMode(String string) {
        replyDeliveryMode = string;
    }

    /**
     * @param l
     */
    public void setReplyMessageTimeToLive(long l) {
        replyMessageTimeToLive = l;
    }

    /**
     * @param string
     */
    public void setReplyMessageType(String string) {
        replyMessageType = string;
    }

    /**
     * @param i
     */
    public void setReplyPriority(int i) {
        replyPriority = i;
    }

    /**
     * @param sender
     */
    public void setSender(ISender sender) {
        this.sender = sender;
    }

    /**
     * @param l
     */
    public void setTimeOut(long l) {
        timeOut = l;
    }

    /**
     * @param b
     */
    public void setUseReplyTo(boolean b) {
        useReplyTo = b;
    }

    private Session getSessionFromThreadContext(Map threadContext) throws JmsException {
        Session session;
        if (threadContext.containsKey(THREAD_CONTEXT_SESSION_KEY)) {
            session = (Session) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
        } else {
            session = super.createSession();
            threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
            threadContext.put(THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY, Boolean.TRUE);
        }
        return session;
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

    public String getLogPrefix() {
        return super.getLogPrefix();
    }
    
    public IReceiver getReceiver() {
        return receiver;
    }
    
    public void setReceiver(IReceiver receiver) {
        this.receiver = receiver;
    }

    public IListenerConnector getListenerPortConnector() {
        return jmsConnector;
    }
}
