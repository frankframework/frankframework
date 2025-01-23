/*
   Copyright 2019, 2020, 2022-2024 WeAreFrank!

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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.ParameterValue;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DomBuilderException;
import org.frankframework.util.StreamUtil;
import org.frankframework.util.StringUtil;
import org.frankframework.util.XmlUtils;

/**
 *
 * @ff.parameter from email address of the sender
 * @ff.parameter subject subject field of the message
 * @ff.parameter threadTopic (optional) conversation field of the message, used to correlate mails in mail viewer (header field "Thread-Topic"). Note: subject must end with value of threadTopic, but cann't be exactly the same
 * @ff.parameter message message itself. If absent, the complete input message is assumed to be the message
 * @ff.parameter messageType message MIME type (at this moment only available are <code>text/plain</code> and <code>text/html</code> - default: <code>text/plain</code>)
 * @ff.parameter messageBase64 (boolean) indicates whether the message content is base64 encoded (default: <code>false</code>)
 * @ff.parameter charSet the character encoding (e.g. ISO-8859-1 or UTF-8) used to send the email (default: UTF-8)
 * @ff.parameter recipients (xml) recipients of the message. Must result in a structure like: <code><pre>
 *       &lt;recipient type="to"&gt;***@hotmail.com&lt;/recipient&gt;
 *       &lt;recipient type="cc"&gt;***@gmail.com&lt;/recipient&gt;
 * </pre></code>
 * @ff.parameter attachments (xml) attachments to the message. Must result in a structure like: <code><pre>
 *       &lt;attachment name="filename1.txt"&gt;This is the first attachment&lt;/attachment&gt;
 *       &lt;attachment name="filename2.pdf" base64="true"&gt;JVBERi0xLjQKCjIgMCBvYmoKPDwvVHlwZS9YT2JqZWN0L1N1YnR5cGUvSW1...vSW5mbyA5IDAgUgo+PgpzdGFydHhyZWYKMzQxNDY2CiUlRU9GCg==&lt;/attachment&gt;
 *       &lt;attachment name="filename3.pdf" url="file:/c:/filename3.pdf"/&gt;
 *       &lt;attachment name="filename4.pdf" sessionKey="fileContent"/&gt;
 * </pre></code>
 *
 */
public abstract class AbstractMailSender extends AbstractSenderWithParameters {

	private @Getter String authAlias;
	private @Getter String userId;
	private @Getter String password;
	private @Getter CredentialFactory credentialFactory;

	private @Getter String defaultAttachmentName = "attachment";
	private @Getter String defaultMessageType = "text/plain";
	private @Getter boolean defaultMessageBase64 = false;
	private @Getter String defaultSubject;
	private @Getter String defaultFrom;
	private @Getter int timeout = 20000;
	private @Getter String bounceAddress;
	private @Getter String domainWhitelist;

	private final ArrayList<String> allowedDomains = new ArrayList<>();

	protected abstract void sendEmail(MailSessionBase mailSession) throws SenderException;

	@Override
	public void configure() throws ConfigurationException {
		credentialFactory = new CredentialFactory(getAuthAlias(), getUserId(), getPassword());

		if (StringUtils.isNotEmpty(getDomainWhitelist())) {
			allowedDomains.addAll(StringUtil.split(getDomainWhitelist()));
		}

		super.configure();
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		MailSessionBase mailSession;
		try {
			mailSession = extract(message, session);
		} catch (DomBuilderException e) {
			throw new SenderException(e);
		}
		sendEmail(mailSession);

		String messageID = session.getMessageId();
		return new SenderResult(messageID);
	}

	/**
	 * Reads fields from either paramList or Xml file
	 */
	public MailSessionBase extract(Message input, PipeLineSession session) throws SenderException, DomBuilderException {
		MailSessionBase mailSession;
		if (paramList.isEmpty()) {
			mailSession = parseXML(input, session);
		} else {
			mailSession = readParameters(input, session);
		}
		return mailSession;
	}

	private @Nonnull Collection<MailAttachmentStream> retrieveAttachmentsFromParamList(@Nullable ParameterValue pv, @Nonnull PipeLineSession session) throws SenderException, ParameterException {
		if (pv == null) {
			return Collections.emptyList();
		}
		Collection<MailAttachmentStream> attachments = retrieveAttachments(pv.asCollection(), session);
		log.debug("MailSender [{}] retrieved attachments-parameter [{}]", getName(), attachments);
		return attachments;
	}

