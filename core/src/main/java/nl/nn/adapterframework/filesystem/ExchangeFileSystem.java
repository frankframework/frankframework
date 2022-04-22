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
import java.net.MalformedURLException;
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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import lombok.Getter;
import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.core.ExchangeService;
import microsoft.exchange.webservices.data.core.PropertySet;
import microsoft.exchange.webservices.data.core.WebProxy;
import microsoft.exchange.webservices.data.core.enumeration.misc.ConnectingIdType;
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
import microsoft.exchange.webservices.data.misc.ImpersonatedUserId;
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
 * <br/>
 * To make use of modern authentication:
 * <ol>
 *     	<li>Create an application in Azure AD -> App Registrations. For more information please read {@link "https://docs.microsoft.com/en-us/exchange/client-developer/exchange-web-services/how-to-authenticate-an-ews-application-by-using-oauth"}</li>
 *     	<li>Request the required API permissions within desired scope <code>https://outlook.office365.com/</code> in Azure AD -> App Registrations -> MyApp -> API Permissions.</li>
 *     	<li>Create a secret for your application in Azure AD -> App Registrations -> MyApp -> Certificates and Secrets</li>
 *     	<li>Configure the clientSecret directly as password or as the password of a JAAS entry referred to by authAlias. Only available upon creation of your secret in the previous step.</li>
 *     	<li>Configure the clientId directly as username or as the username of a JAAS entry referred to by authAlias which could be retrieved from Azure AD -> App Registrations -> MyApp -> Overview</li>
 *     	<li>Configure the tenantId which could be retrieved from Azure AD -> App Registrations -> MyApp -> Overview</li>
 * 		<li>Make sure your application is able to reach <code>https://login.microsoftonline.com</code>. Required for token retrieval. </li>
 * </ol>
 *
 *
 * N.B. MS Exchange is susceptible to problems with invalid XML characters, like &amp;#x3;.
 * To work around these problems, a special streaming XMLInputFactory is configured in
 * METAINF/services/javax.xml.stream.XMLInputFactory as nl.nn.adapterframework.xml.StaxParserFactory
 *
 * @author Peter Leeuwenburgh (as {@link ExchangeMailListener})
 * @author Gerrit van Brakel
 *
 */
public class ExchangeFileSystem extends MailFileSystemBase<EmailMessage,Attachment,ExchangeService> {

	private final ExchangeFileSystemCache cache = new ExchangeFileSystemCache();
	private @Getter String mailboxFolderSeparator;

	private final @Getter(onMethod = @__(@Override)) String domain = "Exchange";
	private @Getter String mailAddress;
	private @Getter boolean validateAllRedirectUrls=true;
	private @Getter String url;
	private @Getter String filter;

	private @Getter String proxyHost = null;
	private @Getter int proxyPort = 8080;
	private @Getter String proxyUsername = null;
	private @Getter String proxyPassword = null;
	private @Getter String proxyAuthAlias = null;
	private @Getter String proxyDomain = null;

	private final String AUTHORITY = "https://login.microsoftonline.com/";
	private final String SCOPE = "https://outlook.office365.com/.default";
	private @Getter String clientId = null;
	private @Getter String clientSecret = null;
	private @Getter String tenantId = null;
	private ConfidentialClientApplication client;

