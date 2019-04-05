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
package nl.nn.adapterframework.filesystem;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;

import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.WebProxy;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.ExchangeCredentials;
import microsoft.exchange.webservices.data.credential.WebCredentials;
import microsoft.exchange.webservices.data.credential.WebProxyCredentials;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import microsoft.exchange.webservices.data.property.complex.AttachmentCollection;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.EmailAddressCollection;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeader;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeaderCollection;
import microsoft.exchange.webservices.data.property.complex.ItemAttachment;
import microsoft.exchange.webservices.data.property.complex.ItemId;
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
import nl.nn.adapterframework.filesystem.ExchangeWebServicesSender.RedirectionUrlCallback;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;

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
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMailAddress(String) mailAddress}</td><td>mail address (also used for auto discovery)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>(only used when mailAddress is empty) url of the service</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to exchange mail server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>username used in authentication to exchange mail server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFolder(String) inputFolder}</td><td>folder (subfolder of inbox) to look for mails. If empty, the inbox folder is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilter(String) filter}</td><td>If empty, all mails are retrieved. If 'NDR' only Non-Delivery Report mails ('bounces') are retrieved</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setSimple(boolean) simple}</td><td>when set to <code>true</code>, the xml string passed to the pipeline contains minimum information about the mail (to save memory)</td><td>false</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel, after {@link ExchangeMailListener} by Peter Leeuwenburgh
 */
public class ExchangeFileSystem implements IBasicFileSystem<Item>, HasPhysicalDestination {
	protected Logger log = LogUtil.getLogger(this);

	private String mailAddress;
	private boolean validateAllRedirectUrls=true;
	private String url;
	private String authAlias;
	private String username;
	private String password;
	private String inputFolder;
	private String filter;
	private boolean simple = false;
	private boolean readMimeContents=false;
	private int maxNumberOfMessagesToList=10;

	private String proxyHost = null;
	private int proxyPort = 8080;
	private String proxyAuthAlias = null;
	private String proxyUsername = null;
	private String proxyPassword = null;
	private String proxyDomain = null;

