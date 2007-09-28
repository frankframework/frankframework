/*
 * Created on 18-sep-07
 * 
 * $Log: JmsListener.java,v $
 * Revision 1.25.4.4  2007-09-28 14:20:26  europe\M00035F
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
import nl.nn.adapterframework.configuration.IJmsConfigurator;
import nl.nn.adapterframework.core.IMessageHandler;
import nl.nn.adapterframework.core.IPushingListener;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IbisExceptionListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;

/**
 * @author m00035f
 * 
 * JMSListener re-implemented as a pushing listener rather than a pulling listener.
 * The JMS messages have to come in from an external source: an MDB or a Spring
 * message container.
 * 
 */
public class JmsListener extends JMSFacade implements IPushingListener {
    public static final String version="$RCSfile: JmsListener.java,v $ $Revision: 1.25.4.4 $ $Date: 2007-09-28 14:20:26 $";

    private final static String THREAD_CONTEXT_SESSION_KEY="session";
    private final static String THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY="isSessionOwner";
    
    private long timeOut = 3000;
    private boolean useReplyTo=true;
    private String replyMessageType=null;
    private long replyMessageTimeToLive=0;
    private int replyPriority=-1;
    private String replyDeliveryMode=MODE_NON_PERSISTENT;
    private ISender sender;
        
    private boolean forceMessageIdAsCorrelationId=false;
 
    private String commitOnState="success";

    
    private IJmsConfigurator jmsConfigurator;
    private IMessageHandler handler;
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
        jmsConfigurator.configureJmsReceiver(this);
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#open()
     */
    public void open() throws ListenerException {
        // DO NOT open JMSFacade!
        jmsConfigurator.openJmsReceiver();
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#close()
     */
    public void close() throws ListenerException {
        try {
            // DO close JMSFacade - it might have been opened via other calls
            jmsConfigurator.closeJmsReceiver();
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
     * TODO: What if Session passed in is <code>null</code>?
     * 
     * @param threadContext
     * @param session
     */
    public void populateThreadContext(Map threadContext, Session session) throws JmsException {
        boolean isSessionOwner = (session == null);
        if (session == null) {
            session = super.createSession();
        }
        threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
        threadContext.put(THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY, Boolean.valueOf(isSessionOwner));
    }
    
    public void destroyThreadContext(Map threadContext) {
        Boolean isSessionOwner = (Boolean) threadContext.get(THREAD_CONTEXT_SESSION_OWNER_FLAG_KEY);
        if (isSessionOwner.booleanValue()) {
            Session session = (Session) threadContext.get(THREAD_CONTEXT_SESSION_KEY);
            super.closeSession(session);
        }
    }
    
    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#getIdFromRawMessage(java.lang.Object, java.util.Map)
     */
    public String getIdFromRawMessage(Object rawMessage, Map context)
        throws ListenerException {
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
    
        context.put("id",id);
        context.put("cid",cid);
        context.put("timestamp",dTimeStamp);
        context.put("replyTo",replyTo);
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

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#getStringFromRawMessage(java.lang.Object, java.util.Map)
     */
    public String getStringFromRawMessage(Object rawMessage, Map context)
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
                    session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
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
                    Session session = (Session)threadContext.get(THREAD_CONTEXT_SESSION_KEY);
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
            throw new ListenerException(e);
        }
    }

    public Destination getDestination() {
        Destination d = getJmsConfigurator().getDestination();
        return d;
    }

    public Destination getDestination(String destinationName) throws JmsException, NamingException {
        return getJmsConfigurator().getDestination(destinationName);
    }
    
    
    /**
     * @return
     */
    public IJmsConfigurator getJmsConfigurator() {
        return jmsConfigurator;
    }

    /**
     * @param configurator
     */
    public void setJmsConfigurator(IJmsConfigurator configurator) {
        jmsConfigurator = configurator;
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

}
