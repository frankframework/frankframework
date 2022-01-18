/*
   Copyright 2019-2022 WeAreFrank!

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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.WebProxy;
import microsoft.exchange.webservices.data.core.enumeration.misc.ExchangeVersion;
import microsoft.exchange.webservices.data.core.enumeration.misc.error.ServiceError;
import microsoft.exchange.webservices.data.core.enumeration.property.WellKnownFolderName;
import microsoft.exchange.webservices.data.core.enumeration.search.SortDirection;
import microsoft.exchange.webservices.data.core.enumeration.service.DeleteMode;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceLocalException;
import microsoft.exchange.webservices.data.core.exception.service.local.ServiceVersionException;
import microsoft.exchange.webservices.data.core.exception.service.remote.ServiceResponseException;
import microsoft.exchange.webservices.data.core.service.folder.Folder;
import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import microsoft.exchange.webservices.data.core.service.item.Item;
import microsoft.exchange.webservices.data.core.service.response.ResponseMessage;
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
import microsoft.exchange.webservices.data.property.complex.FileAttachment;
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
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.xml.SaxElementBuilder;

/**
 * Implementation of a {@link IBasicFileSystem} of an Exchange Mail Inbox.
 * 
 * To obtain an accessToken:
 * <ol>
 * 	<li>follow the steps in {@link "https://docs.microsoft.com/en-us/exchange/client-developer/exchange-web-services/how-to-authenticate-an-ews-application-by-using-oauth"}</li>
 *  <li>request an Authorization-Code for scope https://outlook.office.com/EWS.AccessAsUser.All</li>
 *  <li>exchange the Authorization-Code for an accessToken</li>
 *  <li>configure the accessToken directly, or as the password of a JAAS entry referred to by authAlias</li>
 * </ol>
 * 
 * N.B. MS Exchange is susceptible to problems with invalid XML characters, like &#x3;
 * To work around these problems, a special streaming XMLInputFactory is configured in 
 * METAINF/services/javax.xml.stream.XMLInputFactory as nl.nn.adapterframework.xml.StaxParserFactory
 * 
 * @author Peter Leeuwenburgh (as {@link ExchangeMailListener})
 * @author Gerrit van Brakel
 *
 */
public class ExchangeFileSystem extends MailFileSystemBase<EmailMessage,Attachment,ExchangeService> {
	private ExchangeFileSystemCache cache = new ExchangeFileSystemCache();

	private String mailAddress;
	private boolean validateAllRedirectUrls=true;
	private String url;
	private String accessToken;
	private String filter;

	private String proxyHost = null;
	private int proxyPort = 8080;
	private String proxyUsername = null;
	private String proxyPassword = null;
	private String proxyAuthAlias = null;
	private String proxyDomain = null;

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
	protected ExchangeService createConnection() throws FileSystemException {
		ExchangeService exchangeService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);

		String defaultUsername = StringUtils.isEmpty(getAccessToken())? getUsername() : null;
		String defaultPassword = StringUtils.isEmpty(getAccessToken())? getPassword() : getAccessToken();