	private @Nonnull Collection<EMail> retrieveRecipientsFromParameterList(ParameterValue pv)
			throws ParameterException, SenderException {
		if (pv == null) {
			return Collections.emptyList();
		}
		Collection<EMail> recipients = retrieveRecipients(pv.asCollection());
		log.debug("MailSender [{}] retrieved recipients-parameter [{}]", getName(), recipients);
		return recipients;
	}

	private MailSessionBase readParameters(Message input, PipeLineSession session) throws SenderException {
		EMail from;
		EMail replyTo;
		String subject;
		String threadTopic;
		String messageType;
		boolean messageBase64;
		String charset;
		List<EMail> recipients;
		List<MailAttachmentStream> attachments;
		ParameterValueList pvl;
		ParameterValue pv;

		MailSessionBase mail = createMailSession();
		try {
			pvl = paramList.getValues(input, session);
			pv = pvl.get("from");
			if (pv != null) {
				from = new EMail(pv.asStringValue(null));
				log.debug("MailSender [{}] retrieved from-parameter [{}]", getName(), from);
				mail.setFrom(from);
			}
			pv = pvl.get("replyTo");
			if (pv != null) {
				replyTo = new EMail(pv.asStringValue(null));
				log.debug("MailSender [{}] retrieved replyTo-parameter [{}]", getName(), replyTo);
				mail.setReplyTo(replyTo);
			}
			pv = pvl.get("subject");
			if (pv != null) {
				subject = pv.asStringValue(null);
				log.debug("MailSender [{}] retrieved subject-parameter [{}]", getName(), subject);
				mail.setSubject(subject);
			}
			pv = pvl.get("threadTopic");
			if (pv != null) {
				threadTopic = pv.asStringValue(null);
				log.debug("MailSender [{}] retrieved threadTopic-parameter [{}]", getName(), threadTopic);
				mail.setThreadTopic(threadTopic);
			}
			pv = pvl.get("message");
			if (pv != null) {
				String message = pv.asStringValue("message");
				log.debug("MailSender [{}] retrieved message-parameter [{}]", getName(), message);
				mail.setMessage(message);
			}
			pv = pvl.get("messageType");
			if (pv != null) {
				messageType = pv.asStringValue(null);
				log.debug("MailSender [{}] retrieved messageType-parameter [{}]", getName(), messageType);
				mail.setMessageType(messageType);
			}
			pv = pvl.get("messageBase64");
			if (pv != null) {
				messageBase64 = pv.asBooleanValue(false);
				log.debug("MailSender [{}] retrieved messageBase64-parameter [{}]", getName(), messageBase64);
				mail.setMessageBase64(messageBase64);
			}
			pv = pvl.get("charset");
			if (pv != null) {
				charset = pv.asStringValue(null);
				log.debug("MailSender [{}] retrieved charset-parameter [{}]", getName(), charset);
				mail.setCharSet(charset);
			}
			pv = pvl.get("recipients");
			Collection<EMail> recipientsCollection = retrieveRecipientsFromParameterList(pv);
			if (!recipientsCollection.isEmpty()) {
				recipients = new ArrayList<>(recipientsCollection);
				mail.setRecipientList(recipients);
			} else {
				throw new SenderException("Recipients cannot be empty. At least one recipient is required");
			}

			pv = pvl.get("attachments");
			Collection<MailAttachmentStream> attachmentsCollection = retrieveAttachmentsFromParamList(pv, session);
			if (!attachmentsCollection.isEmpty()) {
				attachments = new ArrayList<>(attachmentsCollection);
				mail.setAttachmentList(attachments);
			}

		} catch (ParameterException e) {
			throw new SenderException("MailSender [" + getName() + "] got exception determining parametervalues", e);
		}
		return mail;
	}

	private @Nonnull List<EMail> retrieveRecipients(@Nonnull Collection<Node> recipientsNode) throws SenderException {
		if (recipientsNode.isEmpty()) {
			return Collections.emptyList();
		}
		List<EMail> recipients = new ArrayList<>(recipientsNode.size());
		for (Node node : recipientsNode) {
			Element recipientElement = (Element) node;
			String value = XmlUtils.getStringValue(recipientElement);
			if (!StringUtils.isNotEmpty(value)) {
				log.debug("empty recipient found, ignoring");
				continue;
			}
			String name = recipientElement.getAttribute("name");
			String type = recipientElement.getAttribute("type");
			EMail recipient = new EMail(value, name, StringUtils.isNotEmpty(type) ? type : "to");
			recipients.add(recipient);
		}

		return recipients;
	}

