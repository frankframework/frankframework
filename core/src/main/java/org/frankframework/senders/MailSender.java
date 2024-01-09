/*
   Copyright 2013, 2019 Nationale-Nederlanden, 2020, 2022-2023 WeAreFrank!

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
package org.frankframework.senders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.SenderException;
import org.frankframework.doc.Category;
import org.frankframework.util.Misc;
import org.frankframework.util.XmlUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.mail.smtp.SMTPMessage;

import jakarta.activation.DataHandler;
import jakarta.mail.BodyPart;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;
import lombok.Getter;
import lombok.Setter;

/**
 * {@link ISender sender} that sends a mail specified by an XML message.
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
 * </p><p>
 * Notice: the XML message must be valid XML. Therefore, especially the message element
 * must be plain text or be wrapped as CDATA. Example:
 * <code><pre>
 *    &lt;message&gt;&lt;![CDATA[&lt;h1&gt;This is a HtmlMessage&lt;/h1&gt;]]&gt;&lt;/message&gt;
 * </pre></code>
 * </p><p>
 * The <code>sessionKey</code> attribute for attachment can contain an inputstream or a string. Other types are not supported at this moment.
 * </p><p>
 * The attribute order for attachments is as follows:
 * <ol>
 *    <li>sessionKey</li>
 *    <li>url</li>
 *    <li><i>value of the attachment element</i></li>
 * </ol>
 * </p><p>
 * The <code>base64</code> attribute is only used when the value of the PipeLineSession variable <code>sessionKey</code> is a String object
 * or when the value of the attachment element is used. If <code>base64=true</code> then the value will be decoded before it's used.
 * </p><p>
 * <b>Compilation and Deployment Note:</b> mail.jar (v1.2) and activation.jar must appear BEFORE j2ee.jar.
 * Otherwise errors like the following might occur: <code>NoClassDefFoundException: com/sun/mail/util/MailDateFormat</code>
 * </p>
 * @author Johan Verrips
 * @author Gerrit van Brakel
 */

@Category("Advanced")
public class MailSender extends MailSenderBase {

	private @Getter String smtpHost;
	private @Getter int smtpPort=25;
	/**
	 * When set to true, we ensure TLS is being used
	 *
	 * @ff.default false
	 */
	private @Setter boolean useSsl = false;

	private Properties properties = new Properties();
	private Session session = null;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (StringUtils.isEmpty(getSmtpHost())) {
			throw new ConfigurationException("MailSender [" + getName() + "] has no smtpHost configured");
		}

		properties.put("mail.smtp.host", getSmtpHost());
		properties.put("mail.smtp.port", getSmtpPort());
		properties.put("mail.smtp.connectiontimeout", getTimeout() + "");
		properties.put("mail.smtp.timeout", getTimeout() + "");
		String userId = getCredentialFactory().getUsername();
		if(StringUtils.isNotEmpty(userId)) {
			properties.put("mail.smtp.auth", "true");
			properties.put("mail.smtp.user", userId);
			properties.put("mail.smtp.password", getCredentialFactory().getPassword());
		}
		//Even though this is called mail.smtp.from, it actually adds the Return-Path header and does not overwrite the MAIL FROM header
		if(StringUtils.isNotEmpty(getBounceAddress())) {
			properties.put("mail.smtp.from", getBounceAddress());
		}

