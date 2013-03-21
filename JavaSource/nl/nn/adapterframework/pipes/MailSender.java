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
package nl.nn.adapterframework.pipes;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

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
 * @version $Id$
 * @author Johan Verrips/Gerrit van Brakel
 * @deprecated Please replace with nl.nn.adapterframework.senders.MailSender
 */
public class MailSender extends nl.nn.adapterframework.senders.MailSender {

	public void configure() throws ConfigurationException {
		ConfigurationWarnings configWarnings = ConfigurationWarnings.getInstance();
		String msg = getLogPrefix()+"The class ["+getClass().getName()+"] has been deprecated. Please change to ["+getClass().getSuperclass().getName()+"]";
		configWarnings.add(log, msg);
		super.configure();
	}
}
