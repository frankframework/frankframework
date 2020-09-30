/*
   Copyright 2019, 2020 WeAreFrank!

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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.Logger;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.WebProxy;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceVersionException;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.schema.EmailMessageSchema;
import microsoft.exchange.webservices.data.core.service.schema.FolderSchema;
import microsoft.exchange.webservices.data.core.service.schema.ItemSchema;
import microsoft.exchange.webservices.data.credential.WebProxyCredentials;
import microsoft.exchange.webservices.data.property.complex.Attachment;
import microsoft.exchange.webservices.data.property.complex.AttachmentCollection;
import microsoft.exchange.webservices.data.property.complex.EmailAddress;
import microsoft.exchange.webservices.data.property.complex.EmailAddressCollection;
import microsoft.exchange.webservices.data.property.complex.FileAttachment;
import microsoft.exchange.webservices.data.property.complex.FolderId;
import microsoft.exchange.webservices.data.property.complex.InternetMessageHeader;
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
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.StreamUtil;

/**
 * Implementation of a {@link IBasicFileSystem} of an Exchange Mail Inbox.
 * <b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMailAddress(String) mailAddress}</td><td>mail address (also used for auto discovery)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>(only used when mailAddress is empty) url of the service</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to exchange mail server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setBaseFolder(String) basefolder}</td><td>folder (subfolder of root or of inbox) to look for mails. If empty, the inbox folder is used</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilter(String) filter}</td><td>If empty, all mails are retrieved. If 'NDR' only Non-Delivery Report mails ('bounces') are retrieved</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * 
 * To obtain an accessToken:
 * <ol>
 * 	<li>follow the steps in {@link "https://docs.microsoft.com/en-us/exchange/client-developer/exchange-web-services/how-to-authenticate-an-ews-application-by-using-oauth"}</li>
 *  <li>request an Authorization-Code for scope https://outlook.office.com/EWS.AccessAsUser.All</li>
 *  <li>exchange the Authorization-Code for an accessToken</li>
 *  <li>configure the accessToken directly, or as the password of a JAAS entry referred to by authAlias</li>
 * </ol>
 * 
 * 
 * @author Gerrit van Brakel, after {@link ExchangeMailListener} by Peter Leeuwenburgh
 */
public class ExchangeFileSystem implements IWithAttachments<Item,Attachment> {
	protected Logger log = LogUtil.getLogger(this);

	private String mailAddress;
	private boolean validateAllRedirectUrls=true;
	private String url;
	private String accessToken;
	private String authAlias;
	private String basefolder;
	private String filter;
	private boolean readMimeContents=false;
	private int maxNumberOfMessagesToList=10;

	private String proxyHost = null;
	private int proxyPort = 8080;
	private String proxyUsername = null;
	private String proxyPassword = null;
	private String proxyAuthAlias = null;
	private String proxyDomain = null;

