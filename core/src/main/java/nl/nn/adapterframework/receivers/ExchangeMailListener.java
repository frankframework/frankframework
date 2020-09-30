/*
   Copyright 2016, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.io.ByteArrayInputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import microsoft.exchange.webservices.data.property.complex.AttachmentCollection;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.EmailAddressCollection;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeader;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeaderCollection;
import microsoft.exchange.webservices.data.property.complex.ItemAttachment;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.property.complex.MimeContent;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.ExchangeFileSystem;
import nl.nn.adapterframework.filesystem.FileSystemListener;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Implementation of a {@link nl.nn.adapterframework.filesystem.FileSystemListener
 * FileSystemListener} that enables a
 * {@link nl.nn.adapterframework.receivers.GenericReceiver} to look in a folder
 * for received mails. When a mail is found, it is moved to an output folder (or
 * it's deleted), so that it isn't found more then once. A xml string with
 * information about the mail is passed to the pipeline.
 * 
 * <p>
 * <b>example:</b> <code><pre>
 *   &lt;email&gt;
 *      &lt;recipients&gt;
 *         &lt;recipient type="to"&gt;***@nn.nl&lt;/recipient&gt;
 *         &lt;recipient type="cc"&gt;***@nn.nl&lt;/recipient&gt;
 *      &lt;/recipients&gt;
 *      &lt;from&gt;***@nn.nl&lt;/from&gt;
 *      &lt;subject&gt;this is the subject&lt;/subject&gt;
 *      &lt;headers&gt;
 *         &lt;header name="prop1"&gt;<i>value of first header property</i>&lt;/header&gt;
 *         &lt;header name="prop2"&gt;<i>value of second header property</i>&lt;/header&gt;
 *      &lt;/headers&gt;
 *      &lt;dateTimeSent&gt;2015-11-18T11:40:19.000+0100&lt;/dateTimeSent&gt;
 *      &lt;dateTimeReceived&gt;2015-11-18T11:41:04.000+0100&lt;/dateTimeReceived&gt;
 *   &lt;/email&gt;
 * </pre></code>
 * </p>
 * 
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setName(String) name}</td><td>name of the listener</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFolder(String) inputFolder}</td><td>folder (subfolder of inbox) to look for mails. If empty, the inbox folder is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilter(String) filter}</td><td>If empty, all mails are retrieved. If 'NDR' only Non-Delivery Report mails ('bounces') are retrieved</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInProcessFolder(String) inProcessFolder}</td><td>folder (subfolder of inbox) where mails are stored <i>while</i> being processed. If empty, processed mails are not temporarily moved</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProcessedFolder(String) processedFolder}</td><td>folder (subfolder of inbox) where mails are stored after being processed. If empty, processed mails are deleted</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to exchange mail server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setMailAddress(String) mailAddress}</td><td>mail address (also used for auto discovery)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>(only used when mailAddress is empty) url of the service</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setStoreEmailAsStreamInSessionKey(String) storeEmailAsStreamInSessionKey}</td><td>if set, the mail is streamed to a file (eml)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSimple(boolean) simple}</td><td>when set to <code>true</code>, the xml string passed to the pipeline only contains the subject of the mail (to save memory)</td><td>false</td></tr>
 * <tr><td>{@link #setProxyHost(String) proxyHost}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPort(int) proxyPort}</td><td>&nbsp;</td><td>80</td></tr>
 * <tr><td>{@link #setProxyAuthAlias(String) proxyAuthAlias}</td><td>alias used to obtain credentials for authentication to proxy</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyUserName(String) proxyUserName}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setProxyPassword(String) proxyPassword}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * </table></p>
 * 
 * @author Peter Leeuwenburgh, Gerrit van Brakel
 */
public class ExchangeMailListener extends FileSystemListener<Item,ExchangeFileSystem> implements HasPhysicalDestination {

	public final String EMAIL_MESSAGE_TYPE="email";
	public final String EXCHANGE_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ExchangeFileSystem";
	
	private String storeEmailAsStreamInSessionKey;
	private boolean simple = false;
	
	{
		setMessageType(EMAIL_MESSAGE_TYPE);
	}
	
	@Override
	protected ExchangeFileSystem createFileSystem() {
		return new ExchangeFileSystem();
	}


