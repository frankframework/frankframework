/*
   Copyright 2013, 2019 Nationale-Nederlanden

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
package nl.nn.adapterframework.senders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

/**
 * {@link nl.nn.adapterframework.core.ISender sender} that sends a mail specified by an XML message.
 * <p>
 * Sample email.xml:
 * <code><pre>
 *    &lt;email&gt;
 *       &lt;recipients&gt;
 *          &lt;recipient type="to"&gt;***@hotmail.com&lt;/recipient&gt;
 *          &lt;recipient type="cc"&gt;***@gmail.com&lt;/recipient&gt;
 *       &lt;/recipients&gt;
 *       &lt;from name="*** ***"&gt;***@yahoo.com&lt;/from&gt;
 *       &lt;subject&gt;This is the subject&lt;/subject&gt;
 *       &lt;threadTopic&gt;subject&lt;/threadTopic&gt;
 *       &lt;message&gt;This is the message&lt;/message&gt;
 *       &lt;messageType&gt;text/plain&lt;/messageType&gt;&lt;!-- Optional --&gt;
 *       &lt;messageBase64&gt;false&lt;/messageBase64&gt;&lt;!-- Optional --&gt;
 *       &lt;charset&gt;UTF-8&lt;/charset&gt;&lt;!-- Optional --&gt;
 *       &lt;attachments&gt;
 *          &lt;attachment name="filename1.txt"&gt;This is the first attachment&lt;/attachment&gt;
 *          &lt;attachment name="filename2.pdf" base64="true"&gt;JVBERi0xLjQKCjIgMCBvYmoKPDwvVHlwZS9YT2JqZWN0L1N1YnR5cGUvSW1...vSW5mbyA5IDAgUgo+PgpzdGFydHhyZWYKMzQxNDY2CiUlRU9GCg==&lt;/attachment&gt;
 *          &lt;attachment name="filename3.pdf" url="file:/c:/filename3.pdf"/&gt;
 *          &lt;attachment name="filename4.pdf" sessionKey="fileContent"/&gt;
 *       &lt;/attachments&gt;&lt;!-- Optional --&gt;
 *   &lt;/email&gt;
 * </pre></code>
 * <p>
 * Notice: the XML message must be valid XML. Therefore, especially the message element
 * must be plain text or be wrapped as CDATA. Example:
 * <code><pre>
 *    &lt;message&gt;&lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;&lt;/message&gt;
 * </pre></code>
 * <p>
 * The <code>sessionKey</code> attribute for attachment can contain an inputstream or a string. Other types are not supported at this moment.
 * <p>
 * The attribute order for attachments is as follows:
 * <ol>
 *    <li>sessionKey</li>
 *    <li>url</li>
 *    <li><i>value of the attachment element</i></li>
 * </ol>
 * <p>
 * The <code>base64</code> attribute is only used when the value of the PipeLineSession variable <code>sessionKey</code> is a String object
 * or when the value of the attachment element is used. If <code>base64=true</code> then the value will be decoded before it's used.
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setSmtpHost(String) smtpHost}</td><td>name of the host by which the messages are to be send</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) smtpAuthAlias}</td><td>alias used to obtain credentials for authentication to smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUserId(String) userId}</td><td>userId on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>password of userId on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpAuthAlias(String) smtpAuthAlias}</td><td>alias used to obtain credentials for authentication to smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpUserid(String) userId}</td><td>userId on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpPassword(String) password}</td><td>password of userId on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultFrom(String) defaultFrom}</td><td>value of the From: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultSubject(String) defaultSubject}</td><td>value of the Subject: header if not specified in message itself</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setDefaultAttachmentName(String) defaultAttachmentName}</td><td>When this name is used, it will be followed by a number which is equal to the node's position</td><td>attachment</td></tr>
 * <tr><td>{@link #setTimeout(int) timeout}</td><td>timeout (in milliseconds). Used for socket connection timeout and socket I/O timeout</td><td>20000</td></tr>
 * </table>
 * <p>
 * <table border="1">
 * <b>Parameters:</b>
 * <tr><th>name</th><th>type</th><th>remarks</th></tr>
 * <tr><td>from</td><td>string</td><td>email address of the sender</td></tr>
 * <tr><td>subject</td><td>string</td><td>subject field of the message</td></tr>
 * <tr><td>threadTopic</td><td>string</td><td>(optional) conversation field of the message, used to correlate mails in mail viewer (header field "Thread-Topic"). Note: subject must end with value of threadTopic, but cann't be exactly the same</td></tr>
 * <tr><td>message</td><td>string</td><td>message itself. If absent, the complete input message is assumed to be the message</td></tr>
 * <tr><td>messageType</td><td>string</td><td>message MIME type (at this moment only available are text/plain and text/html - default: text/plain)</td></tr>
 * <tr><td>messageBase64</td><td>boolean</td><td>indicates whether the message content is base64 encoded (default: false)</td></tr>
 * <tr><td>charSet</td><td>string</td><td>the character encoding (e.g. ISO-8859-1 or UTF-8) used to send the email (default: value of system property mail.mime.charset, when not present the value of system property file.encoding)</td></tr>
 * <tr><td>recipients</td><td>xml</td><td>recipients of the message. must result in a structure like: <code><pre>
 *       &lt;recipient type="to"&gt;***@hotmail.com&lt;/recipient&gt;
 *       &lt;recipient type="cc"&gt;***@gmail.com&lt;/recipient&gt;
 * </pre></code></td></tr>
 * <tr><td>attachments</td><td>xml</td><td>attachments to the message. must result in a structure like: <code><pre>
 *       &lt;attachment name="filename1.txt"&gt;This is the first attachment&lt;/attachment&gt;
 *       &lt;attachment name="filename2.pdf" base64="true"&gt;JVBERi0xLjQKCjIgMCBvYmoKPDwvVHlwZS9YT2JqZWN0L1N1YnR5cGUvSW1...vSW5mbyA5IDAgUgo+PgpzdGFydHhyZWYKMzQxNDY2CiUlRU9GCg==&lt;/attachment&gt;
 *       &lt;attachment name="filename3.pdf" url="file:/c:/filename3.pdf"/&gt;
 *       &lt;attachment name="filename4.pdf" sessionKey="fileContent"/&gt;
 * </pre></code></td></tr>
 * </table>
 * <p>
 * <b>Compilation and Deployment Note:</b> mail.jar (v1.2) and activation.jar must appear BEFORE j2ee.jar.
 * Otherwise errors like the following might occur: <code>NoClassDefFoundException: com/sun/mail/util/MailDateFormat</code> 
 * 
 * @author Johan Verrips
 * @author Gerrit van Brakel
 */