	private final String ANCHOR_HEADER = "X-AnchorMailbox";
	private CredentialFactory cf;

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
		super.open();
		if(cf == null){
			cf = new CredentialFactory(getAuthAlias(), getClientId(), getClientSecret());
		}
		if( StringUtils.isNotEmpty(getTenantId()) ){
			try {
				client = ConfidentialClientApplication.builder(
						cf.getUsername(),
						ClientCredentialFactory.createFromSecret(cf.getPassword()))
					.authority(AUTHORITY + getTenantId())
					.build();
			} catch (MalformedURLException e){
				throw new FileSystemException("Failed to initialize MSAL ConfidentialClientApplication.", e);
			}
		}
		if(StringUtils.isNotEmpty(getMailAddress())){
			registerMailbox(getMailAddress());
		}
	}

	public FolderId getBaseFolderId(String emailAddress, String baseFolderName) throws FileSystemException {
		FolderId basefolderId;

		log.debug("searching inbox ");
		FolderId inboxId;
		if (StringUtils.isNotEmpty(emailAddress)) {
			Mailbox mailbox = new Mailbox(emailAddress);
			inboxId = new FolderId(WellKnownFolderName.Inbox, mailbox);
		} else {
			inboxId = new FolderId(WellKnownFolderName.Inbox);
		}
		log.debug("determined inbox ["+inboxId+"] foldername ["+inboxId.getFolderName()+"]");

		if (StringUtils.isNotEmpty(baseFolderName)) {
			try {
				basefolderId=findFolder(emailAddress, inboxId, baseFolderName);
			} catch (Exception e) {
				throw new FileSystemException("Could not find baseFolder ["+baseFolderName+"] as subfolder of ["+inboxId.getFolderName()+"]", e);
			}
			if (basefolderId==null) {
				log.debug("Could not get baseFolder ["+baseFolderName+"] as subfolder of ["+inboxId.getFolderName()+"]");
				basefolderId=findFolder(emailAddress,null, baseFolderName);
			}
			if (basefolderId==null) {
				throw new FileSystemException("Could not find baseFolder ["+baseFolderName+"]");
			}
		} else {
			basefolderId=inboxId;
		}

		return basefolderId;
	}


	@Override
	protected ExchangeService createConnection() throws FileSystemException {
		ExchangeService exchangeService = new ExchangeService(ExchangeVersion.Exchange2010_SP2);

		if (client != null) {
			ClientCredentialParameters clientCredentialParam = ClientCredentialParameters.builder(
				Collections.singleton(SCOPE)
			).build();

			CompletableFuture<IAuthenticationResult> future = client.acquireToken(clientCredentialParam);
			try {
				String token = future.get().accessToken();
				// use OAuth Bearer token authentication
				exchangeService.getHttpHeaders().put("Authorization", "Bearer "+token);
			} catch (Exception e){
				throw new FileSystemException("Could not generate access token!", e);
			}
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

		if(StringUtils.isNotEmpty(getMailAddress())){
			setMailboxOnService(exchangeService, getMailAddress());
		}

		return exchangeService;
	}

	public FolderId findFolder(String mailbox, FolderId baseFolderId, String folderName) throws FileSystemException {
		ExchangeService exchangeService = getConnection(mailbox);
		boolean invalidateConnectionOnRelease = false;
		try {
			FindFoldersResults findFoldersResultsIn;
			FolderId result;
			FolderView folderViewIn = new FolderView(10);
			if (StringUtils.isNotEmpty(folderName)) {
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
				//findFoldersResultsIn = getExchangeService().findFolders(baseFolderId, folderViewIn);
				return baseFolderId;
			}
			if (findFoldersResultsIn.getFolders().isEmpty()) {
				result=baseFolderId;
			} else {
				result=findFoldersResultsIn.getFolders().get(0).getId();
			}
			return result;
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("Cannot find folder ["+folderName+"]", e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}


	@Override
	public EmailMessage toFile(String filename) throws FileSystemException {
		ExchangeService exchangeService = getConnection();
		boolean invalidateConnectionOnRelease = false;
		try {
			ItemId itemId = ItemId.getItemIdFromString(filename);
			return EmailMessage.bind(exchangeService,itemId);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("Cannot convert filename ["+filename+"] into an ItemId", e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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
		boolean invalidateConnectionOnRelease = false;
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
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}



	@Override
	public DirectoryStream<EmailMessage> listFiles(String folder) throws FileSystemException {
		if (!isOpen()) {
			return null;
		}
		ExchangeFileSystemResolver resolver = getResolver(folder);
		ExchangeService exchangeService = getConnection(resolver.getMailbox());
		boolean invalidateConnectionOnRelease = false;
		boolean closeConnectionOnExit = true;

		try {
			FolderId folderId = getFolderIdByFolderName(exchangeService, resolver, true, false);
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
				return FileSystemUtils.getDirectoryStream((Iterator<EmailMessage>)null);
			}
			Iterator<Item> itemIterator = findResults.getItems().iterator();
			DirectoryStream<EmailMessage> result = FileSystemUtils.getDirectoryStream(new Iterator<EmailMessage>() {

				@Override
				public boolean hasNext() {
					return itemIterator.hasNext();
				}

				@Override
				public EmailMessage next() {
					// must cast <Items> to <EmailMessage> separately, cannot cast Iterator<Item> to Iterator<EmailMessage>
					return (EmailMessage)itemIterator.next();
				}

			}, (Runnable)() -> { releaseConnection(exchangeService, false); });
			closeConnectionOnExit = false;
			return result;
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("Cannot list messages in folder ["+folder+"]", e);
		} finally {
			if (closeConnectionOnExit) {
				releaseConnection(exchangeService, invalidateConnectionOnRelease);
			}
		}
	}

	@Override
	public int getNumberOfFilesInFolder(String foldername) throws FileSystemException {
		if (!isOpen()) {
			return -1;
		}
		ExchangeFileSystemResolver resolver = getResolver(foldername);
		ExchangeService exchangeService = getConnection(resolver.getMailbox());

		boolean invalidateConnectionOnRelease = false;
		try {
			FolderId folderId = getFolderIdByFolderName(exchangeService, resolver, false, false);
			Folder folder = Folder.bind(exchangeService,folderId);
			return folder.getTotalCount();
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("Cannot list messages in folder ["+foldername+"]", e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		ExchangeFileSystemResolver resolver = getResolver(folder);
		ExchangeService exchangeService = getConnection(resolver.getMailbox());
		boolean invalidateConnectionOnRelease = false;

		FolderId folderId = null;
		try {
			folderId = getFolderIdByFolderName(exchangeService, resolver, false, true);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}

		return folderId!=null;
	}


	@Override
	public Message readFile(EmailMessage f, String charset) throws FileSystemException, IOException {
		EmailMessage emailMessage;

		ExchangeService exchangeService = getConnection();
		boolean invalidateConnectionOnRelease = false;
		try {
			if (f.getId()!=null) {
				PropertySet ps = new PropertySet(EmailMessageSchema.DateTimeReceived,
						EmailMessageSchema.From, EmailMessageSchema.Subject,
						EmailMessageSchema.DateTimeSent,
						EmailMessageSchema.LastModifiedTime,
						EmailMessageSchema.Size);
				if (isReadMimeContents()) {
					ps.add(ItemSchema.MimeContent);
				} else {
					ps.add(ItemSchema.Body);
				}
				emailMessage = EmailMessage.bind(exchangeService, f.getId(), ps);
			} else {
				emailMessage = f;
			}
			if (isReadMimeContents()) {
				MimeContent mc = emailMessage.getMimeContent();
				return new Message(mc.getContent(), charset);
			}
			return new Message(MessageBody.getStringFromMessageBody(emailMessage.getBody()), FileSystemUtils.getContext(this, emailMessage));
		} catch (FileSystemException e) {
			invalidateConnectionOnRelease = true;
			throw e;
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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
		ExchangeFileSystemResolver resolver = getResolver(destinationFolder);
		ExchangeService exchangeService = getConnection(resolver.getMailbox());
		boolean invalidateConnectionOnRelease = false;

		try {
			// When f is moved from mailbox A to mailbox B, null will be returned.
			FolderId destinationFolderId = getFolderIdByFolderName(exchangeService, resolver, createFolder, false);
			return (EmailMessage)f.move(destinationFolderId);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}

	@Override
	public EmailMessage copyFile(EmailMessage f, String destinationFolder, boolean createFolder) throws FileSystemException {
		ExchangeFileSystemResolver resolver = getResolver(destinationFolder);
		ExchangeService exchangeService = getConnection(resolver.getMailbox());
		boolean invalidateConnectionOnRelease = false;

		try {
			FolderId destinationFolderId = getFolderIdByFolderName(exchangeService, resolver, createFolder, false);
			Item destinationItem = f.copy(destinationFolderId);

			// When f is copied from mailbox A to mailbox B, a class cast exception occurs:
			// (ClassCastException) microsoft.exchange.webservices.data.core.service.item.Item cannot be cast to microsoft.exchange.webservices.data.core.service.item.EmailMessage
			// Work-around is to use .bind() (expensive) method to retrieve the copied instance of f as an EmailMessage.
			if(destinationItem instanceof EmailMessage){
				if(log.isDebugEnabled()) log.debug("Attempting to cast to EmailMessage");
				return (EmailMessage) destinationItem;
			} else {
				if(log.isDebugEnabled()) log.debug("Unable to cast to EmailMessage, will use bind method!");
				return EmailMessage.bind(exchangeService, destinationItem.getId());
			}
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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
			if (f.getId()==null) {
				return null; // attachments don't have an id, but appear to be loaded at the same time as the main message
			}
			return f.getId().toString();
		} catch (ServiceLocalException e) {
			throw new RuntimeException("Could not determine Name",e);
		}
	}
	@Override
	public String getCanonicalName(EmailMessage f) throws FileSystemException {
		try {
			if (f.getId()==null) {
				return null; // attachments don't have an id, but appear to be loaded at the same time as the main message
			}
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
		boolean invalidateConnectionOnRelease = false;
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
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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
		boolean invalidateConnectionOnRelease = false;
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
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("cannot read attachments",e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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

	public FolderId getFolderIdByFolderName(ExchangeService exchangeService, String folderName, boolean create, boolean skipCache) throws Exception {
		ExchangeFileSystemResolver resolver = getResolver(folderName);

		return getFolderIdByFolderName(exchangeService, resolver, create, skipCache);
	}

	private FolderId getFolderIdByFolderName(ExchangeService exchangeService, ExchangeFileSystemResolver resolver, boolean create, boolean skipCache) throws Exception {
		FolderId folderId = skipCache ? null : cache.getFolderId(resolver);
		setMailboxOnService(exchangeService, resolver.getMailbox());

		if(folderId == null){
			FindFoldersResults findResults;
			FolderId basefolderId = cache.getBaseFolderId(resolver.getMailbox());
			if(basefolderId==null){
				basefolderId = getBaseFolderId(resolver.getMailbox(), getBaseFolder());
			}

			findResults = exchangeService.findFolders(basefolderId, new SearchFilter.IsEqualTo(FolderSchema.DisplayName, resolver.getFolderName()), new FolderView(Integer.MAX_VALUE));
			if (create && findResults.getTotalCount()==0) {
				log.debug("creating folder [" + resolver.getFolderName() + "] in mailbox ["+resolver.getMailbox()+"]");
				createFolder(resolver);
				findResults = exchangeService.findFolders(basefolderId, new SearchFilter.IsEqualTo(FolderSchema.DisplayName, resolver.getFolderName()), new FolderView(Integer.MAX_VALUE));
			}
			if (findResults.getTotalCount()==0) {
				log.debug("folder [" + resolver.getFolderName() + "] not found in mailbox ["+resolver.getMailbox()+"]");
				return null;
			}
			if (log.isDebugEnabled()) {
				log.debug("amount of folders with name: " + resolver.getFolderName() + " = " + findResults.getTotalCount());
				log.debug("found folder with name: " + findResults.getFolders().get(0).getDisplayName());
			}
			folderId = findResults.getFolders().get(0).getId();

			cache.registerResolversFolder(resolver, folderId);
		}

		return folderId;
	}


	private void createFolder(ExchangeFileSystemResolver resolver) throws FileSystemException {
		ExchangeService exchangeService = getConnection(resolver.getMailbox());
		boolean invalidateConnectionOnRelease = false;
		try {
			Folder folder = new Folder(exchangeService);
			folder.setDisplayName(resolver.getFolderName());
			folder.save(cache.getBaseFolderId(resolver.getMailbox()));
			cache.registerFolder(resolver.getMailbox(), folder);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("cannot create folder ["+resolver.getFolderName()+"] in mailbox ["+resolver.getMailbox()+"]", e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}

	@Override
	public void createFolder(String folderName) throws FileSystemException {
		createFolder(getResolver(folderName));
	}


	@Override
	public void removeFolder(String folderName, boolean removeNonEmptyFolder) throws FileSystemException {
		ExchangeFileSystemResolver resolver = getResolver(folderName);
		ExchangeService exchangeService = getConnection(resolver.getMailbox());
		boolean invalidateConnectionOnRelease = false;

		try {
			FolderId folderId = getFolderIdByFolderName(exchangeService, folderName, false, false);
			Folder folder = Folder.bind(exchangeService, folderId);
			if(removeNonEmptyFolder) {
				folder.empty(DeleteMode.HardDelete, true);
			}
			folder.delete(DeleteMode.HardDelete);
			cache.deregisterFolder(resolver.getMailbox(), folder);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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
			if (emailMessage.getId()!=null) { // attachments don't have an id, but appear to be loaded at the same time as the main message
				emailMessage.load(new PropertySet(ItemSchema.Subject));
			}
			return emailMessage.getSubject();
		} catch (Exception e) {
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
		} catch (FileSystemException e) {
			throw e;
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void extractAttachment(Attachment attachment, SaxElementBuilder attachmentsXml) throws FileSystemException {
		try {
			attachment.load();
			MailFileSystemUtils.addAttachmentInfo(this, attachment, attachmentsXml);
		} catch (FileSystemException e) {
			throw e;
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
				boolean invalidateConnectionOnRelease = false;
				try {
					url = exchangeService.getUrl().toString();
				} finally {
					releaseConnection(exchangeService, invalidateConnectionOnRelease);
				}
			}
		} catch (Exception e) {
			log.warn("Could not get url", e);
		}
		return Misc.concatStrings("url [" + url + "] mailAddress [" + (getMailAddress() == null ? "" : getMailAddress()) + "]", " ", result);
	}

	private ExchangeFileSystemResolver getResolver(String folderName){
		return new ExchangeFileSystemResolver(folderName, getMailAddress(), getMailboxFolderSeparator());
	}

	private void setMailboxOnService(ExchangeService service, String mailbox){
		service.getHttpHeaders().put(ANCHOR_HEADER, mailbox);
		service.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.SmtpAddress, mailbox));
	}

	private ExchangeService getConnection(String mailbox) throws FileSystemException {
		ExchangeService service = super.getConnection();

		boolean invalidateConnectionOnRelease = false;

		if(!cache.isMailboxRegistered(mailbox)) {
			try {
				registerMailbox(mailbox, service);
			} catch (Exception e) {
				try {
					invalidateConnectionOnRelease = true;
					throw new FileSystemException(e);
				} finally {
					releaseConnection(service, invalidateConnectionOnRelease);
				}
			}
		} else {
			setMailboxOnService(service, mailbox);
		}

		return service;
	}

	@Override
	protected void releaseConnection(ExchangeService service, boolean invalidateConnectionOnRelease){
		service.getHttpHeaders().remove(ANCHOR_HEADER);
		super.releaseConnection(service, invalidateConnectionOnRelease);
	}

	private void registerMailbox(String mailbox) throws FileSystemException {
		if(!cache.isMailboxRegistered(mailbox)){
			ExchangeService service = super.getConnection();

			boolean invalidateConnectionOnRelease = false;
			try {
				registerMailbox(mailbox, service);
			} catch (Exception e) {
				invalidateConnectionOnRelease = true;
				throw new FileSystemException(e);
			} finally {
				releaseConnection(service, invalidateConnectionOnRelease);
			}
		}
	}

	private void registerMailbox(String mailbox, ExchangeService service) throws Exception {
		setMailboxOnService(service, mailbox);
		FolderId baseFolderId = getBaseFolderId(mailbox, getBaseFolder());
		List<Folder> folders = findFolders(service, baseFolderId, Integer.MAX_VALUE);

		cache.registerMailbox(mailbox, baseFolderId, folders);
	}

	/**
	 * Standard method to retrieve a list of Folder objects within a parent Folder.
	 *
	 * @param service - An instance of Exchange Service to use to make the call
	 * @param parentFolderId - The ID of the parent folder to check for sub Folders.
	 *
	 * @return ArrayList<Folder> - The list of found Folder objects within parent folder.
	 */
	private List<Folder> findFolders(ExchangeService service, FolderId parentFolderId, int folderViewCount) throws Exception {
		return service.findFolders(parentFolderId, new FolderView(folderViewCount)).getFolders();
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

	@IbisDoc({"2", "When <code>true</code>, all redirect uris are accepted when connecting to the server", "true"})
	public void setValidateAllRedirectUrls(boolean validateAllRedirectUrls) {
		this.validateAllRedirectUrls = validateAllRedirectUrls;
	}

	@IbisDoc({"3", "Url of the Exchange server. Set to e.g. https://outlook.office365.com/EWS/Exchange.asmx to speed up start up, leave empty to use autodiscovery", ""})
	public void setUrl(String url) {
		this.url = url;
	}

	@IbisDoc({"4", "Client ID that represents a registered application in Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Overview.", ""})
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}


	@IbisDoc({"5", "Client secret that belongs to registered application in Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Certificates and Secrets", ""})
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	@IbisDoc({"6", "Tenant ID that represents the tenant in which the registered application exists within Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Overview.", ""})
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@IbisDoc({"7", "Alias used to obtain client ID and secret or username and password for authentication to Exchange mail server. " +
			"If the attribute tenantId is empty, the deprecated Basic Authentication method is used. " +
			"If the attribute tenantId is not empty, the username and password are treated as the client ID and secret.", ""})
	@Override
	public void setAuthAlias(String authAlias) {
		super.setAuthAlias(authAlias);
	}

	@IbisDoc({"8", "Username for authentication to Exchange mail server. Ignored when tenantId is also specified", ""})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret!")
	@Override
	public void setUsername(String username) {
		super.setUsername(username);
		setClientId(username);
	}

	@IbisDoc({"9", "Password for authentication to Exchange mail server. Ignored when tenantId is also specified", ""})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret!")
	@Override
	public void setPassword(String password) {
		super.setPassword(password);
		setClientSecret(password);
	}

	@IbisDoc({"10", "If empty, all mails are retrieved. If set to <code>NDR</code> only Non-Delivery Report mails ('bounces') are retrieved", ""})
	public void setFilter(String filter) {
		this.filter = filter;
	}


	@IbisDoc({"13", "proxy host", ""})
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	@IbisDoc({"14", "proxy port", "8080"})
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@IbisDoc({"15", "proxy username", ""})
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	@IbisDoc({"16", "proxy password", ""})
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	@IbisDoc({"17", "proxy authAlias", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}

	@IbisDoc({"18", "proxy domain", ""})
	public void setProxyDomain(String proxyDomain) {
		this.proxyDomain = proxyDomain;
	}

	@IbisDoc({"19", "Separator character used when working with multiple mailboxes, specified before the separator in the folder name <code>test@organisation.com|My sub folder</code>. Please consider when moving emails across mailboxes that there will be a null value returned instead of the newly created identifier. ", "|"})
	public void setMailboxFolderSeparator(String separator) {
		this.mailboxFolderSeparator = separator;
	}

}
