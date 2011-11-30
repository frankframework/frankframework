/*
 * $Log: MailSenderPipe.java,v $
 * Revision 1.8  2011-11-30 13:51:50  europe\m168309
 * adjusted/reversed "Upgraded from WebSphere v5.1 to WebSphere v6.1"
 *
 * Revision 1.1  2011/10/19 14:49:45  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Upgraded from WebSphere v5.1 to WebSphere v6.1
 *
 * Revision 1.6  2009/02/10 10:44:10  Peter Leeuwenburgh <peter.leeuwenburgh@ibissource.org>
 * Replaced deprecated class
 *
 * Revision 1.5  2005/10/17 11:31:43  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * simplified code
 * 
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.senders.MailSender;

/**
 * Pipe that sends a mail-message using a {@link MailSender} as its sender.
 * <br/>
 * Sample email.xml:<br/><code><pre>
 *	&lt;email&gt;
 *	    &lt;recipients&gt;
 *	        &lt;recipient&gt;***@natned&lt;/recipient&gt;
 *	        &lt;recipient&gt;***@nn.nl&lt;/recipient&gt;
 *	    &lt;/recipients&gt;
 *	    &lt;from&gt;***@nn.nl&lt;/from&gt;
 *	    &lt;subject&gt;this is the subject&lt;/subject&gt;
 *	    &lt;message&gt;dit is de message&lt;/message&gt;
 *	&lt;/email&gt;
 * </pre></code> <br/>
 * Notice: it must be valid XML. Therefore, especially the message element
 * must be plain text or be wrapped as CDATA.<br/><br/>
 * example:<br/><code><pre>
 * &lt;message&gt;&lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;&lt;/message&gt;
 * </pre></code><br/>
 *
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setForwardName(String) forwardName}</td><td>name of forward returned upon completion</td><td>"success"</td></tr>
 * <tr><td>{@link MailSender#setSmtpHost(String) sender.smtpHost}</td><td>name of the host by which the messages are to be send</td><td>&nbsp;</td></tr>
 * <tr><td>{@link MailSender#setSmtpUserid(String) sender.smtpUserid}</td><td>userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link MailSender#setSmtpPassword(String) sender.smtpPassword}</td><td>password of userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link MailSender#setDefaultFrom(String) sender.defaultFrom}</td><td>value of the From: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * <tr><td>{@link MailSender#setDefaultSubject(String) sender.defaultSubject}</td><td>value of the Subject: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * <table border="1">
 * <tr><th>nested elements</th><th>description</th></tr>
 * <tr><td>{@link nl.nn.adapterframework.core.ICorrelatedPullingListener listener}</td><td>specification of a listener to listen to for replies, assuming these to arrive quickly!</td></tr>
 * </table>
 * </p>
 * <p><b>Exits:</b>
 * <table border="1">
 * <tr><th>state</th><th>condition</th></tr>
 * <tr><td>"success"</td><td>default when the message was successfully sent and no listener was specified</td></tr>
 * <tr><td><i>{@link #setForwardName(String) forwardName}</i></td><td>if specified, and otherwise under same condition as "success"</td></tr>
 * <tr><td>"timeout"</td><td>no data was received (timeout on listening), if a listener was specified.</td></tr>
 * </table>
 * </p>
 * @version Id
 * @author Johan Verrips
 */

public class MailSenderPipe extends MessageSendingPipe {
	public static final String version = "$RCSfile: MailSenderPipe.java,v $ $Revision: 1.8 $ $Date: 2011-11-30 13:51:50 $";
		
	public MailSenderPipe() {
		super();
		setSender(new MailSender());
	}
}
