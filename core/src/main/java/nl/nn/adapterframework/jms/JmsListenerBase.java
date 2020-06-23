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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.IWithParameters;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.AppConstants;
import nl.nn.adapterframework.util.DateUtils;

/**
 * Common baseclass for Pulling and Pushing JMS Listeners.
 *
 * @author  Gerrit van Brakel
 * @since   4.9
 */
public class JmsListenerBase extends JMSFacade implements HasSender, IWithParameters {

	private long timeOut = 1000; // Same default value as Spring: https://docs.spring.io/spring/docs/3.2.x/javadoc-api/org/springframework/jms/listener/AbstractPollingMessageListenerContainer.html#setReceiveTimeout(long)
	private boolean useReplyTo=true;
	private String replyMessageType=null;
	private long replyMessageTimeToLive=0;
	private int replyPriority=-1;
	private String replyDeliveryMode=MODE_NON_PERSISTENT;
	private ISender sender;
	
	private static final AppConstants APP_CONSTANTS = AppConstants.getInstance();
	private final String MSGLOG_KEYS = APP_CONSTANTS.getResolvedProperty("msg.log.keys");
	private final Map<String, String> xPathLogMap = new HashMap<String, String>();
	private String xPathLoggingKeys=null;
	
	private boolean forceMessageIdAsCorrelationId=false;
 
	private String commitOnState="success";

	private boolean soap=false;
	private String replyEncodingStyleURI=null;
	private String replyNamespaceURI=null;
	private String replySoapAction=null;
	private String soapHeaderSessionKey="soapHeader";
	
	private SoapWrapper soapWrapper=null;

	private ParameterList paramList = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (isSoap()) {
			//ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			//String msg = getLogPrefix()+"the use of attribute soap=true has been deprecated. Please change to SoapWrapperPipe";
			//configWarnings.add(log, msg);
			soapWrapper=SoapWrapper.getInstance();
		}
		ISender sender = getSender();
		if (sender != null) {
			sender.configure();
		}
		configurexPathLogging();

