/*
   Copyright 2019 Integration Partners

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
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
	private boolean defaultMessageBase64 = false;
	private String defaultSubject;
	private String defaultFrom;
	private int timeout = 20000;
	private String bounceAddress;

	protected abstract void sendEmail(MailSession mailSession) throws SenderException;

	@Override
	public void configure() throws ConfigurationException {
		cf = new CredentialFactory(getAuthAlias(), getUserId(), getPassword());
		super.configure();
	}

	@Override
	public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
		MailSession mailSession;
		try {
			mailSession = extract(message, session);
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		}
		sendEmail(mailSession);

		String correlationID = session==null ? null : session.getMessageId();
		return new Message(correlationID);
	}

	/**
	 * Reads fields from either paramList or Xml file
	 */
	public MailSession extract(Message input, IPipeLineSession session) throws SenderException, DomBuilderException {
		MailSession mailSession;
		if (paramList == null) {
			mailSession = parseXML(input, session);
		} else {
			mailSession = readParameters(input, session);
		}
		return mailSession;
	}

	private Collection<MailAttachmentStream> retrieveAttachmentsFromParamList(ParameterValue pv, IPipeLineSession session) throws SenderException, ParameterException {
		Collection<MailAttachmentStream> attachments = null;
		if (pv != null) {
			attachments = retrieveAttachments(pv.asCollection(), session);
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

	private MailSession readParameters(Message input, IPipeLineSession session) throws SenderException {
		EMail from = null;
		String subject = null;
		String threadTopic = null;
		String messageType = null;
		boolean messageBase64 = false;
		String charset = null;
		List<EMail> recipients;
		List<MailAttachmentStream> attachments = null;
		ParameterValueList pvl=null;
		ParameterValue pv;

		MailSession mail = new MailSession();
		try {
			pvl = paramList.getValues(input, session);
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
				messageBase64 = pv.asBooleanValue(false);
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
			Collection<MailAttachmentStream> attachmentsCollection = retrieveAttachmentsFromParamList(pv, session);
			if (attachmentsCollection != null && !attachmentsCollection.isEmpty()) {
				attachments = new ArrayList<MailAttachmentStream>(attachmentsCollection);
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
						String name = recipientElement.getAttribute("name");
						String type = recipientElement.getAttribute("type");
						EMail recipient = new EMail();
						recipient.setAddress(value);
						recipient.setType(type);
						recipient.setName(name);
						recipients.add(recipient);
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

	private Collection<MailAttachmentStream> retrieveAttachments(Collection<Node> attachmentsNode, IPipeLineSession session) throws SenderException {
		Collection<MailAttachmentStream> attachments = null;
		Iterator<Node> iter = attachmentsNode.iterator();
		if (iter != null && iter.hasNext()) {
			attachments = new LinkedList<MailAttachmentStream>();
			while (iter.hasNext()) {
				Element attachmentElement = (Element) iter.next();
				String name = attachmentElement.getAttribute("name");
				String mimeType = attachmentElement.getAttribute("type");
				if (StringUtils.isNotEmpty(mimeType) && mimeType.indexOf("/")<0) {
					throw new SenderException("mimeType ["+mimeType+"] of attachment ["+name+"] must contain a forward slash ('/')");
				}
				String sessionKey = attachmentElement.getAttribute("sessionKey");
				boolean base64 = Boolean.parseBoolean(attachmentElement.getAttribute("base64"));

				MailAttachmentStream attachment = null;
				if (StringUtils.isNotEmpty(sessionKey)) {
					Object object = session.get(sessionKey);
					if (object instanceof InputStream) {
						attachment = streamToMailAttachment((InputStream) object, base64, mimeType);
					} else if (object instanceof String) {
						attachment = stringToMailAttachment((String) object, base64, mimeType);
					} else {
						throw new SenderException("MailSender ["+getName()+"] received unknown attachment type ["+object.getClass().getName()+"] in sessionkey");
					}
				} else {
					String nodeValue = XmlUtils.getStringValue(attachmentElement);
					attachment = stringToMailAttachment(nodeValue, base64, mimeType);
				}
				attachment.setName(name);
				log.debug("created attachment ["+attachment+"]");
				attachments.add(attachment);
			}
		}
		return attachments;
	}

	private MailAttachmentStream stringToMailAttachment(String value, boolean isBase64, String mimeType) {
		ByteArrayInputStream stream = new ByteArrayInputStream(value.getBytes());
		if (!isBase64 && StringUtils.isEmpty(mimeType)) {
			mimeType = "text/plain";
		}
		return streamToMailAttachment(stream, isBase64, mimeType);
	}

	private MailAttachmentStream streamToMailAttachment(InputStream stream, boolean isBase64, String mimeType) {
		MailAttachmentStream attachment = new MailAttachmentStream();
		if(StringUtils.isEmpty(mimeType)) {
			mimeType = "application/octet-stream";
		}
		if (isBase64) {
			attachment.setContent(new Base64InputStream(stream));
		} else {
			attachment.setContent(stream);
		}
		attachment.setMimeType(mimeType);

		return attachment;
	}

	private MailSession parseXML(Message input, IPipeLineSession session) throws SenderException, DomBuilderException {
		Element from;
		String subject;
		String threadTopic;
		String message;
		String messageType;
		boolean messageBase64;
		String charset;
		Collection<Node> recipientList;
		Collection<Node> attachments;
		Element replyTo = null;

		MailSession mailSession = new MailSession();

		Element emailElement = XmlUtils.buildElement(input);
		from = XmlUtils.getFirstChildTag(emailElement, "from");
		subject = XmlUtils.getChildTagAsString(emailElement, "subject");
		if (StringUtils.isEmpty(subject)) {
			subject=getDefaultSubject();
		}
		threadTopic = XmlUtils.getChildTagAsString(emailElement, "threadTopic");
		message = XmlUtils.getChildTagAsString(emailElement, "message");
		messageType = XmlUtils.getChildTagAsString(emailElement, "messageType");
		if (StringUtils.isEmpty(messageType)) {
			messageType=getDefaultMessageType();
		}
		if (messageType.indexOf("/")<0) {
			throw new SenderException("messageType ["+messageType+"] must contain a forward slash ('/')");
		}
		messageBase64 = XmlUtils.getChildTagAsBoolean(emailElement, "messageBase64");
		charset = XmlUtils.getChildTagAsString(emailElement, "charset");

		Element recipientsElement = XmlUtils.getFirstChildTag(emailElement, "recipients");
		if(recipientsElement == null) {
			throw new SenderException("at least 1 recipient must be specified");
		}
		recipientList = XmlUtils.getChildTags(recipientsElement, "recipient");

		Element attachmentsElement = XmlUtils.getFirstChildTag(emailElement, "attachments");
		attachments = attachmentsElement == null ? null : XmlUtils.getChildTags(attachmentsElement, "attachment");
		replyTo = XmlUtils.getFirstChildTag(emailElement, "replyTo");
		Element headersElement = XmlUtils.getFirstChildTag(emailElement, "headers");
		Collection<Node> headers = headersElement == null ? null : XmlUtils.getChildTags(headersElement, "header");

		String bounceAddress = XmlUtils.getChildTagAsString(emailElement, "bounceAddress");
		mailSession.setBounceAddress(bounceAddress);

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
			List<MailAttachmentStream> attachmentList = (List<MailAttachmentStream>) retrieveAttachments(attachments, session);
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

	@Override
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

	@IbisDoc({ "when messageBase64 is not specified defaultMessageBase64 will be used", "false" })
	public void setDefaultMessageBase64(boolean defaultMessageBase64) {
		this.defaultMessageBase64 = defaultMessageBase64;
	}
	public boolean isDefaultMessageBase64() {
		return defaultMessageBase64;
	}

	@IbisDoc({ "NDR return address when mail cannot be delivered. This adds a Return-Path header", "MAIL FROM attribute" })
	public void setBounceAddress(String string) {
		bounceAddress = string;
	}
	public String getBounceAddress() {
		return bounceAddress;
	}

	/**
	 * Generic email class
	 */
	protected class MailSession {
		private EMail from = null;
		private EMail replyto = null;
		private List<EMail> recipients = new ArrayList<EMail>();
		private List<MailAttachmentStream> attachmentList = new ArrayList<MailAttachmentStream>();
		private String subject = getDefaultSubject();
		private String message = null;
		private String messageType = getDefaultMessageType();
		private boolean messageIsBase64 = isDefaultMessageBase64();
		private String charSet = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		private String threadTopic = null;
		private Collection<Node> headers;
		private String bounceAddress = getBounceAddress();

		public MailSession() {
			from = new EMail();
			from.setAddress(getDefaultFrom());
		}

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

		public List<EMail> getRecipientList() throws SenderException {
			if (recipients == null || recipients.size() == 0) {
				throw new SenderException("MailSender [" + getName() + "] has no recipients for message");
			}
			return recipients;
		}

		public void setRecipientList(List<EMail> recipients) {
			this.recipients = recipients;
		}

		public List<MailAttachmentStream> getAttachmentList() {
			return attachmentList;
		}

		public void setAttachmentList(List<MailAttachmentStream> attachmentList) {
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

		public boolean isMessageBase64() {
			return messageIsBase64;
		}

		public void setMessageBase64(boolean messageIsBase64) {
			this.messageIsBase64 = messageIsBase64;
		}

		public String getCharSet() {
			return charSet;
		}

		public void setCharSet(String charSet) {
			if(StringUtils.isNotEmpty(charSet)) {
				this.charSet = charSet;
			}
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

		public void setBounceAddress(String bounceAddress) {
			this.bounceAddress = bounceAddress;
		}

		public String getBounceAddress() {
			return this.bounceAddress;
		}
	}

	/**
	 * Generic mail attachment class
	 * @author Niels Meijer
	 *
	 */
	protected abstract class MailAttachmentBase<T> {
		private String name;
		private String mimeType;
		private T value;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getMimeType() {
			return mimeType;
		}

		public void setMimeType(String mimeType) {
			this.mimeType = mimeType;
		}

		public T getContent() {
			return value;
		}

		public void setContent(T value) {
			this.value = value;
		}

		@Override
		public String toString() {
			return "Attachment name ["+name+"] type ["+value.getClass().getSimpleName()+"]";
		}
	}
	protected class MailAttachmentStream extends MailAttachmentBase<InputStream>{};

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

		@Override
		public String toString() {
			return "address ["+address+"] name ["+name+"] type ["+type+"]";
		}
	}

}
