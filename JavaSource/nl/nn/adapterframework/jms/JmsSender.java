/*
 * $Log: JmsSender.java,v $
 * Revision 1.19  2005-10-20 15:44:50  europe\L190409
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPostboxSender;
import nl.nn.adapterframework.core.ISenderWithParameters;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;

import org.apache.commons.lang.builder.ToStringBuilder;

import javax.jms.JMSException;
import javax.jms.Session;
import javax.jms.MessageProducer;
import javax.jms.Message;

/**
 * This class sends messages with JMS.
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.jms.JmsSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the sender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageTimeToLive(long) messageTimeToLive}</td><td>&nbsp;</td><td>0</td></tr>
 * <tr><td>{@link #setMessageType(boolean) messageType}</td><td>value of the JMSType field</td><td>not set by application</td></tr>
 * <tr><td>{@link #setDeliveryMode(boolean) deliveryMode}</td><td>controls mode that messages are sent with: either 'persistent' or 'non_persistent'</td><td>not set by application</td></tr>
 * <tr><td>{@link #setPriority(int) priority}</td><td>sets the priority that is used to deliver the message. ranges from 0 to 9. Defaults to -1, meaning not set. Effectively the default priority is set by Jms to 4</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>&nbsp;</td><td>AUTO_ACKNOWLEDGE</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setReplyToName(String) replyToName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel
 * @version Id
 */

public class JmsSender extends JMSFacade implements ISenderWithParameters, IPostboxSender {
	public static final String version="$RCSfile: JmsSender.java,v $ $Revision: 1.19 $ $Date: 2005-10-20 15:44:50 $";
	private String replyToName = null;
	private int deliveryMode = 0;
	private String messageType = null;
	private int priority=-1;
	
	
	protected ParameterList paramList = null;

	/**
	 * Configures the sender
	 */
	public void configure() throws ConfigurationException {
		if (paramList!=null) {
			paramList.configure();
		}
		super.configure();
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


	public boolean isSynchronous() {
		return false;
	}

	/**
	 * @see nl.nn.adapterframework.core.ISender#sendMessage(java.lang.String, java.lang.String)
	*/
	public String sendMessage(String correlationID, String message) throws SenderException {
		return sendMessage(correlationID, message, null);
	}

	/** 
	 * @see nl.nn.adapterframework.core.IPostboxSender#sendMessage(java.lang.String, java.lang.String, java.util.ArrayList)
	 */
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException {
		Session s = null;
		MessageProducer mp = null;

		try {
			s = createSession();
			mp = getMessageProducer(s, getDestination());

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
			if (prc != null && paramList != null) {
				setProperties(msg, prc.getValues(paramList));
			}
			if (null != replyToName) {
				msg.setJMSReplyTo(getDestination(replyToName));
				log.debug("replyTo set to [" + msg.getJMSReplyTo().toString() + "]");
			}

			// send message	
			send(mp, msg);
			if (log.isInfoEnabled()) {
				log.info(
					"[" + getName() + "] " + "sent Message: [" + message + "] " + "to [" + getDestinationName()
						+ "] " + "msgID [" + msg.getJMSMessageID() + "] " + "correlationID [" + msg.getJMSCorrelationID()
						+ "] " + "using " + (getPersistent() ? "persistent" : "non-persistent") + " mode "
						+ ((replyToName != null) ? "replyTo:" + replyToName : ""));
			}
			return msg.getJMSMessageID();
		}
		catch (Throwable e) {
			log.error("JmsSender [" + getName() + "] got exception: " + ToStringBuilder.reflectionToString(e), e);
			throw new SenderException(e);
		}
		finally {
			if (mp != null) { 
				try { 
					mp.close(); 
				} catch (JMSException e) { 
					log.warn("JmsSender [" + getName() + "] got exception closing message producer",e); 
				}
			}
			if (s != null) {
				 try { 
				 	s.close(); 
				 } catch (JMSException e) { 
					log.warn("JmsSender [" + getName() + "] got exception closing session",e); 
				 }
			}
		}
	}

	/**
	 * sets the JMS message properties as descriped in the msgProperties arraylist
	 * @param msg
	 * @param msgProperties
	 * @throws JMSException
	 */
	private void setProperties(final Message msg, ParameterValueList msgProperties) throws JMSException {
		for (int i=0; i<msgProperties.size(); i++) {
			ParameterValue property = msgProperties.getParameterValue(i);
			String type = property.getDefinition().getType();
			String name = property.getDefinition().getName();

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
	
	public String toString() {
		String result = super.toString();
		ToStringBuilder ts = new ToStringBuilder(this);
		ts.append("name", getName());
		ts.append("version", version);
		ts.append("replyToName", replyToName);
		result += ts.toString();
		return result;

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
			log.warn("unknown delivery mode ["+deliveryMode+"], delivery mode not changed");
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

}
