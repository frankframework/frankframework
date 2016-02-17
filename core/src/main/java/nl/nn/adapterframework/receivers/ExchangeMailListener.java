/*
   Copyright 2016 Nationale-Nederlanden

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
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import microsoft.exchange.webservices.data.property.complex.AttachmentCollection;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.EmailAddressCollection;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeader;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeaderCollection;
import microsoft.exchange.webservices.data.property.complex.ItemAttachment;
import microsoft.exchange.webservices.data.property.complex.Mailbox;
import microsoft.exchange.webservices.data.property.complex.MessageBody;
import microsoft.exchange.webservices.data.property.complex.MimeContent;
import microsoft.exchange.webservices.data.search.FindFoldersResults;
import microsoft.exchange.webservices.data.search.FindItemsResults;
import microsoft.exchange.webservices.data.search.FolderView;
import microsoft.exchange.webservices.data.search.ItemView;
import microsoft.exchange.webservices.data.search.filter.SearchFilter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPullingListener;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.XmlBuilder;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Implementation of a {@link nl.nn.adapterframework.core.IPullingListener
 * IPullingListener} that enables a
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
 * <tr>
 * <th>attributes</th>
 * <th>description</th>
 * <th>default</th>
 * </tr>
 * <tr>
 * <td>{@link #setName(String) name}</td>
 * <td>name of the listener</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setInputFolder(String) inputFolder}</td>
 * <td>folder (subfolder of inbox) to look for mails. If empty, the inbox folder
 * is used</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setFilter(String) filter}</td>
 * <td>If empty, all mails are retrieved. If 'NDR' only Non-Delivery Report
 * mails ('bounces') are retrieved</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setOutputFolder(String) outputFolder}</td>
 * <td>folder (subfolder of inbox) where mails are stored after being processed.
 * If empty, processed mails are deleted</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setAuthAlias(String) authAlias}</td>
 * <td>alias used to obtain credentials for authentication to exchange mail
 * server</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setUserName(String) userName}</td>
 * <td>username used in authentication to exchange mail server</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setPassword(String) password}</td>
 * <td>&nbsp;</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setMailAddress(String) mailAddress}</td>
 * <td>mail address (also used for auto discovery)</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setUrl(String) url}</td>
 * <td>(only used when mailAddress is empty) url of the service</td>
 * <td>&nbsp;</td>
 * </tr>
 * <tr>
 * <td>{@link #setStoreEmailAsStreamInSessionKey(String)
 * storeEmailAsStreamInSessionKey}</td>
 * <td>if set, the mail is streamed to a file (eml)</td>
 * <td>&nbsp;</td>
 * </tr>
 * </table>
 * </p>
 * 
 * @author Peter Leeuwenburgh
 */