public class MailSender extends MailSenderBase {

	private String smtpHost;

	private Session session;
	private Properties properties;
	private String bounceAddress;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getSmtpHost())) {
			throw new ConfigurationException("MailSender [" + getName() + "] has no smtpHost configured");
		}
		properties = System.getProperties();
		try {
			properties.put("mail.smtp.host", getSmtpHost());
		} catch (Throwable t) {
			throw new ConfigurationException("MailSender [" + getName() + "] cannot set smtpHost [" + getSmtpHost() + "] in properties");
		}
		//Even though this sets mail.smtp.from, this add the Return-Path header and does not overwrite the MAIL FROM header
		if(StringUtils.isNotEmpty(getBounceAddress())) {
			if(properties.contains("mail.smtp.from")){
				properties.remove("mail.smtp.from");
			}
			properties.put("mail.smtp.from", getBounceAddress());
		}
		properties.put("mail.smtp.connectiontimeout", getTimeout() + "");
		properties.put("mail.smtp.timeout", getTimeout() + "");
		String userId = getCredentialFactory().getUsername();
		if(StringUtils.isNotEmpty(userId)) {
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.user", userId);
			properties.put("mail.smtp.password", getCredentialFactory().getPassword());
		}

		if (paramList != null) {
			paramList.configure();
		}
	}

	/**
	 * Create a <code>Session</code> and <code>Transport</code> to the smtp host.
	 */
	@Override
	public void open() throws SenderException {
		try {
			getSession();

		} catch (Exception e) {
			throw new SenderException("Error opening MailSender", e);
		}
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}

	protected Session getSession() {
		if (session == null) {
			session = Session.getInstance(properties, null);
			session.setDebug(log.isDebugEnabled());
		}
		return session;
	}

	@Override
	public void sendEmail(MailSession mailSession) throws SenderException {
		StringBuffer logBuffer = new StringBuffer();
		MimeMessage msg = createMessage(mailSession, logBuffer);
		sendEmail(mailSession, msg, logBuffer);
	}

	private void setRecipient(MailSession mailSession, MimeMessage msg, StringBuffer sb) throws UnsupportedEncodingException, MessagingException, SenderException {
		boolean recipientsFound = false;
		List<EMail> emailList = mailSession.getRecipientList();
		for (EMail recipient : emailList) {
			String value = recipient.getAddress();
			String type = recipient.getType();
			Message.RecipientType recipientType;
			if ("cc".equalsIgnoreCase(type)) {
				recipientType = Message.RecipientType.CC;
			} else if ("bcc".equalsIgnoreCase(type)) {
				recipientType = Message.RecipientType.BCC;
			} else {
				recipientType = Message.RecipientType.TO;
			}
			msg.addRecipient(recipientType, new InternetAddress(value, recipient.getName()));
			recipientsFound = true;
			if (log.isDebugEnabled()) {
				sb.append("[recipient [" + recipient + "]]");
			}
		}
		if (!recipientsFound) {
			throw new SenderException("MailSender [" + getName() + "] did not find any valid recipients");
		}
	}

	private void setAttachments(MailSession mailSession, MimeMessage msg, String messageTypeWithCharset) throws MessagingException {
		List<MailAttachmentStream> attachmentList = mailSession.getAttachmentList();
		String message = mailSession.getMessage();
		if (attachmentList == null || attachmentList.size() == 0) {
			log.debug("no attachments found to attach to mailSession");
			msg.setContent(message, messageTypeWithCharset);
		} else {
			if(log.isDebugEnabled()) log.debug("found ["+attachmentList.size()+"] attachments to attach to mailSession");
			Multipart multipart = new MimeMultipart();
			BodyPart messageBodyPart = new MimeBodyPart();
			messageBodyPart.setContent(message, messageTypeWithCharset);
			multipart.addBodyPart(messageBodyPart);

			int counter = 0;
			for (MailAttachmentStream attachment : attachmentList) {
				counter++;
				String name = attachment.getName();
				if (StringUtils.isEmpty(name)) {
					name = getDefaultAttachmentName() + counter;
				}
				log.debug("found attachment [" + attachment + "]");

				messageBodyPart = new MimeBodyPart();
				messageBodyPart.setFileName(name);

				try {
					ByteArrayDataSource bads = new ByteArrayDataSource(attachment.getContent(), attachment.getMimeType());
					bads.setName(attachment.getName());
					messageBodyPart.setDataHandler(new DataHandler(bads));
				} catch (IOException e) {
					log.error("error attaching attachment to MailSession", e);
				}

				multipart.addBodyPart(messageBodyPart);
			}
			msg.setContent(multipart);
		}
	}

	private String sendEmail(MailSession mailSession, MimeMessage msg, StringBuffer logBuffer) throws SenderException {
		checkRecipientsAndSetDefaults(mailSession);

		if (log.isDebugEnabled()) {
			logBuffer.append("MailSender [" + getName() + "] sending message ");
			logBuffer.append("[smtpHost=" + smtpHost);
			logBuffer.append("[from=" + mailSession.getFrom() + "]");
			logBuffer.append("[subject=" + mailSession.getSubject() + "]");
			logBuffer.append("[threadTopic=" + mailSession.getThreadTopic() + "]");
			logBuffer.append("[text=" + mailSession.getMessage() + "]");
			logBuffer.append("[type=" + mailSession.getMessageType() + "]");
			logBuffer.append("[base64=" + mailSession.isMessageBase64() + "]");
			logBuffer.append("[attachments=" + mailSession.getAttachmentList().size() + "]");
			log.debug(logBuffer);
		}
		if (mailSession.isMessageBase64() && StringUtils.isNotEmpty(mailSession.getMessage())) {
			byte[] message = Base64.decodeBase64(mailSession.getMessage());
			mailSession.setMessage(new String(message));
		}

		// send the message
		putOnTransport(msg);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			msg.writeTo(out);
			byte[] byteArray = out.toByteArray();
			return Misc.byteArrayToString(byteArray, "\n", false);
		} catch (Exception e) {
			throw new SenderException("Error occurred while sending email", e);
		}

	}

	private void checkRecipientsAndSetDefaults(MailSession mailSession) throws SenderException {
		List<EMail> recipientList = mailSession.getRecipientList();
		if (recipientList == null || recipientList.size() == 0) {
			throw new SenderException("MailSender [" + getName() + "] has no recipients for message");
		}
		if (StringUtils.isEmpty(mailSession.getFrom().getAddress())) {
			mailSession.getFrom().setAddress(getDefaultFrom());
		}
		if (StringUtils.isEmpty(mailSession.getSubject())) {
			mailSession.setSubject(getDefaultSubject());
		}
		log.debug("MailSender [" + getName() + "] requested to send message from [" + mailSession.getFrom().getAddress()
				+ "] subject [" + mailSession.getSubject() + "] to #recipients [" + recipientList.size() + "]");
		if (StringUtils.isEmpty(mailSession.getMessageType())) {
			mailSession.setMessageType(getDefaultMessageType());
		}
		if (mailSession.isMessageBase64()) {
			mailSession.setMessageBase64(isDefaultMessageBase64());
		}

	}

	private MimeMessage createMessage(MailSession mailSession, StringBuffer logBuffer) throws SenderException {
		MimeMessage msg = new MimeMessage(session);
		try {
			msg.setFrom(new InternetAddress(mailSession.getFrom().getAddress(), mailSession.getFrom().getName()));
		} catch (Exception e) {
			throw new SenderException("Error occurred while setting sender email", e);
		}

		try {
			msg.setSubject(mailSession.getSubject(), mailSession.getCharSet());
		} catch (MessagingException e) {
			throw new SenderException("Error occurred while setting subject", e);
		}

		if (StringUtils.isNotEmpty(mailSession.getThreadTopic())) {
			try {
				msg.setHeader("Thread-Topic", mailSession.getThreadTopic());
			} catch (MessagingException e) {
				throw new SenderException("Error occurred while setting thread topic", e);
			}
		}

		try {
			setRecipient(mailSession, msg, logBuffer);
		} catch (Exception e) {
			throw new SenderException("Error occurred while processing recipients", e);
		}

		String charSet = mailSession.getCharSet();
		String messageType = mailSession.getMessageType();
		String messageTypeWithCharset = setCharSet(charSet, messageType);

		try {
			setAttachments(mailSession, msg, messageTypeWithCharset);
		} catch (MessagingException e) {
			throw new SenderException("Error occurred while processing attachments", e);
		}

		Collection<Node> headers = mailSession.getHeaders();
		try {
			setHeader(headers, msg);
		} catch (MessagingException e) {
			throw new SenderException("Error occurred while setting header", e);
		}

		log.debug(logBuffer.toString());
		try {
			msg.setSentDate(new Date());
		} catch (MessagingException e) {
			throw new SenderException("Error occurred while setting the date", e);
		}

		try {
			msg.saveChanges();
		} catch (MessagingException e) {
			throw new SenderException("Error occurred while composing email", e);
		}

		return msg;
	}

	private void setHeader(Collection<Node> headers, MimeMessage msg) throws MessagingException {
		if (headers != null && headers.size() > 0) {
			for (Node headerElement : headers) {
				String headerName = ((Element) headerElement).getAttribute("name");
				String headerValue = XmlUtils.getStringValue(((Element) headerElement));
				msg.addHeader(headerName, headerValue);
			}
		}
	}

	private String setCharSet(String charSet, String messageType) {
		String messageTypeWithCharset;
		String charset = charSet;
		if (charset == null) {
			charset = System.getProperty("mail.mime.charset");
			if (charset == null) {
				charset = System.getProperty("file.encoding");
			}
		}
		if (charset != null) {
			messageTypeWithCharset = messageType + ";charset=" + charset;
		} else {
			messageTypeWithCharset = messageType;
		}
		log.debug("MailSender [" + getName() + "] uses encoding [" + messageTypeWithCharset + "]");
		return messageTypeWithCharset;
	}

	protected void putOnTransport(Message msg) throws SenderException {
		// connect to the transport 
		Transport transport = null;
		try {
			transport = session.getTransport("smtp");
			transport.connect(getSmtpHost(), getCredentialFactory().getUsername(),
					getCredentialFactory().getPassword());
			if (log.isDebugEnabled()) {
				log.debug("MailSender [" + getName() + "] connected transport to URL [" + transport.getURLName() + "]");
			}
			transport.sendMessage(msg, msg.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			throw new SenderException(
					"MailSender [" + getName() + "] cannot connect send message to smtpHost [" + getSmtpHost() + "]",
					e);
		} finally {
			if (transport != null) {
				try {
					transport.close();
				} catch (MessagingException e1) {
					log.warn("MailSender [" + getName() + "] got exception closing connection", e1);
				}
			}
		}
	}

	/**
	 * Name of the SMTP Host.
	 */
	@IbisDoc({ "name of the host by which the messages are to be send", "" })
	public void setSmtpHost(String newSmtpHost) {
		smtpHost = newSmtpHost;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setProperties(Properties properties) {
		this.properties = properties;
	}

	@IbisDoc({ "NDR return address when mail cannot be delivered. This adds a Return-Path header", "MAIL FROM attribute" })
	public void setBounceAddress(String string) {
		bounceAddress = string;
	}
	public String getBounceAddress() {
		return bounceAddress;
	}

}
