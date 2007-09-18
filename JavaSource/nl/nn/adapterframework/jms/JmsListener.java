/*
 * Created on 18-sep-07
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
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
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class JmsListener extends JMSFacade implements IPushingListener {
    public static final String version="$RCSfile: JmsListener.java,v $ $Revision: 1.25.4.1 $ $Date: 2007-09-18 11:20:38 $";

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

    
    private IJmsConfigurator jmsConfigurator;
    private String name;
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
        // Do NOT open JMSFacade!
        jmsConfigurator.openJmsReceiver();
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.IListener#close()
     */
    public void close() throws ListenerException {
        // Do NOT close JMSFacade!
        jmsConfigurator.closeJmsReceiver();
    }
    
    public void populateThreadContext(Map threadContext, Session session) {
        threadContext.put(THREAD_CONTEXT_SESSION_KEY, session);
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

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.INamedObject#getName()
     */
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see nl.nn.adapterframework.core.INamedObject#setName(java.lang.String)
     */
    public void setName(String name) {
        this.name = name;
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