	@Override
	public Message extractMessage(Item rawMessage, Map<String,Object> threadContext) throws ListenerException {
		if (!EMAIL_MESSAGE_TYPE.equals(getMessageType())) {
			return super.extractMessage(rawMessage, threadContext);
		}
		Item item = (Item) rawMessage;
		try {
			XmlBuilder emailXml = new XmlBuilder("email");
			EmailMessage emailMessage;
			PropertySet ps;
			if (isSimple()) {
				ps = new PropertySet(EmailMessageSchema.Subject);
				emailMessage = EmailMessage.bind(getFileSystem().getExchangeService(), item.getId(), ps);
				emailMessage.load();
				addEmailInfoSimple(emailMessage, emailXml);
			} else {
				ps = new PropertySet(EmailMessageSchema.DateTimeReceived, EmailMessageSchema.From, EmailMessageSchema.Subject, EmailMessageSchema.Body, EmailMessageSchema.DateTimeSent);
				emailMessage = EmailMessage.bind(getFileSystem().getExchangeService(), item.getId(), ps);
				emailMessage.load();
				addEmailInfo(emailMessage, emailXml);
			}

			if (StringUtils.isNotEmpty(getStoreEmailAsStreamInSessionKey())) {
				emailMessage.load(new PropertySet(ItemSchema.MimeContent));
				MimeContent mc = emailMessage.getMimeContent();
				ByteArrayInputStream bis = new ByteArrayInputStream(mc.getContent());
				threadContext.put(getStoreEmailAsStreamInSessionKey(), bis);
			}

			return new Message(emailXml.toXML());
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	private void addEmailInfoSimple(EmailMessage emailMessage, XmlBuilder emailXml) throws Exception {
		XmlBuilder subjectXml = new XmlBuilder("subject");
		subjectXml.setCdataValue(emailMessage.getSubject());
		emailXml.addSubElement(subjectXml);
	}

	private void addEmailInfo(EmailMessage emailMessage, XmlBuilder emailXml) throws Exception {
		XmlBuilder recipientsXml = new XmlBuilder("recipients");
		EmailAddressCollection eacTo = emailMessage.getToRecipients();
		if (eacTo != null) {
			for (Iterator<EmailAddress> it = eacTo.iterator(); it.hasNext();) {
				XmlBuilder recipientXml = new XmlBuilder("recipient");
				EmailAddress ea = it.next();
				recipientXml.addAttribute("type", "to");
				recipientXml.setValue(ea.getAddress());
				recipientsXml.addSubElement(recipientXml);
			}
		}
		EmailAddressCollection eacCc = emailMessage.getCcRecipients();
		if (eacCc != null) {
			for (Iterator<EmailAddress> it = eacCc.iterator(); it.hasNext();) {
				XmlBuilder recipientXml = new XmlBuilder("recipient");
				EmailAddress ea = it.next();
				recipientXml.addAttribute("type", "cc");
				recipientXml.setValue(ea.getAddress());
				recipientsXml.addSubElement(recipientXml);
			}
		}
		EmailAddressCollection eacBcc = emailMessage.getBccRecipients();
		if (eacBcc != null) {
			for (Iterator<EmailAddress> it = eacBcc.iterator(); it.hasNext();) {
				XmlBuilder recipientXml = new XmlBuilder("recipient");
				EmailAddress ea = it.next();
				recipientXml.addAttribute("type", "bcc");
				recipientXml.setValue(ea.getAddress());
				recipientsXml.addSubElement(recipientXml);
			}
		}
		emailXml.addSubElement(recipientsXml);
		XmlBuilder fromXml = new XmlBuilder("from");
		fromXml.setValue(emailMessage.getFrom().getAddress());
		emailXml.addSubElement(fromXml);
		XmlBuilder subjectXml = new XmlBuilder("subject");
		subjectXml.setCdataValue(emailMessage.getSubject());
		emailXml.addSubElement(subjectXml);
		XmlBuilder messageXml = new XmlBuilder("message");
		messageXml.setCdataValue(MessageBody.getStringFromMessageBody(emailMessage.getBody()));
		emailXml.addSubElement(messageXml);
		XmlBuilder attachmentsXml = new XmlBuilder("attachments");
		try {
			AttachmentCollection ac = emailMessage.getAttachments();
			if (ac != null) {
				for (Iterator<Attachment> it = ac.iterator(); it.hasNext();) {
					XmlBuilder attachmentXml = new XmlBuilder("attachment");
					Attachment att = (Attachment) it.next();
					att.load();
					attachmentXml.addAttribute("name", att.getName());
					if (att instanceof ItemAttachment) {
						ItemAttachment ia = (ItemAttachment) att;
						Item aItem = ia.getItem();
						if (aItem instanceof EmailMessage) {
							EmailMessage em;
							em = (EmailMessage) aItem;
							addEmailInfo(em, attachmentXml);
						}
					}
					attachmentsXml.addSubElement(attachmentXml);
				}
			}
		} catch (Exception e) {
			log.info("error occurred during getting internet message attachment(s): " + e.getMessage());
		}
		emailXml.addSubElement(attachmentsXml);
		XmlBuilder headersXml = new XmlBuilder("headers");
		InternetMessageHeaderCollection imhc = null;
		try {
			imhc = emailMessage.getInternetMessageHeaders();
		} catch (Exception e) {
			log.info("error occurred during getting internet message headers: " + e.getMessage());
		}
		if (imhc != null) {
			for (Iterator<InternetMessageHeader> it = imhc.iterator(); it.hasNext();) {
				XmlBuilder headerXml = new XmlBuilder("header");
				InternetMessageHeader imh = (InternetMessageHeader) it.next();
				headerXml.addAttribute("name", imh.getName());
				headerXml.setCdataValue(imh.getValue());
				headersXml.addSubElement(headerXml);
			}
		}
		emailXml.addSubElement(headersXml);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date dateTimeSend = emailMessage.getDateTimeSent();
		XmlBuilder dateTimeSentXml = new XmlBuilder("dateTimeSent");
		dateTimeSentXml.setValue(sdf.format(dateTimeSend));
		emailXml.addSubElement(dateTimeSentXml);
		Date dateTimeReceived = emailMessage.getDateTimeReceived();
		XmlBuilder dateTimeReceivedXml = new XmlBuilder("dateTimeReceived");
		dateTimeReceivedXml.setValue(sdf.format(dateTimeReceived));
		emailXml.addSubElement(dateTimeReceivedXml);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'outputFolder' has been replaced by 'processedFolder'")
	public void setOutputFolder(String outputFolder) {
		setProcessedFolder(outputFolder);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'tempFolder' has been replaced by 'inProcessFolder'")
	public void setTempFolder(String tempFolder) {
		setInProcessFolder(tempFolder);
	}

	@IbisDocRef({"1", EXCHANGE_FILE_SYSTEM})
	public void setMailAddress(String mailAddress) {
		getFileSystem().setMailAddress(mailAddress);
	}

	@IbisDocRef({"3", EXCHANGE_FILE_SYSTEM})
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}
	

	@IbisDocRef({"4", EXCHANGE_FILE_SYSTEM})
	public void setAccessToken(String accessToken) {
		getFileSystem().setAccessToken(accessToken);
	}

	@IbisDocRef({"5", EXCHANGE_FILE_SYSTEM})
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDocRef({"6", EXCHANGE_FILE_SYSTEM})
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@IbisDocRef({"7", EXCHANGE_FILE_SYSTEM})
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@IbisDocRef({"8", EXCHANGE_FILE_SYSTEM})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDocRef({"9", EXCHANGE_FILE_SYSTEM})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDocRef({"10", EXCHANGE_FILE_SYSTEM})
	public void setProxyUserName(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@IbisDocRef({"11", EXCHANGE_FILE_SYSTEM})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDocRef({"12", EXCHANGE_FILE_SYSTEM})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDocRef({"13", EXCHANGE_FILE_SYSTEM})
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

	
	@IbisDoc({"14", "when set to <code>true</code>, the xml string passed to the pipeline only contains the subject of the mail (to save memory)", ""})
	public void setSimple(boolean b) {
		simple = b;
	}
	public boolean isSimple() {
		return simple;
	}

	public void setStoreEmailAsStreamInSessionKey(String string) {
		storeEmailAsStreamInSessionKey = string;
	}
	public String getStoreEmailAsStreamInSessionKey() {
		return storeEmailAsStreamInSessionKey;
	}


}