		if (useSsl) {
			properties.put("mail.smtp.starttls.enable", "true");
		}
	}

	/**
	 * Create a session to validate connectivity
	 */
	@Override
	public void open() throws SenderException {
		createSession(); //Test connection to SMTP host
	}

	/**
	 * Create the session during runtime
	 */
	protected Session createSession() throws SenderException {
		try {
			if(session == null) {
				session = Session.getInstance(properties, null);
				session.setDebug(log.isDebugEnabled());
			}

			return session;
		}
		catch (Exception e) {
			throw new SenderException("MailSender got error", e);
		}
	}

	@Override
	public String sendEmail(MailSessionBase mailSession) throws SenderException {
		Session session = createSession();
		log.debug("sending mail using session [{}]", session);
		return sendEmail(session, (MailSession)mailSession);
	}

	@Override
	protected MailSession createMailSession() throws SenderException {
		return new MailSession();
	}

	private void setAttachments(MailSessionBase mailSession, MimeMessage msg, String messageTypeWithCharset) throws MessagingException {
		List<MailAttachmentStream> attachmentList = mailSession.getAttachmentList();
		String message = mailSession.getMessage();
		if (attachmentList == null || attachmentList.isEmpty()) {
			log.debug("no attachments found to attach to mailSession");
			msg.setContent(message, messageTypeWithCharset);
		} else {
			log.debug("found [{}] attachments to attach to mailSession", attachmentList::size);
			Multipart multipart = new MimeMultipart();
			BodyPart messagePart = new MimeBodyPart();
			messagePart.setContent(message, messageTypeWithCharset);
			multipart.addBodyPart(messagePart);

			int counter = 0;
			for (MailAttachmentStream attachment : attachmentList) {
				counter++;
				String name = attachment.getName();
				if (StringUtils.isEmpty(name)) {
					name = getDefaultAttachmentName() + counter;
				}
				log.debug("found attachment [{}]", attachment);

				BodyPart messageBodyPart = new MimeBodyPart();
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

	private String sendEmail(Session session, MailSession mailSession) throws SenderException {
		StringBuilder logBuffer = new StringBuilder();

		if (log.isDebugEnabled()) {
			logBuffer.append("MailSender [" + getName() + "] sending message ");
			logBuffer.append("[smtpHost=" + getSmtpHost());
			logBuffer.append("[from=" + mailSession.getFrom() + "]");
			logBuffer.append("[subject=" + mailSession.getSubject() + "]");
			logBuffer.append("[threadTopic=" + mailSession.getThreadTopic() + "]");
			logBuffer.append("[text=" + mailSession.getMessage() + "]");
			logBuffer.append("[type=" + mailSession.getMessageType() + "]");
			logBuffer.append("[base64=" + mailSession.isMessageBase64() + "]");
			logBuffer.append("[attachments=" + mailSession.getAttachmentList().size() + "]");
			log.debug(logBuffer);
		}

		MimeMessage msg = createMessage(session, mailSession, logBuffer);

		// send the message
		// Only send if some recipients remained after whitelisting
		if (mailSession.hasWhitelistedRecipients()) {
			putOnTransport(session, msg);
		} else if (log.isDebugEnabled()) {
			log.debug("No recipients left after whitelisting, mail is not send");
		}

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			msg.writeTo(out);
			byte[] byteArray = out.toByteArray();
			return Misc.byteArrayToString(byteArray, "\n", false);
		} catch (Exception e) {
			throw new SenderException("Error occurred while sending email", e);
		}
	}

	private MimeMessage createMessage(Session session, MailSession mailSession, StringBuilder logBuffer) throws SenderException {
		SMTPMessage msg = new SMTPMessage(session);
		mailSession.setSmtpMessage(msg);

		try {
			msg.setFrom(mailSession.getFrom().getInternetAddress());
		} catch (Exception e) {
			throw new SenderException("Error occurred while setting sender email", e);
		}

		if (mailSession.getReplyTo() != null) {
			try {
				msg.setReplyTo(new InternetAddress[]{mailSession.getReplyTo().getInternetAddress()});
			} catch (Exception e) {
				throw new SenderException("Error occurred while setting replyTo email", e);
			}
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

		if (StringUtils.isNotEmpty(mailSession.getBounceAddress())) {
			msg.setEnvelopeFrom(mailSession.getBounceAddress());
		}

		try {
			mailSession.setRecipientsOnMessage(logBuffer);
		} catch (Exception e) {
			throw new SenderException("Error occurred while processing recipients", e);
		}

		String charSet = mailSession.getCharSet();
		String messageType = mailSession.getMessageType();
		String messageTypeWithCharset = setCharSet(charSet, messageType);

		if (mailSession.isMessageBase64() && StringUtils.isNotEmpty(mailSession.getMessage())) {
			if(!Base64.isBase64(mailSession.getMessage())) {
				throw new SenderException("Input message contains invalid Base64 characters");
			}
			byte[] message = Base64.decodeBase64(mailSession.getMessage());
			mailSession.setMessage(new String(message));
		}

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

		if (log.isDebugEnabled()) {
			log.debug(logBuffer.toString());
		}
		try {
			msg.setSentDate(new Date());
		} catch (MessagingException e) {
			throw new SenderException("Error occurred while setting the date", e);
		}

		try {
			msg.saveChanges();
		} catch (Exception e) {
			throw new SenderException("Error occurred while composing email", e);
		}

		return msg;
	}

	private void setHeader(Collection<Node> headers, MimeMessage msg) throws MessagingException {
		if (headers != null && !headers.isEmpty()) {
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
		log.debug("MailSender {}] uses encoding [{}]", getName(), messageTypeWithCharset);
		return messageTypeWithCharset;
	}

	private void putOnTransport(Session session, Message msg) throws SenderException {
		// connect to the transport
		Transport transport = null;
		try {
			transport = session.getTransport("smtp");
			transport.connect(getSmtpHost(), getCredentialFactory().getUsername(), getCredentialFactory().getPassword());
			log.debug("MailSender [{}] connected transport to URL [{}]", getName(), transport.getURLName());
			transport.sendMessage(msg, msg.getAllRecipients());
		} catch (Exception e) {
			throw new SenderException("MailSender [" + getName() + "] cannot connect send message to smtpHost [" + getSmtpHost() + "]", e);
		} finally {
			if (transport != null) {
				try {
					transport.close();
				} catch (MessagingException e1) {
					log.warn("MailSender [{}] got exception closing connection", getName(), e1);
				}
			}
		}
	}

	/** Name of the SMTP-host by which the messages are to be send */
	public void setSmtpHost(String newSmtpHost) {
		smtpHost = newSmtpHost;
	}

	/**
	 * Port of the SMTP-host by which the messages are to be send
	 * @ff.default 25
	 */
	public void setSmtpPort(int newSmtpPort) {
		smtpPort = newSmtpPort;
	}

	public class MailSession extends MailSessionBase {
		private @Getter @Setter SMTPMessage smtpMessage = null;

		public MailSession() throws SenderException {
			super();
		}

		@Override
		protected void addRecipientToMessage(EMail recipient) throws SenderException {
			String type = recipient.getType();
			Message.RecipientType recipientType;
			if ("cc".equalsIgnoreCase(type)) {
				recipientType = Message.RecipientType.CC;
			} else if ("bcc".equalsIgnoreCase(type)) {
				recipientType = Message.RecipientType.BCC;
			} else {
				recipientType = Message.RecipientType.TO;
			}

			try {
				smtpMessage.addRecipient(recipientType, recipient.getInternetAddress());
			} catch (Exception e) {
				throw new SenderException("Error occurred while processing recipients", e);
			}
		}

		@Override
		protected boolean hasWhitelistedRecipients() throws SenderException {
			try {
				return smtpMessage.getAllRecipients() != null && smtpMessage.getAllRecipients().length > 0;
			} catch (MessagingException e) {
				throw new SenderException("Error occurred while getting mail recipients", e);
			}
		}
	}
}