		CredentialFactory cf = new CredentialFactory(getAuthAlias(), defaultUsername, defaultPassword);
		if (StringUtils.isEmpty(cf.getUsername())) {
			// use OAuth Bearer token authentication
			exchangeService.getHttpHeaders().put("Authorization", "Bearer "+cf.getPassword());
		} else {
			// use deprecated Basic Authentication. Support will end 2021-Q3!
			log.warn("Using deprecated Basic Authentication method for authentication to Exchange Web Services");
			ExchangeCredentials credentials = new WebCredentials(cf.getUsername(), cf.getPassword());
			exchangeService.setCredentials(credentials);
		}

		
		
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
			try {
				exchangeService.autodiscoverUrl(getMailAddress(),redirectionUrlCallback);
				//TODO call setUrl() here to avoid repeated autodiscovery
			} catch (Exception e) {
				throw new FileSystemException("cannot autodiscover for ["+getMailAddress()+"]", e);
			}
		} else {
			try {
				exchangeService.setUrl(new URI(getUrl()));
			} catch (URISyntaxException e) {
				throw new FileSystemException("cannot set URL ["+getUrl()+"]", e);
			}
		}
		log.debug("using url ["+exchangeService.getUrl()+"]");
		return exchangeService;
	}

	@Override
	public EmailMessage toFile(String filename) throws FileSystemException {
		ExchangeService exchangeService = getConnection();
		try {
			ItemId itemId = ItemId.getItemIdFromString(filename);
			EmailMessage item = EmailMessage.bind(exchangeService,itemId);
			return item;
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException("Cannot convert filename ["+filename+"] into an ItemId", e);
		} finally {
			releaseConnection(exchangeService);
		}
	}

	@Override
	public EmailMessage toFile(String folder, String filename) throws FileSystemException {
		return toFile(filename);
	}

	private boolean itemExistsInFolder(ExchangeService exchangeService, FolderId folderId, String itemId) throws Exception {
		ItemView view = new ItemView(1);
		view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Ascending);
		SearchFilter searchFilter =  new SearchFilter.IsEqualTo(ItemSchema.Id, itemId);
		FindItemsResults<Item> findResults;
		findResults = exchangeService.findItems(folderId,searchFilter, view);
		return findResults.getTotalCount()!=0;
	}

	@Override
	public boolean exists(EmailMessage f) throws FileSystemException {
		ExchangeService exchangeService = getConnection();
		try {
			EmailMessage emailMessage = EmailMessage.bind(exchangeService, f.getId());
			return itemExistsInFolder(exchangeService, emailMessage.getParentFolderId(), f.getId().toString());
		} catch (ServiceResponseException e) {
			ServiceError errorCode = e.getErrorCode();
			if (errorCode == ServiceError.ErrorItemNotFound) {
				return false;
			}
			throw new FileSystemException(e);
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService);
		}
	}



	@Override
	public DirectoryStream<EmailMessage> listFiles(String folder) throws FileSystemException {
		if (!isOpen()) {
			return null;
		}
		String mailbox = seperateMailbox(folder);
		folder = seperateFolderName(folder);

		ExchangeService exchangeService = getConnection(mailbox);
		try {
			FolderId folderId = cache.getFolder(mailbox, folder).getId();
			ItemView view = new ItemView(getMaxNumberOfMessagesToList()<0?100:getMaxNumberOfMessagesToList());
			view.getOrderBy().add(ItemSchema.DateTimeReceived, SortDirection.Ascending);
			FindItemsResults<Item> findResults;
			if ("NDR".equalsIgnoreCase(getFilter())) {
				SearchFilter searchFilterBounce = new SearchFilter.IsEqualTo(ItemSchema.ItemClass, "REPORT.IPM.Note.NDR");
				findResults = exchangeService.findItems(folderId,searchFilterBounce, view);
			} else {
				findResults = exchangeService.findItems(folderId, view);
			}
			if (findResults.getTotalCount() == 0) {
				releaseConnection(exchangeService);
				return FileSystemUtils.getDirectoryStream((Iterator<EmailMessage>)null);
			} else {
				Iterator<Item> itemIterator = findResults.getItems().iterator();
				return FileSystemUtils.getDirectoryStream(new Iterator<EmailMessage>() {

					@Override
					public boolean hasNext() {
						return itemIterator.hasNext();
					}

					@Override
					public EmailMessage next() {
						// must cast <Items> to <EmailMessage> separately, cannot cast Iterator<Item> to Iterator<EmailMessage> 
						return (EmailMessage)itemIterator.next();
					}
					
				}, (Runnable)() -> { releaseConnection(exchangeService); });
			}
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException("Cannot list messages in folder ["+folder+"]", e);
		}
	}

	
	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		String mailbox = seperateMailbox(folder);
		folder = seperateFolderName(folder);

		FolderId folderId;
		try {
			folderId = cache.getFolder(mailbox, folder).getId();
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
		return folderId!=null;
	}

	
	@Override
	public Message readFile(EmailMessage f, String charset) throws FileSystemException, IOException {
		EmailMessage emailMessage;
		PropertySet ps = new PropertySet(EmailMessageSchema.Subject);
//		ps = new PropertySet(EmailMessageSchema.DateTimeReceived,
//				EmailMessageSchema.From, EmailMessageSchema.Subject,
//				EmailMessageSchema.Body,
//				EmailMessageSchema.DateTimeSent);
//		
		ExchangeService exchangeService = getConnection();
		try {
			if (f.getId()!=null) {
				emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
				if (isReadMimeContents()) {
					emailMessage.load(new PropertySet(ItemSchema.MimeContent));
				} else {
					emailMessage.load(new PropertySet(ItemSchema.Body));
					
				}
			} else {
				emailMessage = f;
			}
			if (isReadMimeContents()) {
				MimeContent mc = emailMessage.getMimeContent();
				return new Message(mc.getContent(), charset);
			}
			return new Message(MessageBody.getStringFromMessageBody(emailMessage.getBody()));
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService);
		}
	}
	
	@Override
	public void deleteFile(EmailMessage f) throws FileSystemException {
		 try {
			f.delete(DeleteMode.MoveToDeletedItems);
		} catch (Exception e) {
			throw new FileSystemException("Could not delete",e);
		}
	}
	@Override
	public EmailMessage moveFile(EmailMessage f, String destinationFolder, boolean createFolder) throws FileSystemException {
		String mailbox = seperateMailbox(destinationFolder);
		destinationFolder = seperateFolderName(destinationFolder);

		ExchangeService exchangeService = getConnection(mailbox);
		try {
			FolderId destinationFolderId = cache.getFolder(mailbox, destinationFolder).getId();
			return (EmailMessage)f.move(destinationFolderId);
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService);
		}
	}

	@Override
	public EmailMessage copyFile(EmailMessage f, String destinationFolder, boolean createFolder) throws FileSystemException {
		String mailbox = seperateMailbox(destinationFolder);
		destinationFolder = seperateFolderName(destinationFolder);

		ExchangeService exchangeService = getConnection(mailbox);
		try {
			FolderId destinationFolderId = cache.getFolder(mailbox, destinationFolder).getId();
			return (EmailMessage)f.copy(destinationFolderId);
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService);
		}
	}

	@Override
	public long getFileSize(EmailMessage f) throws FileSystemException {
		try {
			return f.getSize();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine size",e);
		}
	}
	@Override
	public String getName(EmailMessage f) {
		try {
			return f.getId().toString();
		} catch (ServiceLocalException e) {
			throw new RuntimeException("Could not determine Name",e);
		}
	}
	@Override
	public String getCanonicalName(EmailMessage f) throws FileSystemException {
		try {
			return f.getId().getUniqueId();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine CanonicalName",e);
		}
	}
	@Override
	public Date getModificationTime(EmailMessage f) throws FileSystemException {
		try {
			return f.getLastModifiedTime();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not determine modification time",e);
		}
	}
	
	
	private String cleanAddress(EmailAddress address) {
		String personal = address.getName();
		String email = address.getAddress();
		InternetAddress iaddress;
		try {
			iaddress = new InternetAddress(email, personal);
		} catch (UnsupportedEncodingException e) {
			return address.toString();
		}
		return iaddress.toUnicodeString();
	}
	
	private List<String> asList(EmailAddressCollection addressCollection) {
		if (addressCollection==null) {
			return Collections.emptyList();
		}
		return addressCollection.getItems().stream().map(this::cleanAddress).collect(Collectors.toList());
	}
	
	
	@Override
	public Map<String, Object> getAdditionalFileProperties(EmailMessage f) throws FileSystemException {
		EmailMessage emailMessage;
		ExchangeService exchangeService = getConnection();
		try {
			if (f.getId()!=null) {
				PropertySet ps=PropertySet.FirstClassProperties;
				emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			} else {
				emailMessage = f;
			}
			Map<String, Object> result=new LinkedHashMap<String,Object>();
			result.put(IMailFileSystem.TO_RECEPIENTS_KEY, asList(emailMessage.getToRecipients()));
			result.put(IMailFileSystem.CC_RECEPIENTS_KEY, asList(emailMessage.getCcRecipients()));
			result.put(IMailFileSystem.BCC_RECEPIENTS_KEY, asList(emailMessage.getBccRecipients()));
			result.put(IMailFileSystem.FROM_ADDRESS_KEY, getFrom(emailMessage)); 
			result.put(IMailFileSystem.SENDER_ADDRESS_KEY, getSender(emailMessage)); 
			result.put(IMailFileSystem.REPLY_TO_RECEPIENTS_KEY, getReplyTo(emailMessage)); 
			result.put(IMailFileSystem.DATETIME_SENT_KEY, getDateTimeSent(emailMessage)); 
			result.put(IMailFileSystem.DATETIME_RECEIVED_KEY, getDateTimeReceived(emailMessage)); 
			try {
				InternetMessageHeaderCollection internetMessageHeaders = emailMessage.getInternetMessageHeaders();
				if (internetMessageHeaders!=null) {
					for(InternetMessageHeader internetMessageHeader : internetMessageHeaders) {
						Object curEntry = result.get(internetMessageHeader.getName());
						if (curEntry==null) {
							result.put(internetMessageHeader.getName(), internetMessageHeader.getValue());
							continue;
						}
						List<Object> values;
						if (curEntry instanceof List) {
							values = (List<Object>)curEntry;
						} else {
							values = new LinkedList<Object>();
							values.add(curEntry);
							result.put(internetMessageHeader.getName(),values);
						}
						values.add(internetMessageHeader.getValue());
					}
				}
			} catch (ServiceLocalException e) {
				log.warn("Message ["+f.getId()+"] Cannot load message headers: "+ e.getMessage());
			}
 			result.put(IMailFileSystem.BEST_REPLY_ADDRESS_KEY, MailFileSystemUtils.findBestReplyAddress(result,getReplyAddressFields()));
			return result;
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			exchangeService = null;
			throw new FileSystemException(e);
		} finally {
			if (exchangeService!=null) {
				releaseConnection(exchangeService);
			}
		}
	}

	
	@Override
	public void forwardMail(EmailMessage emailMessage, String destination) throws FileSystemException {
		try {
			ResponseMessage forward = emailMessage.createForward();
			forward.getToRecipients().clear();
			forward.getToRecipients().add(destination);
			
//			if(forwardMessage != null){
//				forward.setBodyPrefix(MessageBody.getMessageBodyFromText(forwardMessage));
//			}

//			if(getSaveCopy() == true){
//				if(getFolderNameToSaveCopy() != null){
//					forward.sendAndSaveCopy(retrieveFolderIdByFolderName(getFolderNameToSaveCopy()));
//				} else {
//					forward.sendAndSaveCopy();
//				}
//			} else {
				forward.send();
//			}
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Iterator<Attachment> listAttachments(EmailMessage f) throws FileSystemException {
		List<Attachment> result=new LinkedList<Attachment>();
		ExchangeService exchangeService = getConnection();
		try {
			EmailMessage emailMessage;
			if (f.getId()!=null) {
				PropertySet ps = new PropertySet(EmailMessageSchema.Attachments);
				emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			} else {
				emailMessage = f;
			}
			AttachmentCollection attachmentCollection = emailMessage.getAttachments();
			for (Attachment attachment : attachmentCollection) {
				result.add(attachment);
			}
			return result.iterator();
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException("cannot read attachments",e);
		} finally {
			releaseConnection(exchangeService);
		}
	}


	@Override
	public String getAttachmentName(Attachment a) {
		return a.getName();
	}


	@Override
	public Message readAttachment(Attachment a) throws FileSystemException, IOException {
		try {
			a.load();
		} catch (Exception e) {
			throw new FileSystemException("Cannot load attachment",e);
		}
		byte[] content = null;
		if (a instanceof FileAttachment) {
			content=((FileAttachment)a).getContent();
		}
		if (a instanceof ItemAttachment) {
			ItemAttachment itemAttachment=(ItemAttachment)a;
			EmailMessage attachmentItem = (EmailMessage)itemAttachment.getItem();
			return readFile(attachmentItem, null); // we don't know the charset here, leave it null for now
		}
		if (content==null) {
			log.warn("content of attachment is null");
			content = new byte[0];
		}
		return new Message(content);
	}

	@Override
	public EmailMessage getFileFromAttachment(Attachment attachment) throws FileSystemException {
		if (attachment instanceof ItemAttachment) {
			Item item = ((ItemAttachment) attachment).getItem();
			if (item instanceof EmailMessage) {
				return (EmailMessage) item;
			}
		}
		// Attachment is not an EmailMessage itself, no need to parse further, can just return null
		return null;
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

	
	
	public FolderId getFolderIdByFolderName(ExchangeService exchangeService, String folderName, boolean create) throws Exception{
		FindFoldersResults findResults;
		String mailbox = seperateMailbox(folderName);
		folderName = seperateFolderName(folderName);

		findResults = exchangeService.findFolders(cache.getBaseFolderId(mailbox), new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName), new FolderView(Integer.MAX_VALUE));
		if (create && findResults.getTotalCount()==0) {
			log.debug("creating folder [" + folderName + "]");
			createFolder(mailbox+"|"+folderName);
			findResults = exchangeService.findFolders(cache.getBaseFolderId(mailbox), new SearchFilter.IsEqualTo(FolderSchema.DisplayName, folderName), new FolderView(Integer.MAX_VALUE));
		}
		if (findResults.getTotalCount()==0) {
			log.debug("folder [" + folderName + "] not found");
			return null;
		}
		if (log.isDebugEnabled()) {
			log.debug("amount of folders with name: " + folderName + " = " + findResults.getTotalCount());
			log.debug("found folder with name: " + findResults.getFolders().get(0).getDisplayName());
		}
		FolderId folderId = findResults.getFolders().get(0).getId();
		return folderId;
	}

	@Override
	public void createFolder(String folderName) throws FileSystemException {
		String mailbox = seperateMailbox(folderName);
		folderName = seperateFolderName(folderName);

		ExchangeService exchangeService = getConnection(mailbox);
		try {
			Folder folder = new Folder(exchangeService);
			folder.setDisplayName(folderName);
			folder.save(new FolderId(cache.getBaseFolderId(mailbox).getUniqueId()));
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException("cannot create folder ["+folderName+"]", e);
		} finally {
			releaseConnection(exchangeService);
		}
	}


	@Override
	public void removeFolder(String folderName, boolean removeNonEmptyFolder) throws FileSystemException {
		String mailbox = seperateMailbox(folderName);
		folderName = seperateFolderName(folderName);

		ExchangeService exchangeService = getConnection(mailbox);
		try {
			Folder folder = cache.getFolder(mailbox, folderName);
			if(removeNonEmptyFolder) {
				folder.empty(DeleteMode.HardDelete, true);
			}
			folder.delete(DeleteMode.HardDelete);
		} catch (Exception e) {
			invalidateConnection(exchangeService);
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService);
		}
	}

	
	protected String getFrom(EmailMessage emailMessage) throws FileSystemException {
		try {
			return cleanAddress(emailMessage.getFrom());
		} catch (ServiceLocalException e) {
			log.warn("Could not get From Address: "+ e.getMessage());
			return null;
		}
	}
	
	protected String getSender(EmailMessage emailMessage) throws FileSystemException {
		try {
			EmailAddress sender = emailMessage.getSender();
			return sender==null ? null : cleanAddress(sender);
		} catch (ServiceLocalException e) {
			log.warn("Could not get Sender Address: "+ e.getMessage());
			return null;
		}
	}

	protected List<String> getReplyTo(EmailMessage emailMessage) throws FileSystemException {
		try {
			return asList(emailMessage.getReplyTo());
		} catch (ServiceLocalException e) {
			log.warn("Could not get ReplyTo Addresses: "+ e.getMessage());
			return null;
		}
	}
	
	protected Date getDateTimeSent(EmailMessage emailMessage) throws FileSystemException {
		try {
			return emailMessage.getDateTimeSent();
		} catch (ServiceLocalException e) {
			log.warn("Could not get DateTimeSent: "+ e.getMessage());
			return null;
		}
	}
	
	protected Date getDateTimeReceived(EmailMessage emailMessage) throws FileSystemException {
		try {
			return emailMessage.getDateTimeReceived();
		} catch (ServiceLocalException e) {
			log.warn("Could not get getDateTimeReceived: "+ e.getMessage());
			return null;
		}
	}
	
	@Override
	public String getSubject(EmailMessage emailMessage) throws FileSystemException {
		try {
			return emailMessage.getSubject();
		} catch (ServiceLocalException e) {
			throw new FileSystemException("Could not get Subject", e);
		}
	}
	

	@Override
	public Message getMimeContent(EmailMessage emailMessage) throws FileSystemException {
		try {
			emailMessage.load(new PropertySet(ItemSchema.MimeContent));
			MimeContent mc = emailMessage.getMimeContent();
			return new Message(mc.getContent());
		} catch (Exception e) {
			throw new FileSystemException("Could not get MimeContent", e);
		}
		
	}
	
	@Override
	public void extractEmail(EmailMessage emailMessage, SaxElementBuilder emailXml) throws FileSystemException {
		try {
			if (emailMessage.getId()!=null) {
				PropertySet ps = new PropertySet(EmailMessageSchema.DateTimeSent, EmailMessageSchema.DateTimeReceived, EmailMessageSchema.From, 
						EmailMessageSchema.ToRecipients, EmailMessageSchema.CcRecipients, EmailMessageSchema.BccRecipients, EmailMessageSchema.Subject, 
						EmailMessageSchema.Body, EmailMessageSchema.Attachments);
				emailMessage.load(ps);
			}
			MailFileSystemUtils.addEmailInfo(this, emailMessage, emailXml);
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void extractAttachment(Attachment attachment, SaxElementBuilder attachmentsXml) throws FileSystemException {
		try {
			attachment.load();
			MailFileSystemUtils.addAttachmentInfo(this, attachment, attachmentsXml);
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}
	


	@Override
	public String getPhysicalDestinationName() {
		String result = super.getPhysicalDestinationName();
		String url = "";
		try {
			if (isOpen()) {
				ExchangeService exchangeService = getConnection();
				try {
					url = exchangeService.getUrl().toString();
				} finally {
					releaseConnection(exchangeService);
				}
			}
		} catch (Exception e) {
			log.warn("Could not get url", e);
		}
		return Misc.concatStrings("url [" + url + "] mailAddress [" + (getMailAddress() == null ? "" : getMailAddress()) + "]", " ", result);
	}

	private String seperateFolderName(String concatenatedString){
		return concatenatedString.split("\\|")[1];
	}

	private String seperateMailbox(String concatenatedString){
		return concatenatedString.split("\\|")[0];
	}

	private ExchangeService getConnection(String mailbox) throws FileSystemException {
		ExchangeService service = super.getConnection();
		service.getHttpHeaders().put("X-AnchorMailbox", mailbox);

		try {
			cache.ensureMailboxIsRegistered(mailbox, getBaseFolder(), service);
		} catch (Exception e) {
			throw new FileSystemException("An error occurred whilst loading mailbox ["+mailbox+"] into cache.");
		}

		return service;
	}

	private static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
		
		@Override
		public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
			return redirectionUrl.toLowerCase().startsWith("https://"); //TODO: provide better test on how to trust this url
		}
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

	@IbisDoc({"5", "Alias used to obtain accessToken or username and password for authentication to Exchange mail server. " + 
			"If the alias refers to a combination of a username and a password, the deprecated Basic Authentication method is used. " + 
			"If the alias refers to a password without a username, the password is treated as the accessToken.", ""})
	@Override
	public void setAuthAlias(String authAlias) {
		super.setAuthAlias(authAlias);
	}

	@IbisDoc({"6", "Username for authentication to Exchange mail server. Ignored when accessToken is also specified", ""})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to authentication using an accessToken. N.B. username no longer defaults to mailaddress")
	@Override
	public void setUsername(String username) {
		super.setUsername(username);
	}

	@IbisDoc({"7", "Password for authentication to Exchange mail server. Ignored when accessToken is also specified", ""})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to authentication using an accessToken")
	@Override
	public void setPassword(String password) {
		super.setPassword(password);
	}



	@IbisDoc({"9", "If empty, all mails are retrieved. If set to <code>NDR</code> only Non-Delivery Report mails ('bounces') are retrieved", ""})
	public void setFilter(String filter) {
		this.filter = filter;
	}
	public String getFilter() {
		return filter;
	}


	@IbisDoc({"13", "proxy host", ""})
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}
	public String getProxyHost() {
		return proxyHost;
	}

	@IbisDoc({"14", "proxy port", "8080"})
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}
	public int getProxyPort() {
		return proxyPort;
	}

	@IbisDoc({"15", "proxy username", ""})
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}
	public String getProxyUsername() {
		return proxyUsername;
	}

	@IbisDoc({"16", "proxy password", ""})
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}
	public String getProxyPassword() {
		return proxyPassword;
	}

	@IbisDoc({"17", "proxy authAlias", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}
	public String getProxyAuthAlias() {
		return proxyAuthAlias;
	}

	@IbisDoc({"18", "proxy domain", ""})
	public void setProxyDomain(String proxyDomain) {
		this.proxyDomain = proxyDomain;
	}
	public String getProxyDomain() {
		return proxyDomain;
	}
	
}