		if (paramList!=null) {
			paramList.configure();
		}
	}
	
	protected Map<String, String> getxPathLogMap() {
		return xPathLogMap;
	}
	
	private void configurexPathLogging() {
		String logKeys = MSGLOG_KEYS;
		if(getxPathLoggingKeys() != null) //Override on listener level
			logKeys = getxPathLoggingKeys();

		StringTokenizer tokenizer = new StringTokenizer(logKeys, ",");
		while (tokenizer.hasMoreTokens()) {
			String name = tokenizer.nextToken();
			String xPath = APP_CONSTANTS.getResolvedProperty("msg.log.xPath." + name);
			if(xPath != null)
				xPathLogMap.put(name, xPath);
		}
	}

	@Override
	public void open() throws ListenerException {
		try {
			super.open();
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

	@Override
	public void close() {
		super.close();
		try {
			if (getSender() != null) {
				getSender().close();
			}
		} catch (Exception e) {
			log.warn(getLogPrefix() + "caught exception stopping listener", e);
		}
	}

	/**
	 * Fill in thread-context with things needed by the JMSListener code.
	 * This includes a Session. The Session object can be passed in
	 * externally.
	 * 
	 * @param rawMessage - Original message received, can not be <code>null</code>
	 * @param threadContext - Thread context to be populated, can not be <code>null</code>
	 */
	public String getIdFromRawMessage(javax.jms.Message rawMessage, Map<String, Object> threadContext) throws ListenerException {
		TextMessage message = null;
		try {
			message = (TextMessage) rawMessage;
		} catch (ClassCastException e) {
			log.error("message received by listener on ["+ getDestinationName()+ "] was not of type TextMessage, but ["+rawMessage.getClass().getName()+"]", e);
			return null;
		}
		return retrieveIdFromMessage(message, threadContext);
	}
	
	
	protected String retrieveIdFromMessage(javax.jms.Message message, Map<String, Object> threadContext) throws ListenerException {
		String cid = "unset";
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
		PipeLineSessionBase.setListenerParameters(threadContext, id, cid, null, tsSent);
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
	 * Extracts string from message obtained from getRawMessage. May also extract
	 * other parameters from the message and put those in the threadContext.
	 * @return String  input message for adapter.
	 */
	public Message extractMessage(javax.jms.Message rawMessage, Map<String,Object> threadContext) throws ListenerException {
		try {
			return extractMessage(rawMessage, threadContext, isSoap(), getSoapHeaderSessionKey(), soapWrapper);
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public Message prepareReply(Message rawReply, Map<String,Object> threadContext) throws ListenerException {
		return prepareReply(rawReply, threadContext, null);
	}

	public Message prepareReply(Message rawReply, Map<String,Object> threadContext, String soapHeader) throws ListenerException {
		if (!isSoap()) {
			return rawReply;
		}
		Message replyMessage;
		if (soapHeader==null) {
			if (StringUtils.isNotEmpty(getSoapHeaderSessionKey())) {
				soapHeader=(String)threadContext.get(getSoapHeaderSessionKey());
			}
		}
		try {
			replyMessage = new Message(soapWrapper.putInEnvelope(rawReply.asString(), getReplyEncodingStyleURI(),getReplyNamespaceURI(),soapHeader));
		} catch (IOException e) {
			throw new ListenerException("cannot convert message",e);
		}
		if (log.isDebugEnabled()) log.debug("wrapped message [" + replyMessage + "]");
		return replyMessage;
	}

	public void setSender(ISender newSender) {
		sender = newSender;
			log.debug("["+getName()+"] ** registered sender ["+sender.getName()+"] with properties ["+sender.toString()+"]");
    
	}
	@Override
	public ISender getSender() {
		return sender;
	}

	/**
	 * Set additional message headers/properties on the JMS response, read after message has been processed!
	 * @param threadContext which has been build during the pipeline
	 * @return a map with headers to set to the JMS response
	 */
	protected Map<String, Object> getMessageProperties(Map<String, Object> threadContext) {
		Map<String, Object> properties = null;

		if (threadContext != null && paramList != null) {
			if(properties == null) {
				properties = new HashMap<String, Object>();
			}
			properties.putAll(evaluateParameters(threadContext));
		}

		return properties;
	}

	@Override
	public void addParameter(Parameter p) {
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	/**
	 * return the Parameters
	 */
	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	/**
	 * Retrieve JMS properties from the threadContext
	 * @param threadContext used throughout the pipeline
	 * @return a map with JMS headers to set
	 */
	private Map<String, Object> evaluateParameters(Map<String, Object> threadContext) {
		Map<String, Object> result = new HashMap<String, Object>();
		if (threadContext != null && paramList != null) {
			for (Iterator<Parameter> parmIterator = paramList.iterator(); parmIterator.hasNext(); ) {
				Parameter param = parmIterator.next();
				Object value = param.getValue();

				if(StringUtils.isNotEmpty(param.getSessionKey())) {
					log.debug("trying to resolve sessionKey["+param.getSessionKey()+"]");
					Object resolvedValue = threadContext.get(param.getSessionKey());
					if(resolvedValue != null)
						value = resolvedValue;
				}

				result.put(param.getName(), value);
			}
		}
		return result;
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
	@IbisDoc({"<i>deprecated</i> exit state to control commit or rollback of jmssession. only used if <code>jmstransacted</code> is set true.", "success"})
	public void setCommitOnState(String newCommitOnState) {
		commitOnState = newCommitOnState;
	}
	public String getCommitOnState() {
		return commitOnState;
	}

	@IbisDoc({"receive timeout in milliseconds as specified by the JMS API, see https://docs.oracle.com/javaee/7/api/javax/jms/MessageConsumer.html#receive-long-", "1000 [ms]"})
	public void setTimeOut(long newTimeOut) {
		timeOut = newTimeOut;
	}
	public long getTimeOut() {
		return timeOut;
	}


	@IbisDoc({"", "true"})
	public void setUseReplyTo(boolean newUseReplyTo) {
		useReplyTo = newUseReplyTo;
	}
	public boolean isUseReplyTo() {
		return useReplyTo;
	}

	
	@IbisDoc({"value of the jmstype field of the reply message", "not set by application"})
	public void setReplyMessageType(String string) {
		replyMessageType = string;
	}
	public String getReplyMessageType() {
		return replyMessageType;
	}


	@IbisDoc({"controls mode that reply messages are sent with: either 'persistent' or 'non_persistent'", "not set by application"})
	public void setReplyDeliveryMode(String string) {
		replyDeliveryMode = string;
	}
	public String getReplyDeliveryMode() {
		return replyDeliveryMode;
	}


	@IbisDoc({"sets the priority that is used to deliver the reply message. ranges from 0 to 9. defaults to -1, meaning not set. effectively the default priority is set by jms to 4", ""})
	public void setReplyPriority(int i) {
		replyPriority = i;
	}
	public int getReplyPriority() {
		return replyPriority;
	}


	@IbisDoc({"time that replymessage will live", "0 [ms]"})
	public void setReplyMessageTimeToLive(long l) {
		replyMessageTimeToLive = l;
	}
	public long getReplyMessageTimeToLive() {
		return replyMessageTimeToLive;
	}

	@IbisDoc({"when <code>true</code>, messages sent are put in a soap envelope", "<code>false</code>"})
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

	@IbisDoc({"comma separated list of all xpath keys that need to be logged. (overrides <code>msg.log.keys</code> property)", ""})
	public void setxPathLoggingKeys(String string) {
		xPathLoggingKeys = string;
	}
	
	public String getxPathLoggingKeys() {
		return xPathLoggingKeys;
	}
}
