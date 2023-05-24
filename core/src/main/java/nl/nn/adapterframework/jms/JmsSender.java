/*
   Copyright 2013, 2018 Nationale-Nederlanden, 2020-2023 WeAreFrank!

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
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.NamingException;
import javax.xml.transform.TransformerException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xml.sax.SAXException;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.soap.SoapWrapper;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.SpringUtils;

/**
 * This class sends messages with JMS.
 *
 * @ff.parameters All parameters present are set as message-properties.
 * @ff.parameter SoapAction Automatically filled from attribute <code>soapAction</code>
 *
 * @author Gerrit van Brakel
 */

public class JmsSender extends JMSFacade implements ISenderWithParameters {
	private @Getter String replyToName = null;
	private @Getter DeliveryMode deliveryMode = DeliveryMode.NOT_SET;
	private @Getter String messageType = null;
	private @Getter int priority=-1;
	private @Getter boolean synchronous=false;
	private @Getter int replyTimeout=5000;
	private @Getter String replySoapHeaderSessionKey="replySoapHeader";
	private @Getter boolean soap=false;
	private @Getter String encodingStyleURI=null;
	private @Getter String serviceNamespaceURI=null;
	private @Getter String soapAction=null;
	private @Getter String soapHeaderParam="soapHeader";
	private @Getter LinkMethod linkMethod=LinkMethod.MESSAGEID;
	private @Getter String destinationParam = null;

	protected ParameterList paramList = null;
	private SoapWrapper soapWrapper = null;
	private String responseHeaders = null;
	private @Getter List<String> responseHeadersList = new ArrayList<String>();

	public enum LinkMethod {
		/** use the generated messageId as the correlationId in the selector for response messages */
		MESSAGEID,
		/** set the correlationId of the pipeline as the correlationId of the message sent, and use that as the correlationId in the selector for response messages */
		CORRELATIONID,
		/** do not automatically set the correlationId of the message sent, but use use the value found in that header after sending the message as the selector for response messages */
		CORRELATIONID_FROM_MESSAGE
	}

