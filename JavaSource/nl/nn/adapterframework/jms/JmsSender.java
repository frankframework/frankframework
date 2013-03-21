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

import java.io.IOException;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPostboxSender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.util.DomBuilderException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * This class sends messages with JMS.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jms.JmsSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>name of the JMS destination (queue or topic) to use</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>either <code>QUEUE</code> or <code>TOPIC</code></td><td><code>QUEUE</code></td></tr>
 * <tr><td>{@link #setMessageTimeToLive(long) messageTimeToLive}</td><td>the time it takes for the message to expire. If the message is not consumed before, it will be lost. Make sure to set it to a positive value for request/repy type of messages.</td><td>0 (unlimited)</td></tr>
 * <tr><td>{@link #setMessageType(String) messageType}</td><td>value of the JMSType field</td><td>not set by application</td></tr>
 * <tr><td>{@link #setDeliveryMode(String) deliveryMode}</td><td>controls mode that messages are sent with: either 'persistent' or 'non_persistent'</td><td>not set by application</td></tr>
 * <tr><td>{@link #setPriority(int) priority}</td><td>sets the priority that is used to deliver the message. ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by Jms to 4</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>&nbsp;</td><td>AUTO_ACKNOWLEDGE</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setSynchronous(boolean) synchronous}</td><td>when <code>true</code>, the sender operates in RR mode: the a reply is expected, either on the queue specified in 'replyToName', or on a dynamically generated temporary queue</td><td>false</td></tr>
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>(Only used when synchronous="true" and and replyToName is set) Eithter 'CORRELATIONID', 'CORRELATIONID_FROM_MESSAGE' or 'MESSAGEID'. Indicates wether the server uses the correlationID from the pipeline, the correlationID from the message or the messageID in the correlationID field of the reply. This requires the sender to have set the correlationID at the time of sending.</td><td>MESSAGEID</td></tr>
 * <tr><td>{@link #setReplyToName(String) replyToName}</td><td>Name of the queue the reply is expected on. This value is send in the JmsReplyTo-header with the message.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setReplyTimeout(int) replyTimeout}</td><td>maximum time in ms to wait for a reply. 0 means no timeout. (Only for synchronous=true)</td><td>5000</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>rather useless attribute, and not the same as delivery mode. You probably want to use that.</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUseDynamicReplyQueue(boolean) useDynamicReplyQueue}</td><td>when <code>true</code>, a temporary queue is used to receive a reply</td><td>false</td></tr>
 * <tr><td>{@link #setSoap(boolean) soap}</td><td>when <code>true</code>, messages sent are put in a SOAP envelope</td><td><code>false</code></td></tr>
 * <tr><td>{@link #setSoapAction(String) soapAction}</td><td>SoapAction string sent as messageproperty</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSoapHeaderParam(String) soapHeaderParam}</td><td>name of parameter containing SOAP header</td><td>soapHeader</td></tr>
 * <tr><td>{@link #setReplySoapHeaderSessionKey(String) replySoapHeaderSessionKey}</td><td>session key to store SOAP header of reply</td><td>soapHeader</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td></td>SoapAction<td><i>String</i></td><td>SoapAction. Automatically filled from attribute <code>soapAction</code></td></tr>
 * <tr><td><i>any</i></td><td><i>any</i></td><td>all parameters present are set as messageproperties</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel
 * @version $Id$
 */

public class JmsSender extends JMSFacade implements ISenderWithParameters, IPostboxSender {
	private String replyToName = null;
	private int deliveryMode = 0;
	private String messageType = null;
	private int priority=-1;
	private boolean synchronous=false;
	private int replyTimeout=5000;
	private String replySoapHeaderSessionKey="replySoapHeader";
	private boolean soap=false;
	private String encodingStyleURI=null;
	private String serviceNamespaceURI=null;
	private String soapAction=null;
	private String soapHeaderParam="soapHeader";
	private String linkMethod="MESSAGEID";
	
	protected ParameterList paramList = null;
	private SoapWrapper soapWrapper=null;

	/**
	 * Configures the sender
	 */
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getSoapAction()) && (paramList==null || paramList.findParameter("SoapAction")==null)) {
			Parameter p = new Parameter();
			p.setName("SoapAction");
			p.setValue(getSoapAction());
			addParameter(p);
		}
		if (paramList!=null) {
			paramList.configure();
		}
		super.configure();
		if (isSoap()) {
			//ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
			//String msg = getLogPrefix()+"the use of attribute soap=true has been deprecated. Please change to SoapWrapperPipe";
			//configWarnings.add(log, msg);
			soapWrapper=SoapWrapper.getInstance();
		}
	}

	/**
	 * Starts the sender 
	 */
	public void open() throws SenderException {
		try {
			openFacade();
		}
		catch (Exception e) {
			throw new SenderException(e);
		}
	}

	/**
	 * Stops the sender 
	 */
	public void close() throws SenderException {
		try {
			closeFacade();
		}
		catch (Throwable e) {
			throw new SenderException("JmsMessageSender [" + getName() + "] " + "got error occured stopping sender", e);
		}
	}

	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}


	public String sendMessage(String correlationID, String message) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		return sendMessage(correlationID, message, prc, null);
	}

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc, String soapHeader) throws SenderException, TimeOutException {
		Session s = null;
		MessageProducer mp = null;

		ParameterValueList pvl=null;
		if (prc != null && paramList != null) {
			try {
				pvl=prc.getValues(paramList);
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix()+"cannot extract parameters",e);
			}
		}

		if (isSoap()) {
			if (soapHeader==null) {
				if (pvl!=null && StringUtils.isNotEmpty(getSoapHeaderParam())) {
					ParameterValue soapHeaderParamValue=pvl.getParameterValue(getSoapHeaderParam());
					if (soapHeaderParamValue==null) {
						log.warn("no SoapHeader found using parameter ["+getSoapHeaderParam()+"]");
					} else {
						soapHeader=soapHeaderParamValue.asStringValue("");
					}
				}
			}
			message = soapWrapper.putInEnvelope(message, getEncodingStyleURI(),getServiceNamespaceURI(),soapHeader);
			if (log.isDebugEnabled()) log.debug(getLogPrefix()+"correlationId ["+correlationID+"] soap message ["+message+"]");
		}
		try {
			s = createSession();
			mp = getMessageProducer(s, getDestination());
			Destination replyQueue = null;

			// create message
			Message msg = createTextMessage(s, correlationID, message);

			if (getMessageType()!=null) {
				msg.setJMSType(getMessageType());
			}
			if (getDeliveryModeInt()>0) {
				msg.setJMSDeliveryMode(getDeliveryModeInt());
				mp.setDeliveryMode(getDeliveryModeInt());
			}
			if (getPriority()>=0) {
				msg.setJMSPriority(getPriority());
				mp.setPriority(getPriority());
			}

			// set properties
			if (pvl != null) {
				setProperties(msg, pvl);
			}
			if (replyToName != null) {
				replyQueue = getDestination(replyToName);
			} else {
				if (isSynchronous()) {
					replyQueue = getMessagingSource().getDynamicReplyQueue((QueueSession)s);
				}
			}
			if (replyQueue!=null) {
				msg.setJMSReplyTo(replyQueue);
				if (log.isDebugEnabled()) log.debug("replyTo set to queue [" + replyQueue.toString() + "]");
			}

			// send message	
			send(mp, msg);
			if (log.isDebugEnabled()) {
				log.debug(
					"[" + getName() + "] " + "sent message [" + message + "] " + "to [" + mp.getDestination()
						+ "] " + "msgID [" + msg.getJMSMessageID() + "] " + "correlationID [" + msg.getJMSCorrelationID()
						+ "] " + "using deliveryMode [" + getDeliveryMode() + "] "
						+ ((replyToName != null) ? "replyTo [" + replyToName+"]" : ""));
			} else {
				if (log.isInfoEnabled()) {
					log.info(
						"[" + getName() + "] " + "sent message to [" + mp.getDestination()
							+ "] " + "msgID [" + msg.getJMSMessageID() + "] " + "correlationID [" + msg.getJMSCorrelationID()
							+ "] " + "using deliveryMode [" + getDeliveryMode() + "] "
							+ ((replyToName != null) ? "replyTo [" + replyToName+"]" : ""));
				}
			}
			if (isSynchronous()) {
				String replyCorrelationId=null;
				if (replyToName != null) {
					if ("CORRELATIONID".equalsIgnoreCase(getLinkMethod())) {
						replyCorrelationId=correlationID;
					} else if ("CORRELATIONID_FROM_MESSAGE".equalsIgnoreCase(getLinkMethod())) {
						replyCorrelationId=msg.getJMSCorrelationID();
					} else {
						replyCorrelationId=msg.getJMSMessageID();
					}
				}
				if (log.isDebugEnabled()) log.debug("[" + getName() + "] start waiting for reply on [" + replyQueue + "] requestMsgId ["+msg.getJMSMessageID()+"] replyCorrelationId ["+replyCorrelationId+"] for ["+getReplyTimeout()+"] ms");
				MessageConsumer mc = getMessageConsumerForCorrelationId(s,replyQueue,replyCorrelationId);
				try {
					Message rawReplyMsg = mc.receive(getReplyTimeout());
					if (rawReplyMsg==null) {
						throw new TimeOutException("did not receive reply on [" + replyQueue + "] requestMsgId ["+msg.getJMSMessageID()+"] replyCorrelationId ["+replyCorrelationId+"] within ["+getReplyTimeout()+"] ms");
					}
					return getStringFromRawMessage(rawReplyMsg, prc!=null?prc.getSession():null, isSoap(), getReplySoapHeaderSessionKey(),soapWrapper);
				} finally {
					if (mc != null) { 
						try { 
							mc.close(); 
						} catch (JMSException e) { 
							log.warn("JmsSender [" + getName() + "] got exception closing message consumer for reply",e); 
						}
					}
				}
			}
			return msg.getJMSMessageID();
		} catch (JMSException e) {
			throw new SenderException(e);
		} catch (IOException e) {
			throw new SenderException(e);
		} catch (NamingException e) {
			throw new SenderException(e);
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		} catch (TransformerException e) {
			throw new SenderException(e);
		} catch (JmsException e) {
			throw new SenderException(e);
		} finally {
			if (mp != null) { 
				try { 
					mp.close(); 
				} catch (JMSException e) { 
					log.warn("JmsSender [" + getName() + "] got exception closing message producer",e); 
				}
			}
			closeSession(s);
		}
	}

	/**
	 * sets the JMS message properties as descriped in the msgProperties arraylist
	 * @param msg
	 * @param msgProperties
	 * @throws JMSException
	 */
	private void setProperties(Message msg, ParameterValueList msgProperties) throws JMSException {
		for (int i=0; i<msgProperties.size(); i++) {
			ParameterValue property = msgProperties.getParameterValue(i);
			String type = property.getDefinition().getType();
			String name = property.getDefinition().getName();

			if (!isSoap() || !name.equals(getSoapHeaderParam())) {

				if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"setting ["+type+"] property from param ["+name+"] to value ["+property.getValue()+"]"); }

				if ("boolean".equalsIgnoreCase(type))
					msg.setBooleanProperty(name, property.asBooleanValue(false));
				else if ("byte".equalsIgnoreCase(type))
					msg.setByteProperty(name, property.asByteValue((byte) 0));
				else if ("double".equalsIgnoreCase(type))
					msg.setDoubleProperty(name, property.asDoubleValue(0));
				else if ("float".equalsIgnoreCase(type))
					msg.setFloatProperty(name, property.asFloatValue(0));
				else if ("int".equalsIgnoreCase(type))
					msg.setIntProperty(name, property.asIntegerValue(0));
				else if ("long".equalsIgnoreCase(type))
					msg.setLongProperty(name, property.asLongValue(0L));
				else if ("short".equalsIgnoreCase(type))
					msg.setShortProperty(name, property.asShortValue((short) 0));
				else if ("string".equalsIgnoreCase(type))
					msg.setStringProperty(name, property.asStringValue(""));
				else // if ("object".equalsIgnoreCase(type))
					msg.setObjectProperty(name, property.getValue());
			}
		}
	}
	
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		ts.append("replyToName", replyToName);
		ts.append("deliveryMode", getDeliveryMode());
		result += ts.toString();
		return result;

	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous=synchronous;
	}
	public boolean isSynchronous() {
		return synchronous;
	}

	public String getReplyTo() {
		return replyToName;
	}
	public void setReplyToName(String replyTo) {
		this.replyToName = replyTo;
	}
	
	public void setMessageType(String string) {
		messageType = string;
	}
	public String getMessageType() {
		return messageType;
	}

	public void setDeliveryMode(String deliveryMode) {
		int newMode = stringToDeliveryMode(deliveryMode);
		if (newMode==0) {
			ConfigurationWarnings cw = ConfigurationWarnings.getInstance();
			cw.add(log,getLogPrefix()+"unknown delivery mode ["+deliveryMode+"], delivery mode not changed");
		} else
			this.deliveryMode=newMode;
	}
	public String getDeliveryMode() {
		return deliveryModeToString(deliveryMode);
	}
	public int getDeliveryModeInt() {
		return deliveryMode;
	}
	


	public int getPriority() {
		return priority;
	}
	public void setPriority(int i) {
		priority = i;
	}

	public void setSoap(boolean b) {
		soap = b;
	}
	public boolean isSoap() {
		return soap;
	}

	public void setEncodingStyleURI(String string) {
		encodingStyleURI = string;
	}
	public String getEncodingStyleURI() {
		return encodingStyleURI;
	}

	public void setServiceNamespaceURI(String string) {
		serviceNamespaceURI = string;
	}
	public String getServiceNamespaceURI() {
		return serviceNamespaceURI;
	}

	public void setSoapAction(String string) {
		soapAction = string;
	}
	public String getSoapAction() {
		return soapAction;
	}

	public void setSoapHeaderParam(String string) {
		soapHeaderParam = string;
	}
	public String getSoapHeaderParam() {
		return soapHeaderParam;
	}

	public void setReplyTimeout(int i) {
		replyTimeout = i;
	}
	public int getReplyTimeout() {
		return replyTimeout;
	}

	public void setReplySoapHeaderSessionKey(String string) {
		replySoapHeaderSessionKey = string;
	}
	public String getReplySoapHeaderSessionKey() {
		return replySoapHeaderSessionKey;
	}

	public void setLinkMethod(String method) {
		linkMethod=method;
	}
	public String getLinkMethod() {
		return linkMethod;
	}

}
