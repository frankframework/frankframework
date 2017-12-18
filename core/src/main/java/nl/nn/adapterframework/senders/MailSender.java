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
package nl.nn.adapterframework.senders;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
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

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.soap.util.mime.ByteArrayDataSource;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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
 *       &lt;from&gt;***@yahoo.com&lt;/from&gt;
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
 * <tr><td>{@link #setSmtpAuthAlias(String) smtpAuthAlias}</td><td>alias used to obtain credentials for authentication to smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpUserid(String) smtpUserid}</td><td>userid on the smtpHost</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSmtpPassword(String) smtpPassword}</td><td>password of userid on the smtpHost</td><td>&nbsp;</td></tr>
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
 * <tr><td>charset</td><td>string</td><td>the character encoding (e.g. ISO-8859-1 or UTF-8) used to send the email (default: value of system property mail.mime.charset, when not present the value of system property file.encoding)</td></tr>
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
 * @author Johan Verrips/Gerrit van Brakel
 */

public class MailSender extends SenderWithParametersBase {

	private String smtpHost;
	private String smtpAuthAlias;
	private String smtpUserid;
	private String smtpPassword;
	private String defaultAttachmentName = "attachment";
	private String defaultMessageType = "text/plain";
	private String defaultMessageBase64 = "false";
	
	private int timeout=20000;

	// defaults
	private String defaultSubject;
	private String defaultFrom;

	private Session session;
	private Properties properties;

	private class Recipient {
		String value;
		String type;

		Recipient(String value, String type) {
			this.value = value;
			this.type = type;
		}
		
		public String toString() {
			return "type ["+type+"] value ["+value+"]";
		}
		
		public String getValue() {
			return value;
		}

		public String getType() {
			return type;
		}
	}

	private class Attachment {
		Object value;
		String name;

		Attachment(Object value, String name) {
			this.value = value;
			this.name = name;
		}

		public String toString() {
			return "name ["+name+"] value ["+value+"]";
		}

		public Object getValue() {
			return value;
		}

		public String getName() {
			return name;
		}
}
	
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getSmtpHost())) {
			throw new ConfigurationException("MailSender ["+getName()+"] has no smtpHost configured");
		}
 		properties = System.getProperties();
		try { 		
			properties.put("mail.smtp.host", getSmtpHost());
		} catch (Throwable t) {
			throw new ConfigurationException("MailSender ["+getName()+"] cannot set smtpHost ["+getSmtpHost()+"] in properties");
		}
		properties.put("mail.smtp.connectiontimeout", getTimeout()+"");
		properties.put("mail.smtp.timeout", getTimeout()+"");
		if (paramList!=null) {
			paramList.configure();
		}
	}

	/**
	 * Create a <code>Session</code> and <code>Transport</code> to the
	 * smtp host.
	  * @throws SenderException
	 */
	public void open() throws SenderException {
		try {
			getSession();

		} catch (Exception e) {
			throw new SenderException("Error opening MailSender", e);
		}
	}

	/**
	 * Close the <code>transport</code> layer.
	 */
	public void close() throws SenderException {
		/*
		try {
			if (transport!=null) {
				transport.close();
			}
		} catch (Exception e) {
			throw new SenderException("error closing transport", e);
		}
		*/
	}

	public boolean isSynchronous() {
		return false;
	}


	protected Session getSession() {
		if (session == null) {
			session = Session.getInstance(properties, null);
			session.setDebug(log.isDebugEnabled());
//			log.debug("MailSender [" + getName() + "] got session to [" + properties + "]");
		}
		return session;
	}



	public String sendMessage(String correlationID,	String message,	ParameterResolutionContext prc) throws SenderException, TimeOutException {
		String from=null;
		String subject=null;
		String threadTopic=null;
		String messageType=null;
		String messageBase64=null;
		String charset=null;
		Collection<Recipient> recipients=null;
		Collection<Attachment> attachments=null;
		ParameterValueList pvl;
		ParameterValue pv;
		
		String messageInMailSafeForm;
		if (paramList==null) {
			messageInMailSafeForm = sendEmail(message, prc);
		} else {
			try {
				pvl = prc.getValues(paramList);
				pv = pvl.getParameterValue("from");
				if (pv != null) {
					from = pv.asStringValue(null);  
					log.debug("MailSender ["+getName()+"] retrieved from-parameter ["+from+"]");
				}
				pv = pvl.getParameterValue("subject");
				if (pv != null) {
					subject = pv.asStringValue(null);  
					log.debug("MailSender ["+getName()+"] retrieved subject-parameter ["+subject+"]");
				}
				pv = pvl.getParameterValue("threadTopic");
				if (pv != null) {
					threadTopic = pv.asStringValue(null);  
					log.debug("MailSender ["+getName()+"] retrieved threadTopic-parameter ["+threadTopic+"]");
				}
				pv = pvl.getParameterValue("message");
				if (pv != null) {
					message = pv.asStringValue(message);  
					log.debug("MailSender ["+getName()+"] retrieved message-parameter ["+message+"]");
				}
				pv = pvl.getParameterValue("messageType");
				if (pv != null) {
					messageType = pv.asStringValue(null);  
					log.debug("MailSender ["+getName()+"] retrieved messageType-parameter ["+messageType+"]");
				}
				pv = pvl.getParameterValue("messageBase64");
				if (pv != null) {
					messageBase64 = pv.asStringValue(null);  
					log.debug("MailSender ["+getName()+"] retrieved messageBase64-parameter ["+messageBase64+"]");
				}
				pv = pvl.getParameterValue("charset");
				if (pv != null) {
					charset = pv.asStringValue(null);  
					log.debug("MailSender ["+getName()+"] retrieved charset-parameter ["+charset+"]");
				}
				pv = pvl.getParameterValue("recipients");
				if (pv != null) {
					recipients = retrieveRecipients(pv.asCollection());
					log.debug("MailSender ["+getName()+"] retrieved recipients-parameter ["+recipients+"]");
				}
				pv = pvl.getParameterValue("attachments");
				if (pv != null) {
					attachments = retrieveAttachments(pv.asCollection(), prc);
					log.debug("MailSender ["+getName()+"] retrieved attachments-parameter ["+attachments+"]");
				}
			} catch (ParameterException e) {
				throw new SenderException("MailSender ["+getName()+"] got exception determining parametervalues",e);
			}
			messageInMailSafeForm = sendEmail(from, subject, threadTopic, message, messageType, messageBase64, charset, recipients, attachments);
		}
		prc.getSession().put("messageInMailSafeForm", messageInMailSafeForm);
		return correlationID;
	}
	
	public String sendMessage(String correlationID, String input) throws SenderException {
		sendEmail(input, null);
		return correlationID;
	}
	
	private Collection<Recipient> retrieveRecipients(Collection<Node> recipientsNode) {
		Collection<Recipient> recipients=null;
		Iterator iter = recipientsNode.iterator();
		if (iter.hasNext()) {
			recipients = new LinkedList<Recipient>();
			while (iter.hasNext()) {
				Element recipientElement = (Element) iter.next();
				String value = XmlUtils.getStringValue(recipientElement);
				if (StringUtils.isNotEmpty(value)) {
					String type = recipientElement.getAttribute("type");
					Recipient recipient = new Recipient(value, type);
					recipients.add(recipient);
				} else {
					log.debug("empty recipient found, ignoring");
				}
			}
		}
		return recipients;
	}

	private Collection<Attachment> retrieveAttachments(Collection<Node> attachmentsNode, ParameterResolutionContext prc) throws SenderException {
		Collection<Attachment> attachments=null;
		Iterator iter = attachmentsNode.iterator();
		if (iter.hasNext()) {
			attachments = new LinkedList<Attachment>();
			while (iter.hasNext()) {
				Element attachmentElement = (Element) iter.next();
				String name = attachmentElement.getAttribute("name");
				String sessionKey = attachmentElement.getAttribute("sessionKey");
				String url = attachmentElement.getAttribute("url");
				boolean base64 = Boolean.parseBoolean(attachmentElement.getAttribute("base64"));
				Object value = null;
				if (StringUtils.isNotEmpty(sessionKey)) {
					Object object = prc.getSession().get(sessionKey);
					if (object instanceof InputStream) {
						DataSource attachmentDataSource;
						try {
							attachmentDataSource = new ByteArrayDataSource((InputStream)object, "application/octet-stream");
						} catch (IOException e) {
							throw new SenderException("error retrieving attachment from sessionkey", e);
						}
						value = new DataHandler(attachmentDataSource);
					} else if (object instanceof String) {
						String skValue = (String) object;
						if (base64) {
							value = decodeBase64(skValue);
						} else {
							value = skValue;
						}
					} else {
						throw new SenderException("MailSender ["+getName()+"] received unknown attachment type ["+object.getClass().getName()+"] in sessionkey");
					}
				} else {
					if (StringUtils.isNotEmpty(url)) {
						DataSource attachmentDataSource;
						try {
							attachmentDataSource = new URLDataSource(new URL(url));
						} catch (MalformedURLException e) {
							throw new SenderException("error retrieving attachment from url", e);
						}
						value = new DataHandler(attachmentDataSource);
					} else {
						String nodeValue = XmlUtils.getStringValue(attachmentElement);
						if (base64) {
							value = decodeBase64(nodeValue);
						} else {
							value = nodeValue;
						}
					}
				}
				Attachment attachment = new Attachment(value, name);
				attachments.add(attachment);
			}
		}
		return attachments;
	}

	/**
	 * Send a mail conforming to the XML input
	 */
	protected String sendEmail(String input, ParameterResolutionContext prc) throws SenderException {
		// initialize this request
		String from;
		String subject;
		String threadTopic;
		String message;
		String messageType;
		String messageBase64;
		String charset;
		Collection<Recipient> recipients;
		Collection<Attachment> attachments = null;
		
		Element emailElement;
		try {
			emailElement = XmlUtils.buildElement(input);

			from = XmlUtils.getChildTagAsString(emailElement, "from");
			subject = XmlUtils.getChildTagAsString(emailElement, "subject");
			threadTopic = XmlUtils.getChildTagAsString(emailElement, "threadTopic");
			message = XmlUtils.getChildTagAsString(emailElement, "message");
			messageType = XmlUtils.getChildTagAsString(emailElement, "messageType");
			messageBase64 = XmlUtils.getChildTagAsString(emailElement, "messageBase64");
			charset = XmlUtils.getChildTagAsString(emailElement, "charset");

			Element recipientsElement = XmlUtils.getFirstChildTag(emailElement, "recipients");
			recipients = retrieveRecipients(XmlUtils.getChildTags(recipientsElement, "recipient"));

			Element attachmentsElement = XmlUtils.getFirstChildTag(emailElement, "attachments");
			if(attachmentsElement != null)
				attachments = retrieveAttachments(XmlUtils.getChildTags(attachmentsElement, "attachment"), prc);

		} catch (DomBuilderException e) {
			throw new SenderException("exception parsing [" + input + "]", e);
		}

		return sendEmail(from, subject, threadTopic, message, messageType, messageBase64, charset, recipients, attachments);
	}

	protected String sendEmail(String from, String subject, String threadTopic, String message,
			String messageType, String messageBase64, String charset,
			Collection<Recipient> recipients, Collection<Attachment> attachments) throws SenderException {

		StringBuffer sb = new StringBuffer();

		if (recipients==null || recipients.size()==0) {
			throw new SenderException("MailSender ["+getName()+"] has no recipients for message");
		}
		if (StringUtils.isEmpty(from)) {
			from = defaultFrom;
		}
		if (StringUtils.isEmpty(subject)) {
			subject = defaultSubject;
		}
		log.debug("MailSender ["+getName()+"] requested to send message from ["+from+"] subject ["+subject+"] to #recipients ["+recipients.size()+"]");

		if (StringUtils.isEmpty(messageType)) {
			messageType = defaultMessageType;
		}

		if (StringUtils.isEmpty(messageBase64)) {
			messageBase64 = defaultMessageBase64;
		}
		
		try {
			if (log.isDebugEnabled()) {
				sb.append("MailSender [" + getName() + "] sending message ");
				sb.append("[smtpHost=" + smtpHost);
				sb.append("[from=" + from + "]");
				sb.append("[subject=" + subject + "]");
				sb.append("[threadTopic=" + threadTopic + "]");
				sb.append("[text=" + message + "]");
				sb.append("[type=" + messageType + "]");
				sb.append("[base64=" + messageBase64 + "]");
			}

			if ("true".equalsIgnoreCase(messageBase64) && StringUtils.isNotEmpty(message)) {
				message=decodeBase64ToString(message);
			}

			// construct a message  
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(from));
			msg.setSubject(subject, charset);
			if (StringUtils.isNotEmpty(threadTopic)) {
				msg.setHeader("Thread-Topic", threadTopic);
			}
			Iterator iter = recipients.iterator();
			boolean recipientsFound=false;
			while (iter.hasNext()) {
				Recipient recipient = (Recipient) iter.next();
				String value = recipient.value;
				String type = recipient.type;
				Message.RecipientType recipientType;
				if ("cc".equalsIgnoreCase(type)) {
					recipientType = Message.RecipientType.CC;
				} else if ("bcc".equalsIgnoreCase(type)) {
					recipientType = Message.RecipientType.BCC;
				} else {
					recipientType = Message.RecipientType.TO;
				}
				msg.addRecipient(recipientType, new InternetAddress(value));
				recipientsFound = true;
				if (log.isDebugEnabled()) {
					sb.append("[recipient [" + recipient + "]]");
				}
			}
			if (!recipientsFound) {
				throw new SenderException("MailSender [" + getName() + "] did not find any valid recipients");
			}

			String messageTypeWithCharset;
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
			log.debug("MailSender [" + getName() + "] uses encoding ["+messageTypeWithCharset+"]");

			if (attachments==null || attachments.size()==0) {
				//msg.setContent(message, messageType);
				msg.setContent(message, messageTypeWithCharset);
			} else {
				Multipart multipart = new MimeMultipart();
				BodyPart messageBodyPart = new MimeBodyPart();
				//messageBodyPart.setContent(message, messageType);
				messageBodyPart.setContent(message, messageTypeWithCharset);
				multipart.addBodyPart(messageBodyPart);
				
				int counter = 0;
				iter = attachments.iterator();
				while (iter.hasNext()) {
					counter++;
					Attachment attachment = (Attachment) iter.next();
					Object value = attachment.getValue();
					String name = attachment.getName();
					if (StringUtils.isEmpty(name)) {
						name = getDefaultAttachmentName() + counter;
					}
					log.debug("found attachment ["+attachment+"]");
					
					messageBodyPart = new MimeBodyPart();
					messageBodyPart.setFileName(name);

					if (value instanceof DataHandler) {
						messageBodyPart.setDataHandler((DataHandler) value);
					} else {
						messageBodyPart.setText((String) value);
					}

					multipart.addBodyPart(messageBodyPart);
				}
				msg.setContent(multipart);	
			}
			

			log.debug(sb.toString());
			msg.setSentDate(new Date());
			msg.saveChanges();
			// send the message
			putOnTransport(msg);
			// return the mail in mail-safe from
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			msg.writeTo(out);
			byte[] byteArray = out.toByteArray();
			return Misc.byteArrayToString(byteArray,"\n",false); 
		} catch (Exception e) {
			throw new SenderException("MailSender got error", e);
		}
	}

	private DataHandler decodeBase64 (String str) {
			byte[] bytesDecoded = Base64.decode(str);
			String encodingType = "application/octet-stream";
			DataSource ads = new ByteArrayDataSource(bytesDecoded, encodingType);
			return new DataHandler(ads);
	}

	private String decodeBase64ToString (String str) {
			byte[] bytesDecoded = Base64.decode(str);
			return new String(bytesDecoded);
	}

	protected void putOnTransport(Message msg) throws SenderException {
		// connect to the transport 
		Transport transport=null;
		try {
			CredentialFactory cf = new CredentialFactory(getSmtpAuthAlias(), getSmtpUserid(), getSmtpPassword());
			transport = session.getTransport("smtp");
			transport.connect(getSmtpHost(), cf.getUsername(), cf.getPassword());
			if (log.isDebugEnabled()) {
				log.debug("MailSender [" + getName() + "] connected transport to URL ["+transport.getURLName()+"]");
			}
			transport.sendMessage(msg, msg.getAllRecipients());
			transport.close();
		} catch (Exception e) {
			throw new SenderException("MailSender [" + getName() + "] cannot connect send message to smtpHost ["+getSmtpHost()+"]",e);
		} finally {
			if (transport!=null) {
				try {
					transport.close();
				} catch (MessagingException e1) {
					log.warn("MailSender [" + getName() + "] got exception closing connection", e1);
				}
			}
		}
	}


	/**
	 * Set the default for From
	 */
	public void setDefaultFrom(String newFrom) {
		defaultFrom = newFrom;
	}
	/**
	 * Set the default for Subject>
	 */
	public void setDefaultSubject(String newSubject) {
		defaultSubject = newSubject;
	}
	
	/**
	 * Name of the SMTP Host.
	 */
	public void setSmtpHost(String newSmtpHost) {
		smtpHost = newSmtpHost;
	}
	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpAuthAlias(String string) {
		smtpAuthAlias = string;
	}
	public String getSmtpAuthAlias() {
		return smtpAuthAlias;
	}

	public void setSmtpUserid(java.lang.String newSmtpUserid) {
		smtpUserid = newSmtpUserid;
	}
	public String getSmtpUserid() {
		return smtpUserid;
	}
	
	public void setSmtpPassword(String newSmtpPassword) {
		smtpPassword = newSmtpPassword;
	}
	public String getSmtpPassword() {
		return smtpPassword;
	}

	public void setDefaultAttachmentName(String string) {
		defaultAttachmentName = string;
	}
	public String getDefaultAttachmentName() {
		return defaultAttachmentName;
	}

	public int getTimeout() {
		return timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