	private ExchangeService exchangeService;
	private FolderId folderId;


	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getFilter())) {
			if (!getFilter().equalsIgnoreCase("NDR")) {
				throw new ConfigurationException("illegal value for filter [" + getFilter()	+ "], must be 'NDR' or empty");
			}
		}
	}
	
	
	@Override
	public void open() throws FileSystemException {
		try {
			exchangeService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
			String usernameToUse=StringUtils.isNotEmpty(getUsername())?getUsername():getMailAddress();
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), usernameToUse, getPassword());
			ExchangeCredentials credentials = new WebCredentials(cf.getUsername(), cf.getPassword());
			exchangeService.setCredentials(credentials);

			if (StringUtils.isNotEmpty(getProxyHost()) && (StringUtils.isNotEmpty(getProxyAuthAlias()) || StringUtils.isNotEmpty(getProxyUsername()) || StringUtils.isNotEmpty(getProxyPassword()))) {
				CredentialFactory proxyCf = new CredentialFactory(getProxyAuthAlias(), getProxyUsername(), getProxyPassword());
				WebProxyCredentials webProxyCredentials = new WebProxyCredentials(proxyCf.getUsername(), proxyCf.getPassword(), getProxyDomain());
				WebProxy webProxy = new WebProxy(getProxyHost(), getProxyPort(), webProxyCredentials);
				exchangeService.setWebProxy(webProxy);
			}

			RedirectionUrlCallback redirectionUrlCallback = new RedirectionUrlCallback() {

				@Override
				public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
					if (isValidateAllRedirectUrls()) {
						log.debug("validated redirection url ["+redirectionUrl+"]");
						return true;
					}
					log.debug("did not validate redirection url ["+redirectionUrl+"]");
					return super.autodiscoverRedirectionUrlValidationCallback(redirectionUrl);
				}
				
			};

			if (StringUtils.isNotEmpty(getMailAddress())) {
				log.debug("performing autodiscovery for ["+getMailAddress()+"]");
				exchangeService.autodiscoverUrl(getMailAddress(),redirectionUrlCallback);
			} else {
				exchangeService.setUrl(new URI(getUrl()));
			}

			log.debug("searching inbox");
			FolderId inboxId;
			if (StringUtils.isNotEmpty(getMailAddress())) {
				Mailbox mailbox = new Mailbox(getMailAddress());
				inboxId = new FolderId(WellKnownFolderName.Inbox, mailbox);
			} else {
				inboxId = new FolderId(WellKnownFolderName.Inbox);
			}
			log.debug("determined inbox ["+inboxId+"]");
	
			FindFoldersResults findFoldersResultsIn;
			FolderView folderViewIn = new FolderView(10);
			if (StringUtils.isNotEmpty(getInputFolder())) {
				SearchFilter searchFilterIn = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, getInputFolder());
				findFoldersResultsIn = exchangeService.findFolders(inboxId, searchFilterIn, folderViewIn);
				if (findFoldersResultsIn.getTotalCount() == 0) {
					throw new ConfigurationException("no (in) folder found with name ["	+ getInputFolder() + "]");
				} else if (findFoldersResultsIn.getTotalCount() > 1) {
					throw new ConfigurationException("multiple (in) folders found with name ["+ getInputFolder() + "]");
				}
			} else {
				findFoldersResultsIn = exchangeService.findFolders(inboxId,	folderViewIn);
			}
			if (findFoldersResultsIn.getFolders().isEmpty()) {
				folderId=inboxId;
			} else {
				folderId =findFoldersResultsIn.getFolders().get(0).getId();
			}
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}
	

	@Override
	public void close() throws FileSystemException {
		exchangeService.close();
	}
	
	
	@Override
	public Item toFile(String filename) throws FileSystemException {
		try {
			ItemId itemId = ItemId.getItemIdFromString(filename);
			Item item = Item.bind(exchangeService,itemId);
			return item;
		} catch (Exception e) {
			throw new FileSystemException("Cannot convert filename ["+filename+"] into an ItemId");
		}
	}

	@Override
	public boolean exists(Item f) throws FileSystemException {
		try {
			ItemView view = new ItemView(1);
			view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Ascending);
			SearchFilter searchFilter =  new SearchFilter.IsEqualTo(ItemSchema.Id, f.getId().toString());
			FindItemsResults<Item> findResults;
			findResults = exchangeService.findItems(folderId,searchFilter, view);
			return findResults.getTotalCount()!=0;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Iterator<Item> listFiles(String folder) throws FileSystemException {
		try {
			ItemView view = new ItemView(getMaxNumberOfMessagesToList());
			view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Ascending);
			FindItemsResults<Item> findResults;
			if ("NDR".equalsIgnoreCase(getFilter())) {
				SearchFilter searchFilterBounce = new SearchFilter.IsEqualTo(ItemSchema.ItemClass, "REPORT.IPM.Note.NDR");
				findResults = exchangeService.findItems(folderId,searchFilterBounce, view);
			} else {
				findResults = exchangeService.findItems(folderId, view);
			}
			if (findResults.getTotalCount() == 0) {
				return null;
			} else {
				return findResults.getItems().iterator();
			}
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public boolean isFolder(Item f) throws FileSystemException {
		return false;
	}

	
	@Override
	public InputStream readFile(Item f) throws FileSystemException, IOException {
		EmailMessage emailMessage;
		PropertySet ps = new PropertySet(EmailMessageSchema.Subject);
//		ps = new PropertySet(EmailMessageSchema.DateTimeReceived,
//				EmailMessageSchema.From, EmailMessageSchema.Subject,
//				EmailMessageSchema.Body,
//				EmailMessageSchema.DateTimeSent);
//		
		try {
			emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			if (isReadMimeContents()) {
				emailMessage.load(new PropertySet(ItemSchema.MimeContent));
				MimeContent mc = emailMessage.getMimeContent();
				ByteArrayInputStream bis = new ByteArrayInputStream(mc.getContent());
				return bis;
			}
			emailMessage.load(new PropertySet(ItemSchema.Body));
			String body =MessageBody.getStringFromMessageBody(emailMessage.getBody());
			ByteArrayInputStream bis = new ByteArrayInputStream(body.getBytes(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING));
			return bis;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}
	
	@Override
	public void deleteFile(Item f) throws FileSystemException {
		 try {
			f.delete(DeleteMode.MoveToDeletedItems);
		} catch (Exception e) {
			throw new FileSystemException("Could not delete",e);
		}
	}
	@Override
	public Item moveFile(Item f, String destinationFolder, boolean createFolder) throws FileSystemException {
		throw new NotImplementedException("Must implement move");
	}

	@Override
	public long getFileSize(Item f) throws FileSystemException {
		try {
			return f.getSize();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine size",e);
		}
	}
	@Override
	public String getName(Item f) {
		try {
			return f.getId().toString();
		} catch (ServiceLocalException e) {
			throw new RuntimeException("Could not determine name",e);
		}
	}
	@Override
	public String getCanonicalName(Item f) throws FileSystemException {
		try {
			return f.getId().getUniqueId();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine name",e);
		}
	}
	@Override
	public Date getModificationTime(Item f) throws FileSystemException {
		try {
			return f.getLastModifiedTime();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine modification time",e);
		}
	}
	
	private List<String> makeListOfAdresses(EmailAddressCollection addresses) {
		List<String> adressList = new LinkedList<String>();
		for (EmailAddress emailAddress : addresses) {
			adressList.add(emailAddress.getAddress());
		}	
		return adressList;
	}
	
	@Override
	public Map<String, Object> getAdditionalFileProperties(Item f) throws FileSystemException {
		EmailMessage emailMessage;
//		PropertySet ps = new PropertySet(EmailMessageSchema.DateTimeReceived,
//				EmailMessageSchema.From, EmailMessageSchema.Subject,
//				EmailMessageSchema.Body,
//				EmailMessageSchema.DateTimeSent);
		PropertySet ps=PropertySet.FirstClassProperties;
		try {
			emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			Map<String, Object> result=new LinkedHashMap<String,Object>();
			result.put("mailId", emailMessage.getInternetMessageId());
			result.put("toRecipients", makeListOfAdresses(emailMessage.getToRecipients()));
			result.put("ccRecipients", makeListOfAdresses(emailMessage.getCcRecipients()));
			result.put("bccRecipients", makeListOfAdresses(emailMessage.getBccRecipients()));
			result.put("from", emailMessage.getFrom().getAddress());
			result.put("subject", emailMessage.getSubject());
			result.put("dateTimeSent", emailMessage.getDateTimeSent());
			result.put("dateTimeReceived", emailMessage.getDateTimeReceived());
			Map<String,String> headers = new LinkedHashMap<String,String>();
			for(InternetMessageHeader internetMessageHeader : emailMessage.getInternetMessageHeaders()) {
				headers.put(internetMessageHeader.getName(), internetMessageHeader.getValue());
			}
			result.put("headers", headers);
			
//			for (Entry<PropertyDefinition, Object> entry:emailMessage.getPropertyBag().getProperties().entrySet()) {
//				result.put(entry.getKey().getName(), entry.getValue());
//			}
			return result;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

//	
//	
//	@Override
//	public OutputStream createFile(Item f) throws FileSystemException, IOException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public OutputStream appendFile(Item f) throws FileSystemException, IOException {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public void renameTo(Item f, String destination) throws FileSystemException {
//		throw new NotImplementedException("Exchange does not support rename");
//	}

	/*
	 * addEmailInfo copied from ExchangListener, to have ideas what could be useful for getAdditionalFileProperties()
	 */
	private void addEmailInfo(EmailMessage emailMessage, XmlBuilder emailXml) throws Exception {
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


	@Override
	public String getPhysicalDestinationName() {
		if (exchangeService != null) {
			return "url [" + exchangeService.getUrl() + "]";
		}
		return null;
	}



	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}
	public String getMailAddress() {
		return mailAddress;
	}

	public boolean isValidateAllRedirectUrls() {
		return validateAllRedirectUrls;
	}
	public void setValidateAllRedirectUrls(boolean validateAllRedirectUrls) {
		this.validateAllRedirectUrls = validateAllRedirectUrls;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}
	
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}
	public String getInputFolder() {
		return inputFolder;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getFilter() {
		return filter;
	}

	public void setSimple(boolean b) {
		simple = b;
	}
	public boolean isSimple() {
		return simple;
	}


	public boolean isReadMimeContents() {
		return readMimeContents;
	}


	public void setReadMimeContents(boolean readMimeContents) {
		this.readMimeContents = readMimeContents;
	}


	public int getMaxNumberOfMessagesToList() {
		return maxNumberOfMessagesToList;
	}


	public void setMaxNumberOfMessagesToList(int maxNumberOfMessagesToList) {
		this.maxNumberOfMessagesToList = maxNumberOfMessagesToList;
	}

	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public String getProxyHost() {
		return proxyHost;
	}

	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public int getProxyPort() {
		return proxyPort;
	}

	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}
	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	public String getProxyUsername() {
		return proxyUsername;
	}

	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	public String getProxyPassword() {
		return proxyPassword;
	}

	public void setProxyDomain(String proxyDomain) {
		this.proxyDomain = proxyDomain;
	}
	public String getProxyDomain() {
		return proxyDomain;
	}
}
