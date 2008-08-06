/*
 * $Log: MailSender.java,v $
 * Revision 1.15  2008-08-06 16:38:20  europe\L190409
 * moved from pipes to senders package
 *
 * Revision 1.14  2008/05/15 15:12:51  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * allow to send messages without parameters
 *
 * Revision 1.13  2007/02/12 14:02:19  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * Logger from LogUtil
 *
 * Revision 1.12  2005/12/19 16:37:13  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * corrected version string
 *
 * Revision 1.11  2005/12/19 16:36:09  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added authentication using authentication-alias
 *
 * Revision 1.10  2005/04/26 09:21:22  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * added parameters messageType, messageBase64 and attachment[@base64] (by Peter Leeuwenburgh)
 *
 * Revision 1.1  2005/04/21 13:37:15  NNVZNL01#L168309
 * added parameters messageType, messageBase64 and attachment[@base64]
 *
 * Revision 1.9  2004/10/26 07:45:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * check if any recipients are found
 *
 * Revision 1.8  2004/10/19 16:12:29  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * made Transport per thread instead of per instance
 *
 * Revision 1.7  2004/10/19 13:53:45  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * graceful handling of empty recipients
 *
 * Revision 1.6  2004/10/19 06:39:20  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * modified parameter handling, introduced IWithParameters
 *
 * Revision 1.5  2004/10/14 16:13:28  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * parametrization and adding of attachments
 *
 * Revision 1.4  2004/03/26 10:42:34  Johan Verrips <johan.verrips@ibissource.org>
 * added @version tag in javadoc
 *
 * Revision 1.3  2004/03/24 13:58:36  Gerrit van Brakel <gerrit.van.brakel@ibissource.org>
 * removed TimeOutException
 *
 */
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 * {@link ISender} that sends a mail specified by an XML message. <br/>
 *
 * Sample email.xml:<br/><code><pre>
 *	&lt;email&gt;
 *	    &lt;recipients&gt;
 *		&lt;recipient type="to"&gt;***@natned&lt;/recipient&gt;
 *	        &lt;recipient type="cc"&gt;***@nn.nl&lt;/recipient&gt;
 *	    &lt;/recipients&gt;
 *	    &lt;from&gt;***@nn.nl&lt;/from&gt;
 *	    &lt;subject&gt;this is the subject&lt;/subject&gt;
 *	    &lt;message&gt;dit is de message&lt;/message&gt;
 *	    &lt;attachments&gt;
 *	        &lt;attachment name="filename1.txt" type="text"&gt;<i>contents of first attachment</i>&lt;/attachment&gt;
 *	        &lt;attachment name="filename2.txt" type="text" url="url-to-resource" base64="false"&gt;<i>this is an attachment with a resource</i>&lt;/attachment&gt;
 *	    &lt;/attachments&gt;
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
 * <tr><td>{@link #setSmtpHost(String) smtpHost}</td><td>name of the host by which the messages are to be send</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpAuthAlias(String) smtpAuthAlias}</td><td>alias used to obtain credentials for authentication to smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpUserid(String) smtpUserid}</td><td>userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpPassword(String) smtpPassword}</td><td>password of userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultFrom(String) defaultFrom}</td><td>value of the From: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultSubject(String) defaultSubject}</td><td>value of the Subject: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultAttachmentType(String) defaultAttachmentType}</td><td>&nbsp;</td><td>text</td></tr>
 * <tr><td>{@link #setDefaultAttachmentName(String) defaultAttachmentName}</td><td>&nbsp;</td><td>attachment</td></tr>
 * </table>
 * <table border="1">
 * <p><b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>from</td><td>string</td><td>email address of the sender</td></tr>
 * <tr><td>subject</td><td>string</td><td>subject field of the message</td></tr>
 * <tr><td>message</td><td>string</td><td>message itself. If absent, the complete input message is assumed to be the message</td></tr>
 * <tr><td>messageType</td><td>string</td><td>message MIME type (at this moment only available are text/plain and text/html - default: text/plain)</td></tr>
 * <tr><td>messageBase64</td><td>boolean</td><td>indicates whether the message content is base64 encoded (default: false)</td></tr>
 * <tr><td>recipients</td><td>xml</td><td>recipients of the message. must result in a structure like: <code><pre>
 *	        &lt;recipient type="to"&gt;***@natned&lt;/recipient&gt;
 *	        &lt;recipient type="cc"&gt;***@nn.nl&lt;/recipient&gt;
* </pre></code></td></tr>
 * <tr><td>attachments</td><td>xml</td><td>attachments to the message. must result in a structure like: <code><pre>
 *	        &lt;attachment name="filename1.txt" type="text"&gt;<i>contents of first attachment</i>&lt;/attachment&gt;
 *	        &lt;attachment name="filename2.txt" type="text" url="url-to-resource" base64="false"&gt;<i>this is an attachment with a resource</i>&lt;/attachment&gt;
 * </pre></code></td></tr>
 * </table>
 * </p>
 * NB Compilation and Deployment Note: mail.jar (v1.2) and activation.jar must appear BEFORE j2ee.jar
 * Otherwise errors like the following might occur:
 *   NoClassDefFoundException: com/sun/mail/util/MailDateFormat 
 * 
 * @version Id
 * @author Johan Verrips/Gerrit van Brakel
 * @deprecated Please replace with nl.nn.adapterframework.senders.MailSender
 */
public class MailSender extends nl.nn.adapterframework.senders.MailSender {

	public void configure() throws ConfigurationException {
		log.warn("The class ["+getClass().getName()+"] has been deprecated. Please change to ["+super.getClass().getName()+"]");
		super.configure();
	}
}
