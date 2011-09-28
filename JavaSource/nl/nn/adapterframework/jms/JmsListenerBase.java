/*
 * $Log: JmsListenerBase.java,v $
 * Revision 1.11  2011-09-28 06:40:11  europe\m168309
 * removed configWarning soap=true
 *
 * Revision 1.10  2011/09/23 12:10:38  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved configWarning from method setSoap() to method Configuration()
 *
 * Revision 1.9  2011/09/22 14:18:01  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Deprecated attribute soap=true in JmsSender/JmsListener
 *
 * Revision 1.8  2011/06/22 10:44:40  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * improved logging
 *
 * Revision 1.7  2011/06/06 12:26:32  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added soapHeader to method prepareReply
 *
 * Revision 1.6  2011/03/21 14:58:28  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added throws ListenerException to prepareReply()
 *
 * Revision 1.5  2011/01/27 08:45:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * timeout as int
 *
 * Revision 1.4  2009/08/20 12:14:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * user generic getStringFromRawMessage from JMSFacade
 *
 * Revision 1.3  2009/07/28 12:44:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enable SOAP over JMS
 *
 * Revision 1.2  2008/09/17 09:49:32  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implement interface HasSender
 *
 * Revision 1.1  2008/09/01 15:13:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made common baseclass for pushing and pulling jms listeners
 *
 */
package nl.nn.adapterframework.jms;

import java.util.Date;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import nl.nn.adapterframework.configuration.ConfigurationException;
//import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.DateUtils;

import org.apache.commons.lang.StringUtils;

/**
 * Common baseclass for Pulling and Pushing JMS Listeners.
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
 * <tr><td>{@link #setSoap(boolean) soap} <i>deprecated</i></td><td>when <code>true</code>, messages sent are put in a SOAP envelope</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setSoapAction(String) soapAction}</td><td>SoapAction string sent as messageproperty</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapHeaderParam(String) soapHeaderParam}</td><td>name of parameter containing SOAP header</td><td>soapHeader</td></tr>
 * </table>
 * 
 * @author  Gerrit van Brakel
 * @since   4.9
 * @version Id
 */
public class JmsListenerBase extends JMSFacade implements HasSender {

	private int timeOut = 3000;
	private boolean useReplyTo=true;
	private String replyMessageType=null;
	private long replyMessageTimeToLive=0;
	private int replyPriority=-1;
	private String replyDeliveryMode=MODE_NON_PERSISTENT;
	private ISender sender;
		
	private boolean forceMessageIdAsCorrelationId=false;
 
	private String commitOnState="success";

	private boolean soap=false;
	private String replyEncodingStyleURI=null;
	private String replyNamespaceURI=null;
	private String replySoapAction=null;
	private String soapHeaderSessionKey="soapHeader";
	
	private SoapWrapper soapWrapper=null;


	public void configure() throws ConfigurationException {
		super.configure();
		/*if (isSoap()) {
			ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			String msg = getLogPrefix()+"the use of attribute soap=true has been deprecated. Please change to SoapWrapperPipe";
			configWarnings.add(log, msg);
			soapWrapper=SoapWrapper.getInstance();
		}*/
		ISender sender = getSender();
		if (sender != null) {
			sender.configure();
		}
	}

	public void open() throws ListenerException {
		try {
			openFacade();
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
	
	public void close() throws ListenerException {
		try {
			closeFacade();
	
			if (getSender() != null) {
				getSender().close();
			}
		} catch (Exception e) {
			throw new ListenerException(e);
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
		Date tsSent = null;
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
			tsSent = new Date(lTimeStamp);

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

		if (log.isDebugEnabled()) {
			log.debug(getLogPrefix()+"listener on ["+ getDestinationName() 
				+ "] got message with JMSDeliveryMode=[" + mode
				+ "] \n  JMSMessageID=[" + id
				+ "] \n  JMSCorrelationID=[" + cid
				+ "] \n  Timestamp Sent=[" + DateUtils.format(tsSent) 
				+ "] \n  ReplyTo=[" + ((replyTo==null)?"none" : replyTo.toString())
				+ "] \n Message=[" + message.toString()
				+ "]");
		}    
		PipeLineSession.setListenerParameters(threadContext, id, cid, null, tsSent);
		threadContext.put("timestamp",tsSent);
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
		try {
			return getStringFromRawMessage(rawMessage, threadContext, isSoap(), getSoapHeaderSessionKey(),soapWrapper);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public String prepareReply(String rawReply, Map threadContext) throws ListenerException {
		return prepareReply(rawReply, threadContext, null);
	}

	public String prepareReply(String rawReply, Map threadContext, String soapHeader) throws ListenerException {
		if (!isSoap()) {
			return rawReply;
		}
		String replyMessage;
		if (soapHeader==null) {
			if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
				soapHeader=(String)threadContext.get(getSoapHeaderSessionKey());
			}
		}
		replyMessage = soapWrapper.putInEnvelope(rawReply, getReplyEncodingStyleURI(),getReplyNamespaceURI(),soapHeader);
		if (log.isDebugEnabled()) log.debug("wrapped message [" + replyMessage + "]");
		return replyMessage;
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

	public void setTimeOut(int newTimeOut) {
		timeOut = newTimeOut;
	}
	public int getTimeOut() {
		return timeOut;
	}


	public void setUseReplyTo(boolean newUseReplyTo) {
		useReplyTo = newUseReplyTo;
	}
	public boolean isUseReplyTo() {
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

	public void setSoap(boolean b) {
		soap = b;
	}
	public boolean isSoap() {
		return soap;
	}

	public void setReplyEncodingStyleURI(String string) {
		replyEncodingStyleURI = string;
	}
	public String getReplyEncodingStyleURI() {
		return replyEncodingStyleURI;
	}

	public void setReplyNamespaceURI(String string) {
		replyNamespaceURI = string;
	}
	public String getReplyNamespaceURI() {
		return replyNamespaceURI;
	}

	public void setReplySoapAction(String string) {
		replySoapAction = string;
	}
	public String getReplySoapAction() {
		return replySoapAction;
	}

	public void setSoapHeaderSessionKey(String string) {
		soapHeaderSessionKey = string;
	}
	public String getSoapHeaderSessionKey() {
		return soapHeaderSessionKey;
	}

}
