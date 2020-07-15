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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;

import javax.jms.Destination;
import javax.jms.JMSException;
//import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;

/**
 * This class sends messages with JMS.
 *
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td></td>SoapAction<td><i>String</i></td><td>SoapAction. Automatically filled from attribute <code>soapAction</code></td></tr>
 * <tr><td><i>any</i></td><td><i>any</i></td><td>all parameters present are set as messageproperties</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel
 */

public class JmsSender extends JMSFacade implements ISenderWithParameters {
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
	private SoapWrapper soapWrapper = null;
	private String responseHeaders = null;
	private List<String> responseHeadersList = new ArrayList<String>();

	/**
	 * Configures the sender
	 */
	@Override
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

		if (responseHeaders != null) {
			StringTokenizer st = new StringTokenizer(responseHeaders, ",");
			while (st.hasMoreElements()) {
				responseHeadersList.add(st.nextToken());
			}
		}
	}

	/**
	 * Starts the sender 
	 */
	@Override
	public void open() throws SenderException {
		try {
			super.open();
		}
		catch (Exception e) {
			throw new SenderException(e);
		}
	}

	@Override
	public void addParameter(Parameter p) { 
		if (paramList==null) {
			paramList=new ParameterList();
		}
		paramList.add(p);
	}

	@Override
	public ParameterList getParameterList() {
		return paramList;
	}

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		return sendMessage(message, session, null);
	}

	public Message sendMessage(Message input, IPipeLineSession session, String soapHeader) throws SenderException, TimeOutException {
		Session s = null;
		MessageProducer mp = null;
		String correlationID = session==null ? null : session.getMessageId();

		String message;
		try {
			message = input.asString();
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
		ParameterValueList pvl=null;
		if (paramList != null) {
			try {
				pvl=paramList.getValues(input, session);
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
			mp = getMessageProducer(s, getDestination(session));
			Destination replyQueue = null;

			// create message
			javax.jms.Message msg = createMessage(s, correlationID, message);

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
					replyQueue = getMessagingSource().getDynamicReplyQueue(s);
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
					javax.jms.Message rawReplyMsg = mc.receive(getReplyTimeout());
					if (rawReplyMsg==null) {
						throw new TimeOutException("did not receive reply on [" + replyQueue + "] requestMsgId ["+msg.getJMSMessageID()+"] replyCorrelationId ["+replyCorrelationId+"] within ["+getReplyTimeout()+"] ms");
					}
					if(getResponseHeadersList().size() > 0) {
						Enumeration<?> propertyNames = rawReplyMsg.getPropertyNames();
						while(propertyNames.hasMoreElements()) {
							String jmsProperty = (String) propertyNames.nextElement();
							if(getResponseHeadersList().contains(jmsProperty)) {
								session.put(jmsProperty, rawReplyMsg.getObjectProperty(jmsProperty));
							}
						}
					}
					return extractMessage(rawReplyMsg, session, isSoap(), getReplySoapHeaderSessionKey(),soapWrapper);
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
			return new Message(msg.getJMSMessageID());
		} catch (JMSException e) {
			throw new SenderException(e);
		} catch (IOException e) {
			throw new SenderException(e);
		} catch (NamingException e) {
			throw new SenderException(e);
		} catch (SAXException e) {
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

	public Destination getDestination(IPipeLineSession session) throws JmsException, NamingException, JMSException {
		return getDestination();
	}

	/**
	 * Sets the JMS message properties as described in the msgProperties arraylist
	 */
	private void setProperties(javax.jms.Message msg, ParameterValueList msgProperties) throws JMSException {
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
	
	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		ts.append("replyToName", replyToName);
		ts.append("deliveryMode", getDeliveryMode());
		result += ts.toString();
		return result;

	}

	@IbisDoc({"when <code>true</code>, the sender operates in rr mode: the a reply is expected, either on the queue specified in 'replytoname', or on a dynamically generated temporary queue", "false"})
	public void setSynchronous(boolean synchronous) {
		this.synchronous=synchronous;
	}
	@Override
	public boolean isSynchronous() {
		return synchronous;
	}

	public String getReplyTo() {
		return replyToName;
	}

	@IbisDoc({"name of the queue the reply is expected on. this value is send in the jmsreplyto-header with the message.", ""})
	public void setReplyToName(String replyTo) {
		this.replyToName = replyTo;
	}
	
	@IbisDoc({"value of the jmstype field", "not set by application"})
	public void setMessageType(String string) {
		messageType = string;
	}
	public String getMessageType() {
		return messageType;
	}

	@IbisDoc({"controls mode that messages are sent with: either 'persistent' or 'non_persistent'", "not set by application"})
	public void setDeliveryMode(String deliveryMode) {
		int newMode = stringToDeliveryMode(deliveryMode);
		if (newMode==0) {
			ConfigurationWarnings.add(this, log, "unknown delivery mode ["+deliveryMode+"], delivery mode not changed");
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

	@IbisDoc({"sets the priority that is used to deliver the message. ranges from 0 to 9. defaults to -1, meaning not set. effectively the default priority is set by jms to 4", ""})
	public void setPriority(int i) {
		priority = i;
	}

	@IbisDoc({"when <code>true</code>, messages sent are put in a soap envelope", "<code>false</code>"})
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

	@IbisDoc({"soapaction string sent as messageproperty", ""})
	public void setSoapAction(String string) {
		soapAction = string;
	}
	public String getSoapAction() {
		return soapAction;
	}

	@IbisDoc({"name of parameter containing soap header", "soapheader"})
	public void setSoapHeaderParam(String string) {
		soapHeaderParam = string;
	}
	public String getSoapHeaderParam() {
		return soapHeaderParam;
	}

	@IbisDoc({"maximum time in ms to wait for a reply. 0 means no timeout. (only for synchronous=true)", "5000"})
	public void setReplyTimeout(int i) {
		replyTimeout = i;
	}
	public int getReplyTimeout() {
		return replyTimeout;
	}

	@IbisDoc({"session key to store soap header of reply", "soapheader"})
	public void setReplySoapHeaderSessionKey(String string) {
		replySoapHeaderSessionKey = string;
	}
	public String getReplySoapHeaderSessionKey() {
		return replySoapHeaderSessionKey;
	}

	@IbisDoc({"(only used when synchronous='true' and and replytoname is set) eithter 'correlationid', 'correlationid_from_message' or 'messageid'. indicates wether the server uses the correlationid from the pipeline, the correlationid from the message or the messageid in the correlationid field of the reply. this requires the sender to have set the correlationid at the time of sending.", "messageid"})
	public void setLinkMethod(String method) {
		linkMethod=method;
	}
	public String getLinkMethod() {
		return linkMethod;
	}

	@IbisDoc({"a list with jms headers to add to the ipipelinesession", ""})
	public void setResponseHeadersToSessionKeys(String responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
	public List<String> getResponseHeadersList() {
		return responseHeadersList;
	}
}
