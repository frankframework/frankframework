/*
 * $Log: JmsSender.java,v $
 * Revision 1.55  2012-01-10 10:48:22  europe\m168309
 * modified logging
 *
 * Revision 1.54  2012/01/05 10:00:58  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved addEsbSoapAction attribute to EsbJmsSender
 *
 * Revision 1.53  2012/01/04 10:52:46  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * adjusted addEsbSoapAction attribute
 *
 * Revision 1.52  2011/12/30 09:39:31  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added addEsbSoapAction attribute
 *
 * Revision 1.51  2011/12/05 15:34:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved warning for delivery mode
 *
 * Revision 1.43  2011/06/06 14:32:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed NPE in getting contents of reply message
 *
 * Revision 1.42  2011/06/06 12:26:16  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * added soapHeader to method sendMessage
 *
 * Revision 1.41  2011/02/07 13:10:57  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * updated javadoc
 *
 * Revision 1.40  2010/04/27 09:25:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * improved logging of waiting for reply
 *
 * Revision 1.39  2010/03/22 11:08:13  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * moved message logging from INFO level to DEBUG level
 *
 * Revision 1.38  2010/03/10 14:30:05  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * rolled back testtool adjustments (IbisDebuggerDummy)
 *
 * Revision 1.36  2010/01/28 14:59:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * renamed 'Connection' classes to 'MessageSource'
 *
 * Revision 1.35  2009/09/17 08:21:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed timeout handling of JmsSender
 *
 * Revision 1.34  2009/09/09 13:54:24  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.33  2009/09/09 07:34:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.32  2009/09/09 07:15:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed IDoubleAsynchronous
 *
 * Revision 1.31  2009/09/08 14:22:07  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made linkMethod controllable using IDoubleASynchronous
 *
 * Revision 1.30  2009/08/24 08:21:14  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * support for reply using dynamic reply queue
 *
 * Revision 1.29  2009/07/28 12:42:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * avoid NPE evaluating soapHeaderParam
 *
 * Revision 1.28  2008/09/01 12:58:17  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified logging
 *
 * Revision 1.27  2008/08/07 11:36:33  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added support for SoapHeaders
 *
 * Revision 1.26  2008/05/15 14:56:40  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added Soap support
 *
 * Revision 1.25  2007/05/11 09:50:37  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.24  2006/10/13 08:15:18  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * update javadoc
 *
 * Revision 1.23  2006/01/05 14:30:15  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * updated javadoc
 *
 * Revision 1.22  2005/12/29 15:15:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.21  2005/12/28 08:46:30  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected javadoc
 *
 * Revision 1.20  2005/12/20 16:59:25  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * implemented support for connection-pooling
 *
 * Revision 1.19  2005/10/20 15:44:50  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified JMS-classes to use shared connections
 * open()/close() became openFacade()/closeFacade()
 *
 * Revision 1.18  2005/09/13 15:40:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * log exceptions on closing the sender
 *
 * Revision 1.17  2005/08/02 07:13:05  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * deliveryMode to String and vv
 *
 * Revision 1.16  2005/07/05 11:54:03  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected deliveryMode handling; added priority-attribute
 *
 * Revision 1.15  2005/06/20 09:10:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added outputType attribute
 * added deliveryMode attribute
 *
 * Revision 1.14  2005/06/13 09:58:59  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * cosmetic changes
 *
 * Revision 1.13  2004/10/19 06:39:21  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling, introduced IWithParameters
 *
 * Revision 1.12  2004/10/12 15:12:34  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * reworked handling of  ParameterValueList
 *
 * Revision 1.11  2004/10/05 10:43:58  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made into parameterized sender
 *
 * Revision 1.10  2004/09/01 07:30:00  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * correction in documentation
 *
 * Revision 1.9  2004/08/16 11:27:56  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * changed timeToLive back to messageTimeToLive
 *
 * Revision 1.8  2004/08/16 09:26:01  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected JavaDoc
 *
 * Revision 1.7  2004/05/21 07:59:30  unknown <unknown@ibissource.org>
 * Add (modifications) due to the postbox sender implementation
 *
 * Revision 1.6  2004/03/31 12:04:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * fixed javadoc
 *
 * Revision 1.5  2004/03/26 10:42:55  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.4  2004/03/26 09:50:51  Johan Verrips <johan.verrips@ibissource.org>
 * Updated javadoc
 *
 * Revision 1.3  2004/03/23 18:22:39  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * enabled Transaction control
 *
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
 * <tr><td>{@link #setLinkMethod(String) linkMethod}</td><td>(Only used when synchronous="true" and and replyToName is set) Eithter 'MESSAGEID' or 'CORRELATIONID'. Indicates wether the server uses the correlationID or the messageID in the correlationID field of the reply. This requires the sender to have set the correlationID at the time of sending.</td><td>MESSAGEID</td></tr>
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
 * @version Id
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
