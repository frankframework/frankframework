package nl.nn.adapterframework.senders;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.soap.util.mime.ByteArrayDataSource;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDescription; 
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlUtils;

public abstract class MailSenderBase extends SenderWithParametersBase {

	private String authAlias;
	private String userId;
	private String password;
	private CredentialFactory cf;

	private String defaultAttachmentName = "attachment";
	private String defaultMessageType = "text/plain";
	private String defaultMessageBase64 = "false";
	private String defaultSubject;
	private String defaultFrom;
	private int timeout = 20000;

	protected abstract void sendEmail(MailSession mailSession) throws SenderException;

	@Override
	public void configure() throws ConfigurationException {
		cf = new CredentialFactory(getAuthAlias(), getUserId(), getPassword());
		super.configure();
	}

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		MailSession mailSession;
		try {
			mailSession = extract(message, prc);
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		}
		sendEmail(mailSession);

		return correlationID;
	}

	@Override
	public String sendMessage(String correlationID, String input) throws SenderException, TimeOutException {
		return sendMessage(correlationID, input, null);
	}

	/**
	 * Reads fields from either paramList or Xml file
	 * @param input
	 * @param prc
	 * @return MailSession 
	 * @throws SenderException
	 * @throws DomBuilderException
	 */
	public MailSession extract(String input, ParameterResolutionContext prc)
			throws SenderException, DomBuilderException {
		MailSession mailSession;
		if (paramList == null) {
			mailSession = parseXML(input, prc);
		} else {
			mailSession = readParameters(prc);
		}
		return mailSession;
	}

	private Collection<Attachment> retrieveAttachmentsFromParamList(ParameterValue pv, ParameterResolutionContext prc)
			throws SenderException, ParameterException {
		Collection<Attachment> attachments = null;
		if (pv != null) {
			attachments = retrieveAttachments(pv.asCollection(), prc);
			log.debug("MailSender [" + getName() + "] retrieved attachments-parameter [" + attachments + "]");
		}
		return attachments;
	}

	private Collection<EMail> retrieveRecipientsFromParameterList(ParameterValue pv)
			throws ParameterException, SenderException {
		Collection<EMail> recipients = null;
		if (pv != null) {
			recipients = retrieveRecipients(pv.asCollection());
			log.debug("MailSender [" + getName() + "] retrieved recipients-parameter [" + recipients + "]");
		}
		return recipients;
	}

	private MailSession readParameters(ParameterResolutionContext prc) throws SenderException {
		EMail from = null;
		String subject = null;
		String threadTopic = null;
		String messageType = null;
		String messageBase64 = null;
		String charset = null;
		List<EMail> recipients;
		ArrayList<Attachment> attachments = null;
		ParameterValueList pvl;
		ParameterValue pv;

		MailSession mail = new MailSession();
		try {
			pvl = prc.getValues(paramList);
			pv = pvl.getParameterValue("from");
			if (pv != null) {
				from = new EMail();
				from.setAddress(pv.asStringValue(null));
				log.debug("MailSender [" + getName() + "] retrieved from-parameter [" + from + "]");
				mail.setFrom(from);
			}
			pv = pvl.getParameterValue("subject");
			if (pv != null) {
				subject = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved subject-parameter [" + subject + "]");
				mail.setSubject(subject);
			}
			pv = pvl.getParameterValue("threadTopic");
			if (pv != null) {
				threadTopic = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved threadTopic-parameter [" + threadTopic + "]");
				mail.setThreadTopic(threadTopic);
			}
			pv = pvl.getParameterValue("message");
			if (pv != null) {
				String message = pv.asStringValue("message");
				log.debug("MailSender [" + getName() + "] retrieved message-parameter [" + message + "]");
				mail.setMessage(message);
			}
			pv = pvl.getParameterValue("messageType");
			if (pv != null) {
				messageType = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved messageType-parameter [" + messageType + "]");
				mail.setMessageType(messageType);
			}
			pv = pvl.getParameterValue("messageBase64");
			if (pv != null) {
				messageBase64 = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved messageBase64-parameter [" + messageBase64 + "]");
				mail.setMessageBase64(messageBase64);
			}
			pv = pvl.getParameterValue("charset");
			if (pv != null) {
				charset = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved charset-parameter [" + charset + "]");
				mail.setCharSet(charset);
			}
			pv = pvl.getParameterValue("recipients");
			Collection<EMail> recipientsCollection = retrieveRecipientsFromParameterList(pv);
			if (recipientsCollection != null && !recipientsCollection.isEmpty()) {
				recipients = new ArrayList<EMail>(recipientsCollection);
				mail.setRecipientList(recipients);
			} else {
				throw new SenderException("Recipients cannot be empty. At least one recipient is required");
			}

			pv = pvl.getParameterValue("attachments");
			Collection<Attachment> attachmentsCollection = retrieveAttachmentsFromParamList(pv, prc);
			if (attachmentsCollection != null && !attachmentsCollection.isEmpty()) {
				attachments = new ArrayList<Attachment>(attachmentsCollection);
				mail.setAttachmentList(attachments);
			}

		} catch (ParameterException e) {
			throw new SenderException("MailSender [" + getName() + "] got exception determining parametervalues", e);
		}
		return mail;
	}

	private List<EMail> retrieveRecipients(Collection<Node> recipientsNode) throws SenderException {
		List<EMail> recipients = null;
		if (recipientsNode != null && !recipientsNode.isEmpty()) {
			Iterator<Node> iter = recipientsNode.iterator();
			if (iter.hasNext()) {
				recipients = new LinkedList<EMail>();
				while (iter.hasNext()) {
					Element recipientElement = (Element) iter.next();
					String value = XmlUtils.getStringValue(recipientElement);
					if (StringUtils.isNotEmpty(value)) {
						String type = recipientElement.getAttribute("type");
						EMail email = new EMail();
						email.setAddress(value);
						email.setType(type);
						recipients.add(email);
					} else {
						log.debug("empty recipient found, ignoring");
					}
				}
			}
		} else {
			throw new SenderException("no recipients for message");
		}

		return recipients;
	}

	private Collection<Attachment> retrieveAttachments(Collection<Node> attachmentsNode, ParameterResolutionContext prc)
			throws SenderException {
		Collection<Attachment> attachments = null;
		Iterator<Node> iter = attachmentsNode.iterator();
		if (iter != null && iter.hasNext()) {
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
							attachmentDataSource = new ByteArrayDataSource((InputStream) object,
									"application/octet-stream");
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
						throw new SenderException("MailSender [" + getName() + "] received unknown attachment type ["
								+ object.getClass().getName() + "] in sessionkey");
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
				Attachment attachment = new Attachment();
				attachment.setAttachmentText(value);
				attachment.setAttachmentName(name);
				attachment.setSessionKey(sessionKey);
				attachment.setAttachmentURL(url);
				attachments.add(attachment);
			}
		}
		return attachments;
	}

	private MailSession parseXML(String input, ParameterResolutionContext prc)
			throws SenderException, DomBuilderException {
		Element from;
		String subject;
		String threadTopic;
		String message;
		String messageType;
		String messageBase64;
		String charset;
		Collection<Node> recipientList;
		Collection<Node> attachments;
		Element replyTo = null;

		MailSession mailSession = new MailSession();

		Element emailElement = XmlUtils.buildElement(input);
		from = XmlUtils.getFirstChildTag(emailElement, "from");
		subject = XmlUtils.getChildTagAsString(emailElement, "subject");
		threadTopic = XmlUtils.getChildTagAsString(emailElement, "threadTopic");
		message = XmlUtils.getChildTagAsString(emailElement, "message");
		messageType = XmlUtils.getChildTagAsString(emailElement, "messageType");
		messageBase64 = XmlUtils.getChildTagAsString(emailElement, "messageBase64");
		charset = XmlUtils.getChildTagAsString(emailElement, "charset");

		Element recipientsElement = XmlUtils.getFirstChildTag(emailElement, "recipients");
		recipientList = XmlUtils.getChildTags(recipientsElement, "recipient");

		Element attachmentsElement = XmlUtils.getFirstChildTag(emailElement, "attachments");
		attachments = attachmentsElement == null ? null : XmlUtils.getChildTags(attachmentsElement, "attachment");
		replyTo = XmlUtils.getFirstChildTag(emailElement, "replyTo");
		Element headersElement = XmlUtils.getFirstChildTag(emailElement, "headers");
		Collection<Node> headers = headersElement == null ? null : XmlUtils.getChildTags(headersElement, "header");

		EMail emailFrom = getFrom(from);
		mailSession.setFrom(emailFrom);
		mailSession.setSubject(subject);
		mailSession.setThreadTopic(threadTopic);
		mailSession.setMessage(message);
		mailSession.setMessageType(messageType);
		mailSession.setMessageBase64(messageBase64);
		mailSession.setCharSet(charset);
		mailSession.setHeaders(headers);
		EMail replyto = getReplyTo(replyTo);
		mailSession.setReplyto(replyto);

		List<EMail> recipients = retrieveRecipients(recipientList);
		mailSession.setRecipientList(recipients);
		if (attachments != null) {
			List<Attachment> attachmentList = (List<Attachment>) retrieveAttachments(attachments, prc);
			mailSession.setAttachmentList(attachmentList);
		}

		return mailSession;
	}

	private EMail getFrom(Element fromElement) {
		String value = XmlUtils.getStringValue(fromElement);
		if (StringUtils.isNotEmpty(value)) {
			EMail from = new EMail();
			from.setAddress(value);
			from.setName(fromElement.getAttribute("name"));
			from.setType("from");
			return from;
		}
		return null;
	}

	private EMail getReplyTo(Element replyToElement) {
		if (replyToElement != null) {
			String value = XmlUtils.getStringValue(replyToElement);
			if (StringUtils.isNotEmpty(value)) {
				EMail reply = new EMail();
				reply.setAddress(value);
				reply.setName(replyToElement.getAttribute("name"));
				reply.setType("replyTo");
				return reply;
			}
		}
		return null;
	}

	protected DataHandler decodeBase64(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		String encodingType = "application/octet-stream";
		DataSource ads = new ByteArrayDataSource(bytesDecoded, encodingType);
		return new DataHandler(ads);
	}

	protected String decodeBase64ToString(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		return new String(bytesDecoded);
	}

	protected byte[] decodeBase64ToBytes(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		return bytesDecoded;
	}

	protected String encodeFileToBase64Binary(InputStream inputStream) throws IOException {
		String encodedfile = null;
		byte[] bytes = StreamUtil.streamToByteArray(inputStream, false);
		encodedfile = new String(Base64.encode(bytes));

		return encodedfile;
	}

	public boolean isSynchronous() {
		return false;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	@IbisDoc({ "authAlias used to obtain credentials for authentication", "" })
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getUserId() {
		return userId;
	}

	@IbisDoc({ "userId on the smtphost", "" })
	public void setUserId(String userId) {
		this.userId = userId;
	}

	public String getPassword() {
		return password;
	}

	@IbisDoc({ "password of userid", "" })
	public void setPassword(String password) {
		this.password = password;
	}

	@IbisDoc({ "alias used to obtain credentials for authentication to smtphost", "" })
	@Deprecated
	public void setSmtpAuthAlias(String smtpAuthAlias) {
		setAuthAlias(smtpAuthAlias);
	}

	@IbisDoc({ "userId on the smtphost", "" })
	@Deprecated
	public void setSmtpUserid(String smtpUserId) {
		setUserId(smtpUserId);
	}

	@IbisDoc({ "password of userid on the smtphost", "" })
	@Deprecated
	public void setSmtpPassword(String smtpPassword) {
		setPassword(smtpPassword);
	}

	public CredentialFactory getCredentialFactory() {
		return cf;
	}

	public void setCredentialFactory(CredentialFactory cf) {
		this.cf = cf;
	}

	public String getDefaultSubject() {
		return defaultSubject;
	}

	/**
	 * Set the default for Subject>
	 */
	@IbisDoc({ "value of the subject: header if not specified in message itself", "" })
	public void setDefaultSubject(String defaultSubject) {
		this.defaultSubject = defaultSubject;
	}

	public String getDefaultFrom() {
		return defaultFrom;
	}

	/**
	 * Set the default for From
	 */
	@IbisDoc({ "value of the from: header if not specified in message itself", "" })
	public void setDefaultFrom(String defaultFrom) {
		this.defaultFrom = defaultFrom;
	}

	public int getTimeout() {
		return timeout;
	}

	@IbisDoc({ "timeout (in milliseconds). used for socket connection timeout and socket i/o timeout", "20000" })
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	public String getDefaultAttachmentName() {
		return defaultAttachmentName;
	}

	@IbisDoc({ "when this name is used, it will be followed by a number which is equal to the node's position",
			"attachment" })
	public void setDefaultAttachmentName(String defaultAttachmentName) {
		this.defaultAttachmentName = defaultAttachmentName;
	}

	public String getDefaultMessageType() {
		return defaultMessageType;
	}

	@IbisDoc({ "when messageType is not specified defaultMessageType will be used", "text/plain" })
	public void setDefaultMessageType(String defaultMessageType) {
		this.defaultMessageType = defaultMessageType;
	}

	public String getDefaultMessageBase64() {
		return defaultMessageBase64;
	}

	@IbisDoc({ "when messageBase64 is not specified defaultMessageBase64 will be used", "false" })
	public void setDefaultMessageBase64(String defaultMessageBase64) {
		this.defaultMessageBase64 = defaultMessageBase64;
	}

	/**
	 * Generic email class
	 * @author alisihab
	 *
	 */
	protected class MailSession {
		private EMail from = null;
		private EMail replyto = null;
		private List<EMail> recipients = new ArrayList<EMail>();
		private List<Attachment> attachmentList = new ArrayList<Attachment>();
		private String subject = null;
		private String message = null;
		private String messageType = null;
		private String messageBase64 = null;
		private String charSet = null;
		private String threadTopic = null;
		private Collection<Node> headers;

		public EMail getFrom() {
			return from;
		}

		public void setFrom(EMail from) {
			this.from = from;
		}

		public EMail getReplyto() {
			return replyto;
		}

		public void setReplyto(EMail replyto) {
			this.replyto = replyto;
		}

		public List<EMail> getRecipientList() {
			return recipients;
		}

		public void setRecipientList(List<EMail> recipients) {
			this.recipients = recipients;
		}

		public List<Attachment> getAttachmentList() {
			return attachmentList;
		}

		public void setAttachmentList(List<Attachment> attachmentList) {
			this.attachmentList = attachmentList;
		}

		public String getSubject() {
			return subject;
		}

		public void setSubject(String subject) {
			this.subject = subject;
		}

		public String getMessage() {
			return message;
		}

		public void setMessage(String message) {
			this.message = message;
		}

		public String getMessageType() {
			return messageType;
		}

		public void setMessageType(String messageType) {
			this.messageType = messageType;
		}

		public String getMessageBase64() {
			return messageBase64;
		}

		public void setMessageBase64(String messageBase64) {
			this.messageBase64 = messageBase64;
		}

		public String getCharSet() {
			return charSet;
		}

		public void setCharSet(String charSet) {
			this.charSet = charSet;
		}

		public String getThreadTopic() {
			return threadTopic;
		}

		public void setThreadTopic(String threadTopic) {
			this.threadTopic = threadTopic;
		}

		public Collection<Node> getHeaders() {
			return headers;
		}

		public void setHeaders(Collection<Node> headers) {
			this.headers = headers;
		}
	}

	/**
	 * Generic attachment class
	 * @author alisihab
	 *
	 */
	protected class Attachment {
		private String attachmentName;
		private String attachmentType;
		private String attachmentURL;
		private Object attachmentText;
		private String attachmentBase64;
		private String sessionKey;

		public String getAttachmentName() {
			return attachmentName;
		}

		public void setAttachmentName(String attachmentName) {
			this.attachmentName = attachmentName;
		}

		public String getAttachmentType() {
			return attachmentType;
		}

		public void setAttachmentType(String attachmentType) {
			this.attachmentType = attachmentType;
		}

		public String getAttachmentURL() {
			return attachmentURL;
		}

		public void setAttachmentURL(String attachmentURL) {
			this.attachmentURL = attachmentURL;
		}

		public Object getAttachmentText() {
			return attachmentText;
		}

		public void setAttachmentText(Object attachmentText) {
			this.attachmentText = attachmentText;
		}

		public String getAttachmentBase64() {
			return attachmentBase64;
		}

		public void setAttachmentBase64(String attachmentBase64) {
			this.attachmentBase64 = attachmentBase64;
		}

		public String getSessionKey() {
			return sessionKey;
		}

		public void setSessionKey(String sessionKey) {
			this.sessionKey = sessionKey;
		}
	}

	/**
	 * Generic mail class 
	 * @author alisihab
	 *
	 */
	protected class EMail {
		private String address;
		private String name;
		private String type; //"cc", "to", "from", "bcc" 

		public String getAddress() {
			return address;
		}

		public void setAddress(String address) {
			this.address = address;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getType() {
			return type;
		}

		public void setType(String type) {
			this.type = type;
		}
	}

}
