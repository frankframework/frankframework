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

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.parameters.ParameterValue;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.util.DomBuilderException;
import nl.nn.adapterframework.util.XmlUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.soap.util.mime.ByteArrayDataSource;
import org.apache.xerces.impl.dv.util.Base64;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class MailSenderBase extends SenderWithParametersBase {

	EMail from = new EMail();
	EMail replyTo = new EMail();
	List<EMail> emailList = new ArrayList<EMail>();
	List<Attachment> attachmentList = new ArrayList<Attachment>();
	String subject = null;
	String message = null;
	String messageType = null;
	String messageBase64 = null;
	String charSet = null;
	String threadTopic = null;
	Collection headers;

	/**
	 * Reads fields from either paramList or Xml file
	 * @param input : XML file
	 * @param prc
	 * @throws SenderException
	 * @throws DomBuilderException
	 */
	public void extract(String input, ParameterResolutionContext prc) throws SenderException,
			DomBuilderException {
		if (paramList == null) {
			parseXML(input, prc);
		} else {
			readParameters(prc);
		}
	}

	/**
	 * Reads fields from paramList
	 * @param prc
	 * @throws SenderException
	 */
	private void readParameters(ParameterResolutionContext prc) throws SenderException {
		String subject = null;
		String threadTopic = null;
		String messageType = null;
		String messageBase64 = null;
		String charset = null;
		Collection<EMail> recipients = null;
		Collection<Attachment> attachments = null;
		ParameterValueList pvl;
		ParameterValue pv;

		try {
			pvl = prc.getValues(paramList);
			pv = pvl.getParameterValue("from");
			if (pv != null) {
				from.setAddress(pv.asStringValue(null));
				log.debug("MailSender [" + getName() + "] retrieved from-parameter [" + from + "]");
			}
			pv = pvl.getParameterValue("subject");
			if (pv != null) {
				subject = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved subject-parameter [" + subject
						+ "]");
			}
			pv = pvl.getParameterValue("threadTopic");
			if (pv != null) {
				threadTopic = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved threadTopic-parameter ["
						+ threadTopic + "]");
			}
			pv = pvl.getParameterValue("message");
			if (pv != null) {
				message = pv.asStringValue(message);
				log.debug("MailSender [" + getName() + "] retrieved message-parameter [" + message
						+ "]");
			}
			pv = pvl.getParameterValue("messageType");
			if (pv != null) {
				messageType = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved messageType-parameter ["
						+ messageType + "]");
			}
			pv = pvl.getParameterValue("messageBase64");
			if (pv != null) {
				messageBase64 = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved messageBase64-parameter ["
						+ messageBase64 + "]");
			}
			pv = pvl.getParameterValue("charset");
			if (pv != null) {
				charset = pv.asStringValue(null);
				log.debug("MailSender [" + getName() + "] retrieved charset-parameter [" + charset
						+ "]");
			}
			pv = pvl.getParameterValue("recipients");
			if (pv != null) {
				recipients = retrieveRecipients(pv.asCollection());
				log.debug("MailSender [" + getName() + "] retrieved recipients-parameter ["
						+ recipients + "]");
			}
			pv = pvl.getParameterValue("attachments");
			if (pv != null) {
				attachments = retrieveAttachments(pv.asCollection(), prc);
				log.debug("MailSender [" + getName() + "] retrieved attachments-parameter ["
						+ attachments + "]");
			}
		} catch (ParameterException e) {
			throw new SenderException("MailSender [" + getName()
					+ "] got exception determining parametervalues", e);
		}
	}

	private Collection<Attachment> retrieveAttachments(Collection<Node> attachmentsNode,
			ParameterResolutionContext prc) throws SenderException {
		Collection<Attachment> attachments = null;
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
							attachmentDataSource = new ByteArrayDataSource((InputStream) object,
									"application/octet-stream");
						} catch (IOException e) {
							throw new SenderException(
									"error retrieving attachment from sessionkey", e);
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
						throw new SenderException("MailSender [" + getName()
								+ "] received unknown attachment type ["
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

	private List<EMail> retrieveRecipients(Collection<Node> recipientsNode) {
		List<EMail> recipients = null;
		Iterator iter = recipientsNode.iterator();
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
		return recipients;
	}

	private void parseXML(String input, ParameterResolutionContext prc) throws SenderException,
			DomBuilderException {
		Collection<Node> recipients = null;
		Collection<Node> attachments = null;

		Element fromElement = null;
		String date = null;
		Element replyTo = null;
		Element emailElement = XmlUtils.buildElement(input);
		Element recipientsElement = XmlUtils.getFirstChildTag(emailElement, "recipients");
		recipients = XmlUtils.getChildTags(recipientsElement, "recipient");

		subject = XmlUtils.getChildTagAsString(emailElement, "subject");
		fromElement = XmlUtils.getFirstChildTag(emailElement, "from");
		message = XmlUtils.getChildTagAsString(emailElement, "message");
		messageType = XmlUtils.getChildTagAsString(emailElement, "messageType");
		messageBase64 = XmlUtils.getChildTagAsString(emailElement, "messageBase64");
		threadTopic = XmlUtils.getChildTagAsString(emailElement, "threadTopic");
		replyTo = XmlUtils.getFirstChildTag(emailElement, "replyTo");
		date = XmlUtils.getChildTagAsString(emailElement, "date");
		// TODO : can be customized to send the email scheduled time.
		charSet = XmlUtils.getChildTagAsString(emailElement, "charset");
		Element attachmentsElement = XmlUtils.getFirstChildTag(emailElement, "attachments");
		attachments = attachmentsElement == null ? null : XmlUtils.getChildTags(attachmentsElement,
				"attachment");
		Element headersElement = XmlUtils.getFirstChildTag(emailElement, "headers");
		headers = headersElement == null ? null : XmlUtils.getChildTags(headersElement, "header");
		getFrom(fromElement);
		getReplyTo(replyTo);
		emailList = retrieveRecipients(recipients);
		attachmentList = (List<Attachment>) retrieveAttachments(attachments, prc);
	}

	private void getFrom(Element fromElement) {
		String value = XmlUtils.getStringValue(fromElement);
		if (StringUtils.isNotEmpty(value)) {
			from.setAddress(value);
			from.setName(fromElement.getAttribute("name"));
			from.setType("from");
		}
	}

	private void getReplyTo(Element replyToElement) {
		String value = XmlUtils.getStringValue(replyToElement);
		if (StringUtils.isNotEmpty(value)) {
			replyTo.setAddress(value);
			replyTo.setName(replyToElement.getAttribute("name"));
			replyTo.setType("replyTo");
		}
	}

	private DataHandler decodeBase64(String str) {
		byte[] bytesDecoded = Base64.decode(str);
		String encodingType = "application/octet-stream";
		DataSource ads = new ByteArrayDataSource(bytesDecoded, encodingType);
		return new DataHandler(ads);
	}

	/**
	 * Generic attachment class
	 * @author alisihab
	 *
	 */
	public class Attachment {
		String attachmentName;
		String attachmentType;
		String attachmentURL;
		Object attachmentText;
		String attachmentBase64;
		String sessionKey;

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
	public class EMail {
		String address;
		String name;
		String type; //"cc", "to", "from", "bcc" 

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

	@Override
	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc)
			throws SenderException, TimeOutException {
		readParameters(prc);
		return null;
	}
}
