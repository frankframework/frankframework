/*
 * $Log: JmsSender.java,v $
 * Revision 1.9  2004-08-16 11:27:56  L190409
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

import java.util.ArrayList;
import java.util.Iterator;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPostboxSender;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.ParameterValue;
import nl.nn.adapterframework.core.ParameterValueResolver;
import nl.nn.adapterframework.core.SenderException;
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
 * <tr><td>{@link #setName(String) name}</td>  <td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationName(String) destinationName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDestinationType(String) destinationType}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageTimeToLive(long) messageTimeToLive}</td><td>&nbsp;</td><td>0</td></tr>
 * <tr><td>{@link #setPersistent(boolean) persistent}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAcknowledgeMode(String) acknowledgeMode}</td><td>&nbsp;</td><td>AUTO_ACKNOWLEDGE</td></tr>
 * <tr><td>{@link #setTransacted(boolean) transacted}</td><td>&nbsp;</td><td>false</td></tr>
 * <tr><td>{@link #setReplyToName(String) ReplyToName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setJmsRealm(String) jmsRealm}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * @version Id
 *
 * @author Gerrit van Brakel
 */

public class JmsSender extends JMSFacade implements ISender, IPostboxSender {
	public static final String version = "$Id: JmsSender.java,v 1.9 2004-08-16 11:27:56 L190409 Exp $";
	private String replyToName = null;

	public JmsSender() {
		super();
	}

	/**
	 * Starts the sender 
	 */
	public void open() throws SenderException {
		try {
			super.open();
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
			super.close();
		}
		catch (Throwable e) {
			throw new SenderException("JmsMessageSender [" + getName() + "] " + "got error occured stopping sender", e);
		}
	}

	/**
	 * Configures the sender
	 */
	public void configure() throws ConfigurationException {
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
	public String sendMessage(String correlationID, String message, ArrayList msgProperties) throws SenderException {
		Session s = null;
		MessageProducer mp = null;

		try {
			s = createSession();
			mp = getMessageProducer(s, getDestination());

			// create message
			Message msg = createTextMessage(s, correlationID, message);

			// set properties
			if (null != msgProperties)
				setProperties(msg, msgProperties);
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
			if (mp != null) try { mp.close(); } catch (JMSException e) { }
			if (s != null) try { s.close(); } catch (JMSException e) { }
		}
	}

	/**
	 * sets the JMS message properties as descriped in the msgProperties arraylist
	 * @param msg
	 * @param msgProperties
	 * @throws JMSException
	 */
	private void setProperties(final Message msg, ArrayList msgProperties) throws JMSException {
		for (Iterator it = msgProperties.iterator(); it.hasNext();) {
			ParameterValue property = (ParameterValue) it.next();
			String type = property.getType().getType();
			String name = property.getType().getName();

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
	
	public String getReplyTo() {
		return replyToName;
	}

	public void setReplyToName(String replyTo) {
		this.replyToName = replyTo;
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
}