	private @Nonnull Collection<MailAttachmentStream> retrieveAttachments(@Nonnull Collection<Node> attachmentsNode, @Nonnull PipeLineSession session) throws SenderException {
		if (attachmentsNode.isEmpty()) {
			return Collections.emptyList();
		}
		Collection<MailAttachmentStream> attachments = new ArrayList<>(attachmentsNode.size());
		for (Node node : attachmentsNode) {
			Element attachmentElement = (Element) node;
			String name = attachmentElement.getAttribute("name");
			String mimeType = attachmentElement.getAttribute("type");
			if (StringUtils.isNotEmpty(mimeType) && !mimeType.contains("/")) {
				throw new SenderException("mimeType [" + mimeType + "] of attachment [" + name + "] must contain a forward slash ('/')");
			}
			String sessionKey = attachmentElement.getAttribute("sessionKey");
			boolean base64 = Boolean.parseBoolean(attachmentElement.getAttribute("base64"));

			MailAttachmentStream attachment;
			if (StringUtils.isNotEmpty(sessionKey)) {
				Object object = session.get(sessionKey);
				if (object instanceof InputStream stream) {
					attachment = streamToMailAttachment(stream, base64, mimeType);
				} else if (object instanceof String string) {
					attachment = stringToMailAttachment(string, base64, mimeType);
				} else {
					throw new SenderException("MailSender [" + getName() + "] received unknown attachment type [" + object.getClass()
							.getName() + "] in sessionkey");
				}
			} else {
				String nodeValue = XmlUtils.getStringValue(attachmentElement);
				attachment = stringToMailAttachment(nodeValue, base64, mimeType);
			}
			attachment.setName(name);
			log.debug("created attachment [{}]", attachment);
			attachments.add(attachment);
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

	private MailSessionBase parseXML(Message input, PipeLineSession session) throws SenderException, DomBuilderException {
		Element from;
		String subject;
		String threadTopic;
		String message;
		String messageType;
		boolean messageBase64;
		String charset;
		Collection<Node> recipientList;
		Collection<Node> attachments;
		Element replyTo;

		MailSessionBase mailSession = createMailSession();

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
		if (!messageType.contains("/")) {
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
		if (StringUtils.isNotBlank(bounceAddress)) {
			mailSession.setBounceAddress(bounceAddress);
		}

		mailSession.setFrom(getEmailAddress(from, "from"));
		mailSession.setSubject(subject);
		mailSession.setThreadTopic(threadTopic);
		mailSession.setMessage(message);
		mailSession.setMessageType(messageType);
		mailSession.setMessageBase64(messageBase64);
		mailSession.setCharSet(charset);
		mailSession.setHeaders(headers);
		mailSession.setReplyTo(getEmailAddress(replyTo,"replyTo"));

		List<EMail> recipients = retrieveRecipients(recipientList);
		mailSession.setRecipientList(recipients);
		if (attachments != null) {
			List<MailAttachmentStream> attachmentList = (List<MailAttachmentStream>) retrieveAttachments(attachments, session);
			mailSession.setAttachmentList(attachmentList);
		}

		return mailSession;
	}

	protected abstract MailSessionBase createMailSession() throws SenderException;

	private @Nullable EMail getEmailAddress(Element element, String type) throws SenderException {
		if (element == null) {
			return null;
		}
		String value = XmlUtils.getStringValue(element);
		if (StringUtils.isNotEmpty(value)) {
			return new EMail(value, element.getAttribute("name"), type);
		}
		return null;
	}

	@Override
	public boolean isSynchronous() {
		return false;
	}

	/** authAlias used to obtain credentials for authentication */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/** userId on the smtphost */
	public void setUserId(String userId) {
		this.userId = userId;
	}

	/** password of userid */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Set the default value of the subject: header, if not specified in message itself */
	public void setDefaultSubject(String defaultSubject) {
		this.defaultSubject = defaultSubject;
	}

	/**
	 * Set the default from: header, if not specified in message itself */
	public void setDefaultFrom(String defaultFrom) {
		this.defaultFrom = defaultFrom;
	}

	/**
	 * Timeout <i>in milliseconds</i> for socket connection timeout and socket i/o timeouts
	 * @ff.default 20000
	 */
	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

	/**
	 * When this name is used, it will be followed by a number which is equal to the node's position
	 * @ff.default attachment
	 */
	public void setDefaultAttachmentName(String defaultAttachmentName) {
		this.defaultAttachmentName = defaultAttachmentName;
	}

	/**
	 * when messageType is not specified defaultMessageType will be used
	 * @ff.default text/plain
	 */
	public void setDefaultMessageType(String defaultMessageType) {
		this.defaultMessageType = defaultMessageType;
	}

	/**
	 * when messageBase64 is not specified defaultMessageBase64 will be used
	 * @ff.default false
	 */
	public void setDefaultMessageBase64(boolean defaultMessageBase64) {
		this.defaultMessageBase64 = defaultMessageBase64;
	}

	/**
	 * NDR return address when mail cannot be delivered. This adds a Return-Path header
	 * @ff.default MAIL FROM attribute
	 */
	public void setBounceAddress(String string) {
		bounceAddress = string;
	}

	/** Comma separated list of domains to which mails can be send, domains not on the list are filtered out. Empty allows all domains */
	public void setDomainWhitelist(String domainWhitelist) {
		this.domainWhitelist = domainWhitelist;
	}

	/**
	 * Generic email class
	 */
	public abstract class MailSessionBase {
		private EMail from;
		private EMail replyTo = null;
		private List<EMail> recipients = new ArrayList<>();
		private List<MailAttachmentStream> attachmentList = new ArrayList<>();
		private String subject = getDefaultSubject();
		private String message = null;
		private String messageType = getDefaultMessageType();
		private boolean messageIsBase64 = isDefaultMessageBase64();
		private String charSet = StreamUtil.DEFAULT_INPUT_STREAM_ENCODING;
		private String threadTopic = null;
		private Collection<Node> headers;
		private String bounceAddress = AbstractMailSender.this.getBounceAddress();

		public MailSessionBase() throws SenderException {
			from = new EMail(getDefaultFrom(), "from");
		}

		public void setRecipientsOnMessage(StringBuilder logBuffer) throws SenderException {
			boolean recipientsFound = false;
			List<EMail> emailList = getRecipientList();
			for (EMail recipient : emailList) {
				recipientsFound = true;

				if(isRecipientWhitelisted(recipient)) {
					addRecipientToMessage(recipient);
					if (log.isDebugEnabled()) {
						logBuffer.append("[recipient [").append(recipient).append("]]");
					}
				} else {
					log.warn("Recipient [{}] ignored, not in domain whitelist [{}]", recipient, getDomainWhitelist());
				}
			}
			if (!recipientsFound) {
				throw new SenderException("Sender [" + getName() + "] did not find any valid recipients");
			}
		}

		private boolean isRecipientWhitelisted(EMail recipient) {
			return allowedDomains.isEmpty() || allowedDomains.contains(StringUtils.substringAfter(recipient.getAddress(),'@').toLowerCase());
		}

		protected abstract void addRecipientToMessage(EMail recipient) throws SenderException;

		protected abstract boolean hasWhitelistedRecipients() throws SenderException;

		public EMail getFrom() {
			return from;
		}

		public void setFrom(EMail from) {
			this.from = from;
		}

		public EMail getReplyTo() {
			return replyTo;
		}

		public void setReplyTo(EMail replyTo) {
			this.replyTo = replyTo;
		}

		public List<EMail> getRecipientList() throws SenderException {
			if (recipients == null || recipients.isEmpty()) {
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
	protected abstract static class MailAttachmentBase<T> {
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

	public static class MailAttachmentStream extends MailAttachmentBase<InputStream>{}

	/**
	 * Generic mail class
	 * @author alisihab
	 *
	 */
	public static class EMail {
		private final InternetAddress emailAddress;
		private final String type; //"cc", "to", "from", "bcc"

		public EMail(String address, String name, String type) throws SenderException {
			try {
				if (StringUtils.isNotEmpty(address)) {
					InternetAddress[] ia = InternetAddress.parseHeader(address, true);
					if (ia.length==0) {
						throw new AddressException("No address found in ["+address+"]");
					}
					emailAddress = ia[0];
				} else {
					emailAddress = new InternetAddress();
				}
				if (StringUtils.isNotEmpty(name)) {
					emailAddress.setPersonal(name);
				}
				this.type = type;
			} catch (AddressException | UnsupportedEncodingException e) {
				throw new SenderException("cannot parse email address from ["+address+"] ["+name+"]", e);
			}
		}

		public EMail(String address, String type) throws SenderException {
			this(address, null, type);
		}

		public EMail(String address) throws SenderException {
			this(address, null, null);
		}

		public InternetAddress getInternetAddress() {
			return emailAddress;
		}

		public String getAddress() {
			return emailAddress.getAddress();
		}

		public String getName() {
			return emailAddress.getPersonal();
		}

		public String getType() {
			return type;
		}

		@Override
		public String toString() {
			return "address ["+emailAddress.toUnicodeString()+"] type ["+type+"]";
		}
	}

}