	/**
	 * Configures the sender
	 */
	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getSoapAction()) && (paramList==null || paramList.findParameter("SoapAction")==null)) {
			Parameter p = SpringUtils.createBean(getApplicationContext(), Parameter.class);
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
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		return new SenderResult(sendMessage(message, session, null));
	}

	public Message sendMessage(Message message, PipeLineSession session, String soapHeader) throws SenderException, TimeoutException {
		Session s = null;
		MessageProducer mp = null;
		String correlationID = session==null ? null : session.getCorrelationId();

		checkTransactionManagerValidity();
		ParameterValueList pvl=null;
		if (paramList != null) {
			try {
				pvl=paramList.getValues(message, session);
			} catch (ParameterException e) {
				throw new SenderException(getLogPrefix()+"cannot extract parameters",e);
			}
		}

		try {
			if (isSoap()) {
				if (soapHeader==null) {
					if (pvl!=null && StringUtils.isNotEmpty(getSoapHeaderParam())) {
						ParameterValue soapHeaderParamValue=pvl.get(getSoapHeaderParam());
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
			s = createSession();
			mp = getMessageProducer(s, getDestination(session, pvl));
			Destination replyQueue = null;

			// create message
			javax.jms.Message msg = createMessage(s, correlationID, message);

			if (getMessageType()!=null) {
				msg.setJMSType(getMessageType());
			}
			if (getDeliveryMode()!=DeliveryMode.NOT_SET) {
				msg.setJMSDeliveryMode(getDeliveryMode().getDeliveryMode());
				mp.setDeliveryMode(getDeliveryMode().getDeliveryMode());
			}
			if (getPriority()>=0) {
				msg.setJMSPriority(getPriority());
				mp.setPriority(getPriority());
			}

			// set properties
			if (pvl != null) {
				setProperties(msg, pvl);
			}
			if (getReplyToName() != null) {
				replyQueue = getDestination(getReplyToName());
			} else {
				if (isSynchronous()) {
					replyQueue = getMessagingSource().getDynamicReplyQueue(s);
				}
			}
			if (replyQueue!=null) {
				msg.setJMSReplyTo(replyQueue);
				if (log.isDebugEnabled()) log.debug("replyTo set to queue [" + replyQueue + "]");
			}

			// send message
			send(mp, msg);
			if (log.isDebugEnabled()) {
				log.debug("[" + getName() + "] " + "sent message [" + message + "] " + "to [" + mp.getDestination() + "] " + "msgID [" + msg.getJMSMessageID() + "] " + "correlationID [" + msg.getJMSCorrelationID() + "] " + "using deliveryMode [" + getDeliveryMode() + "] " + ((getReplyToName() != null) ? "replyTo [" + getReplyToName()+"]" : ""));
			} else {
				if (log.isInfoEnabled()) {
					log.info("[" + getName() + "] " + "sent message to [" + mp.getDestination() + "] " + "msgID [" + msg.getJMSMessageID() + "] " + "correlationID [" + msg.getJMSCorrelationID() + "] " + "using deliveryMode [" + getDeliveryMode() + "] " + ((getReplyToName() != null) ? "replyTo [" + getReplyToName()+"]" : ""));
				}
			}
			if (isSynchronous()) {
				String replyCorrelationId=null;
				if (getReplyToName() != null) {
					switch (getLinkMethod()) {
					case MESSAGEID:
						replyCorrelationId=msg.getJMSMessageID();
						break;
					case CORRELATIONID:
						replyCorrelationId=correlationID;
						break;
					case CORRELATIONID_FROM_MESSAGE:
						replyCorrelationId=msg.getJMSCorrelationID();
						break;
					default:
						throw new IllegalStateException("unknown linkMethod ["+getLinkMethod()+"]");
					}
				}
				if (log.isDebugEnabled()) log.debug("[" + getName() + "] start waiting for reply on [" + replyQueue + "] requestMsgId ["+msg.getJMSMessageID()+"] replyCorrelationId ["+replyCorrelationId+"] for ["+getReplyTimeout()+"] ms");
				MessageConsumer mc = getMessageConsumerForCorrelationId(s,replyQueue,replyCorrelationId);
				try {
					javax.jms.Message rawReplyMsg = mc.receive(getReplyTimeout());
					if (rawReplyMsg==null) {
						throw new TimeoutException("did not receive reply on [" + replyQueue + "] requestMsgId ["+msg.getJMSMessageID()+"] replyCorrelationId ["+replyCorrelationId+"] within ["+getReplyTimeout()+"] ms");
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
					if(mc != null) {
						try {
							mc.close();
						} catch (JMSException e) {
							log.warn("JmsSender [" + getName() + "] got exception closing message consumer for reply", e);
						}
					}
				}
			}
			return new Message(msg.getJMSMessageID());
		} catch (JMSException | IOException | NamingException | SAXException | TransformerException | JmsException e) {
			throw new SenderException(e);
		}  finally {
			if(mp != null) {
				try {
					mp.close();
				} catch (JMSException e) {
					log.warn("JmsSender [" + getName() + "] got exception closing message producer", e);
				}
			}
			closeSession(s);
		}
	}

	public Destination getDestination(PipeLineSession session, ParameterValueList pvl) throws JmsException, NamingException, JMSException {
		if (StringUtils.isNotEmpty(getDestinationParam())) {
			String destinationName = pvl.get(getDestinationParam()).asStringValue(null);
			if (StringUtils.isNotEmpty(destinationName)) {
				return getDestination(destinationName);
			}
		}
		return getDestination();
	}

	/**
	 * Sets the JMS message properties as described in the msgProperties arraylist
	 */
	private void setProperties(javax.jms.Message msg, ParameterValueList pvl) throws JMSException {
		for(ParameterValue property : pvl) {
			ParameterType type = property.getDefinition().getType();
			String name = property.getDefinition().getName();

			if ((!isSoap() || !name.equals(getSoapHeaderParam()) && (StringUtils.isEmpty(getDestinationParam()) || !name.equals(getDestinationParam())))) {

				if (log.isDebugEnabled()) { log.debug(getLogPrefix()+"setting ["+type+"] property from param ["+name+"] to value ["+property.getValue()+"]"); }
				switch(type) {
					case BOOLEAN:
						msg.setBooleanProperty(name, property.asBooleanValue(false));
						break;
					case INTEGER:
						msg.setIntProperty(name, property.asIntegerValue(0));
						break;
					case STRING:
						msg.setStringProperty(name, property.asStringValue(""));
						break;
					default:
						msg.setObjectProperty(name, property.getValue());
						break;
				}
			}
		}
	}

	@Override
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		ts.append("replyToName", getReplyToName());
		ts.append("deliveryMode", getDeliveryMode());
		result += ts.toString();
		return result;

	}

	/** Parameter that is used, if specified and not empty, to determine the destination. Overrides the <code>destination</code> attribute */
	public void setDestinationParam(String string) {
		destinationParam = string;
	}

	/**
	 * If <code>true</code>, the sender operates in RR mode: A reply is expected, either on the queue specified in <code>replyToName</code>, or on a dynamically generated temporary queue
	 * @ff.default false
	 */
	public void setSynchronous(boolean synchronous) {
		this.synchronous=synchronous;
	}

	/**
	 * Name of the queue the reply is expected on. This value is sent in the JMSReplyTo-header with the message.
	 * @ff.default a dynamically generated temporary destination
	 */
	public void setReplyToName(String replyTo) {
		this.replyToName = replyTo;
	}

	/**
	 * (Only used when <code>synchronous</code>=<code>true</code> and <code>replyToName</code> is set). Indicates whether the server uses the correlationId from the pipeline, the correlationId from the message or the messageId in the correlationId field of the reply. This requires the sender to have set the correlationId at the time of sending.
	 * @ff.default MESSAGEID
	 */
	public void setLinkMethod(LinkMethod method) {
		linkMethod=method;
	}

	/**
	 * (Only for <code>synchronous</code>=<code>true</code>). Maximum time in ms to wait for a reply. 0 means no timeout.
	 * @ff.default 5000
	 */
	public void setReplyTimeout(int i) {
		replyTimeout = i;
	}

	/**
	 * Value of the JMSType field
	 * @ff.default not set by application
	 */
	public void setMessageType(String string) {
		messageType = string;
	}

	/**
	 * Controls mode that messages are sent with
	 * @ff.default not set by application
	 */
	public void setDeliveryMode(DeliveryMode deliveryMode) {
		this.deliveryMode = deliveryMode;
	}


	/**
	 * Sets the priority that is used to deliver the message. Ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by JMS to 4
	 * @ff.default -1
	 */
	public void setPriority(int i) {
		priority = i;
	}

	/**
	 * If <code>true</code>, messages sent are put in a SOAP envelope
	 * @ff.default false
	 */
	public void setSoap(boolean b) {
		soap = b;
	}

	/** SOAP encoding style URI */
	public void setEncodingStyleURI(String string) {
		encodingStyleURI = string;
	}

	/** SOAP service namespace URI */
	public void setServiceNamespaceURI(String string) {
		serviceNamespaceURI = string;
	}

	/** SOAPAction string sent as message property */
	public void setSoapAction(String string) {
		soapAction = string;
	}

	/**
	 * Name of parameter containing SOAP header
	 * @ff.default soapHeader
	 */
	public void setSoapHeaderParam(String string) {
		soapHeaderParam = string;
	}

	/**
	 * session key to store SOAP header of reply
	 * @ff.default replySoapHeader
	 */
	public void setReplySoapHeaderSessionKey(String string) {
		replySoapHeaderSessionKey = string;
	}

	/** A list of JMS headers of the response to add to the PipeLineSession */
	public void setResponseHeadersToSessionKeys(String responseHeaders) {
		this.responseHeaders = responseHeaders;
	}
}