public class ExchangeMailListener implements IPullingListener, INamedObject,
		HasPhysicalDestination {
	protected Logger log = LogUtil.getLogger(this);

	private String name;
	private String inputFolder;
	private String outputFolder;
	private String authAlias;
	private String userName;
	private String password;
	private String mailAddress;
	private String filter;
	private String url;
	private String storeEmailAsStreamInSessionKey;

	private ExchangeService exchangeService;
	private Folder folderIn;
	private Folder folderOut;

	public void afterMessageProcessed(PipeLineResult processResult,
			Object rawMessage, Map context) throws ListenerException {
		Item item = (Item) rawMessage;
		try {
			if (folderOut != null) {
				item.move(folderOut.getId());
				log.debug("moved item [" + item.getId() + "] from folder ["
						+ folderIn.getDisplayName() + "] to folder ["
						+ folderOut.getDisplayName() + "]");
			} else {
				item.delete(DeleteMode.MoveToDeletedItems);
				log.debug("deleted item [" + item.getId() + "] from folder ["
						+ folderIn.getDisplayName() + "]");
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public void close() throws ListenerException {
		// TODO Auto-generated method stub
	}

	public void configure() throws ConfigurationException {
		try {
			exchangeService = new ExchangeService(
					ExchangeVersion.Exchange2010_SP2);
			CredentialFactory cf = new CredentialFactory(getAuthAlias(),
					getUserName(), getPassword());
			ExchangeCredentials credentials = new WebCredentials(
					cf.getUsername(), cf.getPassword());
			exchangeService.setCredentials(credentials);
			if (StringUtils.isNotEmpty(getMailAddress())) {
				exchangeService.autodiscoverUrl(getMailAddress());
			} else {
				exchangeService.setUrl(new URI(getUrl()));
			}

			FolderId inboxId;
			if (StringUtils.isNotEmpty(getMailAddress())) {
				Mailbox mailbox = new Mailbox(getMailAddress());
				inboxId = new FolderId(WellKnownFolderName.Inbox, mailbox);
			} else {
				inboxId = new FolderId(WellKnownFolderName.Inbox);
			}

			FindFoldersResults findFoldersResultsIn;
			FolderView folderViewIn = new FolderView(10);
			if (StringUtils.isNotEmpty(getInputFolder())) {
				SearchFilter searchFilterIn = new SearchFilter.IsEqualTo(
						FolderSchema.DisplayName, getInputFolder());
				findFoldersResultsIn = exchangeService.findFolders(inboxId,
						searchFilterIn, folderViewIn);
				if (findFoldersResultsIn.getTotalCount() == 0) {
					throw new ConfigurationException(
							"no (in) folder found with name ["
									+ getInputFolder() + "]");
				} else if (findFoldersResultsIn.getTotalCount() > 1) {
					throw new ConfigurationException(
							"multiple (in) folders found with name ["
									+ getInputFolder() + "]");
				}
			} else {
				findFoldersResultsIn = exchangeService.findFolders(inboxId,
						folderViewIn);
			}
			folderIn = findFoldersResultsIn.getFolders().get(0);

			if (StringUtils.isNotEmpty(getFilter())) {
				if (!getFilter().equalsIgnoreCase("NDR")) {
					throw new ConfigurationException(
							"illegal value for filter [" + getFilter()
									+ "], must be 'NDR' or empty");
				}
			}

			if (StringUtils.isNotEmpty(getOutputFolder())) {
				SearchFilter searchFilterOut = new SearchFilter.IsEqualTo(
						FolderSchema.DisplayName, getOutputFolder());
				FolderView folderViewOut = new FolderView(10);
				FindFoldersResults findFoldersResultsOut = exchangeService
						.findFolders(inboxId, searchFilterOut, folderViewOut);
				if (findFoldersResultsOut.getTotalCount() == 0) {
					throw new ConfigurationException(
							"no (out) folder found with name ["
									+ getOutputFolder() + "]");
				} else if (findFoldersResultsOut.getTotalCount() > 1) {
					throw new ConfigurationException(
							"multiple (out) folders found with name ["
									+ getOutputFolder() + "]");
				}
				folderOut = findFoldersResultsOut.getFolders().get(0);
			}
		} catch (Exception e) {
			throw new ConfigurationException(e);
		}
	}

	public String getIdFromRawMessage(Object rawMessage, Map threadContext)
			throws ListenerException {
		Item item = (Item) rawMessage;
		try {
			return "" + item.getId();
		} catch (ServiceLocalException e) {
			throw new ListenerException(e);
		}
	}

	public String getStringFromRawMessage(Object rawMessage, Map threadContext)
			throws ListenerException {
		Item item = (Item) rawMessage;
		try {
			PropertySet ps = new PropertySet(
					EmailMessageSchema.DateTimeReceived,
					EmailMessageSchema.From, EmailMessageSchema.Subject,
					EmailMessageSchema.Body, EmailMessageSchema.DateTimeSent);
			EmailMessage emailMessage = EmailMessage.bind(exchangeService,
					item.getId(), ps);
			XmlBuilder emailXml = new XmlBuilder("email");
			addEmailInfo(emailMessage, emailXml);

			if (StringUtils.isNotEmpty(getStoreEmailAsStreamInSessionKey())) {
				emailMessage.load(new PropertySet(ItemSchema.MimeContent));
				MimeContent mc = emailMessage.getMimeContent();
				ByteArrayInputStream bis = new ByteArrayInputStream(
						mc.getContent());
				threadContext.put(getStoreEmailAsStreamInSessionKey(), bis);
			}

			return emailXml.toXML();
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	private void addEmailInfo(EmailMessage emailMessage, XmlBuilder emailXml)
			throws Exception {
		XmlBuilder recipientsXml = new XmlBuilder("recipients");
		EmailAddressCollection eacTo = emailMessage.getToRecipients();
		if (eacTo != null) {
			for (Iterator it = eacTo.iterator(); it.hasNext();) {
				XmlBuilder recipientXml = new XmlBuilder("recipient");
				EmailAddress ea = (EmailAddress) it.next();
				recipientXml.addAttribute("type", "to");
				recipientXml.setValue(ea.getAddress());
				recipientsXml.addSubElement(recipientXml);
			}
		}
		EmailAddressCollection eacCc = emailMessage.getCcRecipients();
		if (eacCc != null) {
			for (Iterator it = eacCc.iterator(); it.hasNext();) {
				XmlBuilder recipientXml = new XmlBuilder("recipient");
				EmailAddress ea = (EmailAddress) it.next();
				recipientXml.addAttribute("type", "cc");
				recipientXml.setValue(ea.getAddress());
				recipientsXml.addSubElement(recipientXml);
			}
		}
		EmailAddressCollection eacBcc = emailMessage.getBccRecipients();
		if (eacBcc != null) {
			for (Iterator it = eacBcc.iterator(); it.hasNext();) {
				XmlBuilder recipientXml = new XmlBuilder("recipient");
				EmailAddress ea = (EmailAddress) it.next();
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
		messageXml.setCdataValue(MessageBody
				.getStringFromMessageBody(emailMessage.getBody()));
		emailXml.addSubElement(messageXml);
		XmlBuilder attachmentsXml = new XmlBuilder("attachments");
		AttachmentCollection ac = emailMessage.getAttachments();
		if (ac != null) {
			for (Iterator it = ac.iterator(); it.hasNext();) {
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
		emailXml.addSubElement(attachmentsXml);
		XmlBuilder headersXml = new XmlBuilder("headers");
		InternetMessageHeaderCollection imhc = null;
		try {
			imhc = emailMessage.getInternetMessageHeaders();
		} catch (Exception e) {
			log.info("error occurred during getting internet message headers: "
					+ e.getMessage());
		}
		if (imhc != null) {
			for (Iterator it = imhc.iterator(); it.hasNext();) {
				XmlBuilder headerXml = new XmlBuilder("header");
				InternetMessageHeader imh = (InternetMessageHeader) it.next();
				headerXml.addAttribute("name", imh.getName());
				headerXml.setCdataValue(imh.getValue());
				headersXml.addSubElement(headerXml);
			}
		}
		emailXml.addSubElement(headersXml);
		SimpleDateFormat sdf = new SimpleDateFormat(
				"yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		Date dateTimeSend = emailMessage.getDateTimeSent();
		XmlBuilder dateTimeSentXml = new XmlBuilder("dateTimeSent");
		dateTimeSentXml.setValue(sdf.format(dateTimeSend));
		emailXml.addSubElement(dateTimeSentXml);
		Date dateTimeReceived = emailMessage.getDateTimeReceived();
		XmlBuilder dateTimeReceivedXml = new XmlBuilder("dateTimeReceived");
		dateTimeReceivedXml.setValue(sdf.format(dateTimeReceived));
		emailXml.addSubElement(dateTimeReceivedXml);
	}

	public void open() throws ListenerException {
		// TODO Auto-generated method stub
	}

	public String getPhysicalDestinationName() {
		if (exchangeService != null) {
			return "url [" + exchangeService.getUrl() + "]";
		}
		return null;
	}

	public void closeThread(Map threadContext) throws ListenerException {
		// TODO Auto-generated method stub
	}

	public Object getRawMessage(Map threadContext) throws ListenerException {
		try {
			ItemView view = new ItemView(1);
			view.getOrderBy().add(ItemSchema.DateTimeReceived,
					SortDirection.Ascending);
			FindItemsResults<Item> findResults;
			if ("NDR".equalsIgnoreCase(getFilter())) {
				SearchFilter searchFilterBounce = new SearchFilter.IsEqualTo(
						ItemSchema.ItemClass, "REPORT.IPM.Note.NDR");
				findResults = exchangeService.findItems(folderIn.getId(),
						searchFilterBounce, view);
			} else {
				findResults = exchangeService.findItems(folderIn.getId(), view);
			}
			if (findResults.getTotalCount() == 0) {
				return null;
			} else {
				return findResults.getItems().get(0);
			}
		} catch (Exception e) {
			throw new ListenerException(e);
		}
	}

	public Map openThread() throws ListenerException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	public String getInputFolder() {
		return inputFolder;
	}

	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	public String getOutputFolder() {
		return outputFolder;
	}

	public void setAuthAlias(String string) {
		authAlias = string;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setUserName(String string) {
		userName = string;
	}

	public String getUserName() {
		return userName;
	}

	public void setPassword(String string) {
		password = string;
	}

	public String getPassword() {
		return password;
	}

	public void setMailAddress(String string) {
		mailAddress = string;
	}

	public String getMailAddress() {
		return mailAddress;
	}

	public void setFilter(String string) {
		filter = string;
	}

	public String getFilter() {
		return filter;
	}

	public void setUrl(String string) {
		url = string;
	}

	public String getUrl() {
		return url;
	}

	public void setStoreEmailAsStreamInSessionKey(String string) {
		storeEmailAsStreamInSessionKey = string;
	}

	public String getStoreEmailAsStreamInSessionKey() {
		return storeEmailAsStreamInSessionKey;
	}
}