	private ExchangeService exchangeService;
	private FolderId basefolderId;


	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getFilter())) {
			if (!getFilter().equalsIgnoreCase("NDR")) {
				throw new ConfigurationException("illegal value for filter [" + getFilter()	+ "], must be 'NDR' or empty");
			}
		}
		if (StringUtils.isEmpty(getUrl()) && StringUtils.isEmpty(getMailAddress())) {
			throw new ConfigurationException("either url or mailAddress needs to be specified");
		}
	}
	
	@Override
	public void open() throws FileSystemException {
		try {
			exchangeService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);
			CredentialFactory cf = new CredentialFactory(getAuthAlias(), null, getAccessToken());
			exchangeService.getHttpHeaders().put("Authorization", "Bearer "+cf.getPassword());

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

			if (StringUtils.isEmpty(getUrl())) {
				log.debug("performing autodiscovery for ["+getMailAddress()+"]");
				exchangeService.autodiscoverUrl(getMailAddress(),redirectionUrlCallback);
			} else {
				exchangeService.setUrl(new URI(getUrl()));
			}
			log.debug("using url ["+exchangeService.getUrl()+"]");
			
			log.debug("searching inbox");
			FolderId inboxId;
			if (StringUtils.isNotEmpty(getMailAddress())) {
				Mailbox mailbox = new Mailbox(getMailAddress());
				inboxId = new FolderId(WellKnownFolderName.Inbox, mailbox);
			} else {
				inboxId = new FolderId(WellKnownFolderName.Inbox);
			}
			log.debug("determined inbox ["+inboxId+"] foldername ["+inboxId.getFolderName()+"]");
	
			if (StringUtils.isNotEmpty(getBaseFolder())) {
				try {
					basefolderId=findFolder(inboxId,getBaseFolder());
				} catch (Exception e) {
					throw new FileSystemException("Could not find baseFolder ["+getBaseFolder()+"] as subfolder of ["+inboxId.getFolderName()+"]", e);
				}	
				if (basefolderId==null) {
					log.debug("Could not get baseFolder ["+getBaseFolder()+"] as subfolder of ["+inboxId.getFolderName()+"]");
					basefolderId=findFolder(null,getBaseFolder());
				}
				if (basefolderId==null) {
					throw new FileSystemException("Could not find baseFolder ["+getBaseFolder()+"]");
				}
			} else {
				basefolderId=inboxId;
			}
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}
	
	public FolderId findFolder(String folderName) throws Exception {
		return findFolder(basefolderId, folderName);
	}
	
	public FolderId findFolder(FolderId baseFolderId, String folderName) throws Exception {
		FindFoldersResults findFoldersResultsIn;
		FolderId result;
		FolderView folderViewIn = new FolderView(10);
		if (StringUtils.isNotEmpty(folderName)) {
			log.debug("searching folder ["+folderName+"]");
			SearchFilter searchFilterIn = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName);
			if (baseFolderId==null) {
				findFoldersResultsIn = exchangeService.findFolders(WellKnownFolderName.MsgFolderRoot, searchFilterIn, folderViewIn);
			} else {
				findFoldersResultsIn = exchangeService.findFolders(baseFolderId, searchFilterIn, folderViewIn);
			}
			if (findFoldersResultsIn.getTotalCount() == 0) {
				if(log.isDebugEnabled()) log.debug("no folder found with name [" + folderName + "] in basefolder ["+baseFolderId+"]");
				return null;
			} 
			if (findFoldersResultsIn.getTotalCount() > 1) {
				if (log.isDebugEnabled()) {
					for (Folder folder:findFoldersResultsIn.getFolders()) {
						log.debug("found folder ["+folder.getDisplayName()+"]");
					}
				}
				throw new ConfigurationException("multiple folders found with name ["+ folderName + "]");
			}
		} else {
			//findFoldersResultsIn = exchangeService.findFolders(baseFolderId, folderViewIn);
			return baseFolderId;
		}
		if (findFoldersResultsIn.getFolders().isEmpty()) {
			result=baseFolderId;
		} else {
			result=findFoldersResultsIn.getFolders().get(0).getId();
		}
		return result;
	}

	@Override
	public void close() throws FileSystemException {
		if (exchangeService!=null) {
			exchangeService.close();
		}
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
	public Item toFile(String folder, String filename) throws FileSystemException {
		throw new NotImplementedException("Cannot make item for ["+filename+"] file in Exchange folder ["+folder+"]");
	}


	@Override
	public boolean exists(Item f) throws FileSystemException {
		try {
			ItemView view = new ItemView(1);
			view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Ascending);
			SearchFilter searchFilter =  new SearchFilter.IsEqualTo(ItemSchema.Id, f.getId().toString());
			FindItemsResults<Item> findResults;
			findResults = exchangeService.findItems(basefolderId,searchFilter, view);
			return findResults.getTotalCount()!=0;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	public Item extractNestedItem(Item item) throws Exception {
		Iterator<Attachment> attachments = listAttachments(item);
		if (attachments!=null) {
			while (attachments.hasNext()) {
				Attachment attachment=attachments.next();
				if (attachment instanceof ItemAttachment) {
					ItemAttachment itemAttachment = (ItemAttachment)attachment;
					itemAttachment.load();
					return itemAttachment.getItem();
				}
			}
		}
		return null;
	}
	
	
	@Override
	public Iterator<Item> listFiles(String folder) throws FileSystemException {
		try {
			FolderId folderId = findFolder(basefolderId,folder);
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
			throw new FileSystemException("Cannot list messages in folder ["+folder+"]", e);
		}
	}

	
	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		FolderId folderId;
		try {
			folderId = findFolder(folder);
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
		return folderId!=null;
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
		EmailMessage emailMessage;
		try {
			emailMessage = EmailMessage.bind(exchangeService, f.getId());
			emailMessage = (EmailMessage) emailMessage.move(getFolderIdByFolderName(destinationFolder));
			return emailMessage;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Item copyFile(Item f, String destinationFolder, boolean createFolder) throws FileSystemException {
		EmailMessage emailMessage;
		try {
			emailMessage = EmailMessage.bind(exchangeService, f.getId());
			emailMessage = (EmailMessage) emailMessage.copy(getFolderIdByFolderName(destinationFolder));
			return emailMessage;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
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
			throw new RuntimeException("Could not determine Name",e);
		}
	}
	@Override
	public String getCanonicalName(Item f) throws FileSystemException {
		try {
			return f.getId().getUniqueId();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine CanonicalName",e);
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

	@Override
	public Iterator<Attachment> listAttachments(Item f) throws FileSystemException {
		List<Attachment> result=new LinkedList<Attachment>();
		try {
			EmailMessage emailMessage;
			PropertySet ps = new PropertySet(EmailMessageSchema.Attachments);
			emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			AttachmentCollection attachmentCollection = emailMessage.getAttachments();
			emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			for (Attachment attachment : attachmentCollection) {
				result.add(attachment);
			}
			return result.iterator();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("cannot read attachments",e);
		} catch (Exception e) {
			throw new FileSystemException("cannot read attachments",e);
		}
	}


	@Override
	public String getAttachmentName(Attachment a) {
		return a.getName();
	}

	@Override
	public FileAttachment getAttachmentByName(Item f, String name) throws FileSystemException {
		try {
			EmailMessage emailMessage;
			PropertySet ps = new PropertySet(EmailMessageSchema.Attachments);
			emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			AttachmentCollection attachmentCollection = emailMessage.getAttachments();
			emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			for (Attachment attachment : attachmentCollection) {
				FileAttachment fileAttachment = (FileAttachment) attachment;
				if (name.equals(attachment.getName())) {
					return fileAttachment;
				}
			}
			return null;
		} catch (ServiceLocalException e) {
			throw new FileSystemException("cannot read attachments",e);
		} catch (Exception e) {
			throw new FileSystemException("cannot read attachments",e);
		}
	}



	@Override
	public InputStream readAttachment(Attachment a) throws FileSystemException, IOException {
		try {
			a.load();
		} catch (Exception e) {
			throw new FileSystemException("Cannot load attachment",e);
		}
		byte[] content = null;
		if (a instanceof FileAttachment) {
			content=((FileAttachment)a).getContent(); // TODO: should do streaming, instead of via byte array
		}
		if (a instanceof ItemAttachment) {
			ItemAttachment itemAttachment=(ItemAttachment)a;
			Item attachmentItem = itemAttachment.getItem();
			return readFile(attachmentItem);
		}
		if (content==null) {
			log.warn("content of attachment is null");
			content = new byte[0];
		}
		InputStream binaryInputStream = new ByteArrayInputStream(content);
		return binaryInputStream;
	}


	@Override
	public long getAttachmentSize(Attachment a) throws FileSystemException {
		try {
			return a.getSize();
		} catch (ServiceVersionException e) {
			throw new FileSystemException(e);
		}
	}


	@Override
	public String getAttachmentContentType(Attachment a) throws FileSystemException {
		return a.getContentType();
	}

	@Override
	public String getAttachmentFileName(Attachment a) throws FileSystemException {
		if (a instanceof FileAttachment) {
			return ((FileAttachment)a).getFileName();
		}
		return null;
	}


	@Override
	public Map<String, Object> getAdditionalAttachmentProperties(Attachment a) throws FileSystemException {
		Map<String, Object> result = new LinkedHashMap<String,Object>();
		result.put("id", a.getId());
		result.put("contentId", a.getContentId());
		result.put("contentLocation", a.getContentLocation());
		return result;
	}

	
	
	public FolderId getFolderIdByFolderName(String folderName) throws Exception{
		FindFoldersResults findResults;
		findResults = exchangeService.findFolders(basefolderId, new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName), new FolderView(Integer.MAX_VALUE));
		if (log.isDebugEnabled()) {
			log.debug("amount of folders with name: " + folderName + " = " + findResults.getTotalCount());
			log.debug("found folder with name: " + findResults.getFolders().get(0).getDisplayName());
		}
		FolderId folderId = findResults.getFolders().get(0).getId();
		return folderId;
	}

	@Override
	public void createFolder(String folderName) throws FileSystemException {
		try {
			Folder folder = new Folder(exchangeService);
			folder.setDisplayName(folderName);
			folder.save(new FolderId(basefolderId.getUniqueId()));
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}


	@Override
	public void removeFolder(String folderName) throws FileSystemException {
		try {
			FolderId folderId = getFolderIdByFolderName(folderName);
			Folder folder = Folder.bind(exchangeService, folderId);
			folder.delete(DeleteMode.HardDelete);
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}




	@Override
	public String getPhysicalDestinationName() {
		if (exchangeService != null) {
			return "url [" + (exchangeService == null ? "" : exchangeService.getUrl()) + "] mailAddress [" + (getMailAddress() == null ? "" : getMailAddress()) + "]";
		}
		return null;
	}


	@IbisDoc({"1", "The mail address of the mailbox connected to (also used for auto discovery)", ""})
	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}
	public String getMailAddress() {
		return mailAddress;
	}

	@IbisDoc({"2", "When <code>true</code>, all redirect uris are accepted when connecting to the server", "true"})
	public boolean isValidateAllRedirectUrls() {
		return validateAllRedirectUrls;
	}
	public void setValidateAllRedirectUrls(boolean validateAllRedirectUrls) {
		this.validateAllRedirectUrls = validateAllRedirectUrls;
	}

	@IbisDoc({"3", "Url of the Exchange server. Set to e.g. https://outlook.office365.com/EWS/Exchange.asmx to speed up start up, leave empty to use autodiscovery", ""})
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUrl() {
		return url;
	}

	@IbisDoc({"4", "AccessToken for authentication to Exchange mail server", ""})
	public void setAccessToken(String accessToken) {
		this.accessToken = accessToken;
	}
	public String getAccessToken() {
		return accessToken;
	}


	@IbisDoc({"5", "Alias used to obtain accessToken for authentication to Exchange mail server", ""})
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}


	@IbisDoc({"7", "Folder (subfolder of root or of inbox) to look for mails. If empty, the inbox folder is used", ""})
	public void setBaseFolder(String basefolder) {
		this.basefolder = basefolder;
	}
	public String getBaseFolder() {
		return basefolder;
	}

	@IbisDoc({"8", "If empty, all mails are retrieved. If set to <code>NDR</code> only Non-Delivery Report mails ('bounces') are retrieved", ""})
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getFilter() {
		return filter;
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

	@IbisDoc({"9", "proxy host", ""})
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public String getProxyHost() {
		return proxyHost;
	}

	@IbisDoc({"10", "proxy port", ""})
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public int getProxyPort() {
		return proxyPort;
	}

	@IbisDoc({"11", "proxy username", ""})
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	public String getProxyUsername() {
		return proxyUsername;
	}

	@IbisDoc({"12", "proxy password", ""})
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	public String getProxyPassword() {
		return proxyPassword;
	}

	@IbisDoc({"12", "proxy authAlias", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}
	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	@IbisDoc({"13", "proxy domain", ""})
	public void setProxyDomain(String proxyDomain) {
		this.proxyDomain = proxyDomain;
	}
	public String getProxyDomain() {
		return proxyDomain;
	}
	
	static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
		
		@Override
		public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
			return redirectionUrl.toLowerCase().startsWith("https://"); //TODO: provide better test on how to trust this url
		}
	}

	public ExchangeService getExchangeService() {
		return exchangeService;
	}

}
