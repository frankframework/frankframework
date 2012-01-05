/*
 * $Log: EsbJmsSender.java,v $
 * Revision 1.1  2012-01-05 09:59:16  europe\m168309
 * initial version
 *
 *
 */
package nl.nn.adapterframework.extensions.esb;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.jms.JmsSender;
import nl.nn.adapterframework.parameters.Parameter;

import org.apache.commons.lang.StringUtils;

/**
 * ESB (Enterprise Service Bus) extension of JmsSender.
 *
 * <p><b>Configuration </b><i>(where deviating from JmsSender)</i><b>:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>classname</td><td>nl.nn.adapterframework.extensions.bis.BisSoapJmsSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMessageProtocol(String) messageProtocol}</td><td>protocol of ESB service to be called. Possible values 
 * <ul>
 *   <li>"FF": Fire & Forget protocol</li>
 *   <li>"RR": Request-Reply protocol</li>
 * </ul></td><td><td>&nbsp;</td></td></tr>
 * <tr><td>{@link #setTimeOut(long) timeOut}</td><td>receiver timeout, in milliseconds</td><td>20000 (20s)</td></tr>
 * <tr><td>{@link #setMessageTimeToLive(String) messageTimeToLive}</td><td>if messageProtocol=<code>RR</code>: </td><td>@link #setTimeOut(long) timeOut}</td></tr>
 * <tr><td>{@link #setReplyTimeout(String) replyTimeout}</td><td>if messageProtocol=<code>RR</code>: </td><td>@link #setTimeOut(long) timeOut}</td></tr>
 * <tr><td>{@link #setSynchronous(String) synchronous}</td><td>if messageProtocol=<code>RR</code>: </td><td><code>true</code></td></tr>
 * <tr><td>{@link #setSoapAction(String) soapAction}</td><td>&nbsp;</td><td>is derived from the element MessageHeader/To/Location in the SOAP header of the input message</td></tr>
 * </table></p>
 * 
 * @author  Peter Leeuwenburgh
 * @version Id
 */
public class EsbJmsSender extends JmsSender {
	private final static String REQUEST_REPLY = "RR";
	private final static String FIRE_AND_FORGET = "FF";

	private String messageProtocol = null;
	private long timeOut = 20000;

	public void configure() throws ConfigurationException {
		if (getMessageProtocol() == null) {
			throw new ConfigurationException(getLogPrefix() + "messageProtocol must be set");
		}
		if (!getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY) && !getMessageProtocol().equalsIgnoreCase(FIRE_AND_FORGET)) {
			throw new ConfigurationException(getLogPrefix() + "illegal value for messageProtocol [" + getMessageProtocol() + "], must be '" + REQUEST_REPLY + "' or '" + FIRE_AND_FORGET + "'");
		}
		if (getMessageProtocol().equalsIgnoreCase(REQUEST_REPLY)) {
			setMessageTimeToLive(getTimeOut());
			setReplyTimeout((int) getTimeOut());
			setSynchronous(true);
		} else {
			if (getReplyTo() != null) {
				throw new ConfigurationException(getLogPrefix() + "replyToName [" + getReplyTo() + "] must not be set for messageProtocol [" + getMessageProtocol() + "]");
			}
		}
		if ((paramList == null || paramList.findParameter("SoapAction") == null)) {
			Parameter p = new Parameter();
			p.setName("SoapAction");
			p.setStyleSheetName("/xml/xsl/esb/soapAction.xsl");
			p.setRemoveNamespaces(true);
			if (StringUtils.isNotEmpty(getSoapAction())) {
				p.setDefaultValue(getSoapAction());
			}
			addParameter(p);
		}
		super.configure();
	}

	public void setMessageProtocol(String string) {
		messageProtocol = string;
	}

	public String getMessageProtocol() {
		return messageProtocol;
	}

	public long getTimeOut() {
		return timeOut;
	}

	public void setTimeOut(long l) {
		timeOut = l;
	}
}
