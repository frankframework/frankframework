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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import jakarta.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import com.microsoft.aad.msal4j.ClientCredentialFactory;
import com.microsoft.aad.msal4j.ClientCredentialParameters;
import com.microsoft.aad.msal4j.ConfidentialClientApplication;
import com.microsoft.aad.msal4j.IAuthenticationResult;

import lombok.Getter;
import lombok.Setter;
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
import microsoft.exchange.webservices.data.core.response.ServiceResponse;
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
import microsoft.exchange.webservices.data.misc.SoapFaultDetails;
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
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.encryption.HasKeystore;
import nl.nn.adapterframework.encryption.HasTruststore;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.receivers.ExchangeMailListener;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.SpringUtils;
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
public class ExchangeFileSystem extends MailFileSystemBase<EmailMessage,Attachment,ExchangeService> implements HasKeystore, HasTruststore {
	private final @Getter(onMethod = @__(@Override)) String domain = "Exchange";
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private @Getter String name="ExchangeFileSystem";

	private @Getter String mailAddress;
	private @Getter String mailboxObjectSeparator="|";
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
	private final String ANCHOR_HEADER = "X-AnchorMailbox";
	private @Getter String clientId = null;
	private @Getter String clientSecret = null;
	private @Getter String tenantId = null;

	/* SSL */
	private @Getter @Setter String keystore;
	private @Getter @Setter String keystoreAuthAlias;
	private @Getter @Setter String keystorePassword;
	private @Getter @Setter KeystoreType keystoreType=KeystoreType.PKCS12;
	private @Getter @Setter String keystoreAlias;
	private @Getter @Setter String keystoreAliasAuthAlias;
	private @Getter @Setter String keystoreAliasPassword;
	private @Getter @Setter String keyManagerAlgorithm=null;

	private @Getter @Setter String truststore=null;
	private @Getter @Setter String truststoreAuthAlias;
	private @Getter @Setter String truststorePassword=null;
	private @Getter @Setter KeystoreType truststoreType=KeystoreType.JKS;
	private @Getter @Setter String trustManagerAlgorithm=null;
	private @Getter @Setter boolean allowSelfSignedCertificates = false;
	private @Getter @Setter boolean verifyHostname=true;
	private @Getter @Setter boolean ignoreCertificateExpiredException=false;

	private @Getter CredentialFactory credentials=null;
	private @Getter CredentialFactory proxyCredentials=null;

	private ConfidentialClientApplication client;
	private MsalClientAdapter msalClientAdapter;
	private ExecutorService executor;
	private ClientCredentialParameters clientCredentialParam;

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

		if (StringUtils.isNotEmpty(getTenantId())) {
			credentials = new CredentialFactory(getAuthAlias(), getClientId(), getClientSecret());
		} else {
			credentials = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		}
		if (StringUtils.isNotEmpty(getProxyHost()) && (StringUtils.isNotEmpty(getProxyAuthAlias()) || StringUtils.isNotEmpty(getProxyUsername()) || StringUtils.isNotEmpty(getProxyPassword()))) {
			proxyCredentials = new CredentialFactory(getProxyAuthAlias(), getProxyUsername(), getProxyPassword());
		}

		if( StringUtils.isNotEmpty(getTenantId()) ){
			msalClientAdapter = applicationContext!=null ? SpringUtils.createBean(applicationContext, MsalClientAdapter.class): new MsalClientAdapter();
			msalClientAdapter.setProxyHost(getProxyHost());
			msalClientAdapter.setProxyPort(getProxyPort());
			CredentialFactory proxyCf = getProxyCredentials();
			if (proxyCf!=null) {
				msalClientAdapter.setProxyUsername(proxyCf.getUsername());
				msalClientAdapter.setProxyPassword(proxyCf.getPassword());
			}

			msalClientAdapter.setKeystore(getKeystore());
			msalClientAdapter.setKeystoreType(getKeystoreType());
			msalClientAdapter.setKeystoreAuthAlias(getKeystoreAuthAlias());
			msalClientAdapter.setKeystorePassword(getKeystorePassword());
			msalClientAdapter.setKeystoreAlias(getKeystoreAlias());
			msalClientAdapter.setKeystoreAliasAuthAlias(getKeystoreAliasAuthAlias());
			msalClientAdapter.setKeystoreAliasPassword(getKeystoreAliasPassword());
			msalClientAdapter.setKeyManagerAlgorithm(getKeyManagerAlgorithm());

			msalClientAdapter.setTruststore(getTruststore());
			msalClientAdapter.setTruststoreType(getTruststoreType());
			msalClientAdapter.setTruststoreAuthAlias(getTruststoreAuthAlias());
			msalClientAdapter.setTruststorePassword(getTruststorePassword());
			msalClientAdapter.setTrustManagerAlgorithm(getTrustManagerAlgorithm());
			msalClientAdapter.setVerifyHostname(isVerifyHostname());
			msalClientAdapter.setAllowSelfSignedCertificates(isAllowSelfSignedCertificates());
			msalClientAdapter.setIgnoreCertificateExpiredException(isIgnoreCertificateExpiredException());

			msalClientAdapter.configure();

			clientCredentialParam = ClientCredentialParameters.builder(
				Collections.singleton(SCOPE)
			).tenant(getTenantId()).build();
		}
	}

	@Override
	public void open() throws FileSystemException {
		super.open();
		if( msalClientAdapter != null ){
			executor = Executors.newSingleThreadExecutor(); //Create a new Executor in the same thread(context) to avoid SecurityExceptions when setting a ClassLoader on the Runnable.

			CredentialFactory cf = getCredentials();

			try {
				msalClientAdapter.open();

				client = ConfidentialClientApplication.builder(
						cf.getUsername(),
						ClientCredentialFactory.createFromSecret(cf.getPassword()))
					.authority(AUTHORITY + getTenantId())
					.httpClient(msalClientAdapter)
					.executorService(executor)
					.build();
			} catch (MalformedURLException | SenderException e) {
				throw new FileSystemException("Failed to initialize MSAL ConfidentialClientApplication.", e);
			}
		}
		basefolderId = getBaseFolderId(getMailAddress(),getBaseFolder());
	}

	@Override
	public void close() throws FileSystemException {
		try {
			super.close();
			if(msalClientAdapter != null){
				msalClientAdapter.close();
				client = null;
			}
		} catch (SenderException e){
			throw new FileSystemException("An exception occurred during closing of MSAL HttpClient", e);
		} finally {
			if(executor != null) {
				executor.shutdown();
				executor = null;
			}
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
				basefolderId=findFolder(inboxId,baseFolderName);
			} catch (Exception e) {
				throw new FileSystemException("Could not find baseFolder ["+baseFolderName+"] as subfolder of ["+inboxId.getFolderName()+"]", e);
			}
			if (basefolderId==null) {
				log.debug("Could not get baseFolder ["+baseFolderName+"] as subfolder of ["+inboxId.getFolderName()+"]");
				basefolderId=findFolder(null,baseFolderName);
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
			CompletableFuture<IAuthenticationResult> future = client.acquireToken(clientCredentialParam);
			try {
				String token = future.get().accessToken();
				// use OAuth Bearer token authentication
				exchangeService.getHttpHeaders().put("Authorization", "Bearer "+token);
			} catch (Exception e){
				throw new FileSystemException("Could not generate access token!", e);
			}
		} else {
			CredentialFactory cf = getCredentials();
			// use deprecated Basic Authentication. Support will end 2021-Q3!
			log.warn("Using deprecated Basic Authentication method for authentication to Exchange Web Services");
			ExchangeCredentials exchangeCredentials = new WebCredentials(cf.getUsername(), cf.getPassword());
			exchangeService.setCredentials(exchangeCredentials);
		}

		if(StringUtils.isNotEmpty(getMailAddress())){
			setMailboxOnService(exchangeService, getMailAddress());
		}

		if (StringUtils.isNotEmpty(getProxyHost())) {
			CredentialFactory proxyCf = getProxyCredentials();
			WebProxyCredentials webProxyCredentials = proxyCf !=null ? new WebProxyCredentials(proxyCf.getUsername(), proxyCf.getPassword(), getProxyDomain()) : null;
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


	public FolderId findFolder(ExchangeObjectReference reference) throws FileSystemException {
		return findFolder(reference.getBaseFolderId(), reference.getObjectName());
	}

	/**
	 * find a folder for listFiles(), getNumberOfFilesInFolder(), folderExists.
	 * If folderName is empty, the result defaults to the baseFolder.
	 * If baseFolder is null, the folder is searched in the root of the message folder hierarchy.
	 * If the folder is not found, null is returned.
	 */
	public FolderId findFolder(FolderId baseFolderId, String folderName) throws FileSystemException {
		if (StringUtils.isEmpty(folderName)) {
			return baseFolderId;
		}
		ExchangeObjectReference targetFolder = asObjectReference(folderName, baseFolderId);
		ExchangeService exchangeService = getConnection(targetFolder);
		boolean invalidateConnectionOnRelease = false;
		try {
			return findFolder(exchangeService, targetFolder);
		} catch (FileSystemException e) {
			invalidateConnectionOnRelease = true;
			throw e;
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}

	private FolderId findFolder(ExchangeService exchangeService, ExchangeObjectReference targetFolder) throws FileSystemException {
		return findFolder(exchangeService, targetFolder, false);
	}

	private FolderId findFolder(ExchangeService exchangeService, ExchangeObjectReference targetFolder, boolean createFolder) throws FileSystemException {
		FindFoldersResults findFoldersResultsIn;
		FolderView folderViewIn = new FolderView(10);
		SearchFilter searchFilterIn = new SearchFilter.IsEqualTo(FolderSchema.DisplayName, targetFolder.getObjectName());
		try {
			if (targetFolder.getBaseFolderId()!=null) {
				findFoldersResultsIn = exchangeService.findFolders(targetFolder.getBaseFolderId(), searchFilterIn, folderViewIn);
			} else {
				findFoldersResultsIn = exchangeService.findFolders(WellKnownFolderName.MsgFolderRoot, searchFilterIn, folderViewIn);
			}
		} catch (Exception e) {
			throw new FileSystemException("Cannot find folder ["+targetFolder.getObjectName()+"]", e);
		}
		if (findFoldersResultsIn.getTotalCount() == 0) {
			if(log.isDebugEnabled()) log.debug("no folder found with name [" + targetFolder.getObjectName() + "] in basefolder ["+targetFolder.getBaseFolderId()+"]");
			if(createFolder){
				if(log.isDebugEnabled()) log.debug("creating folder with name [" + targetFolder.getObjectName() + "] in basefolder ["+targetFolder.getBaseFolderId()+"]");
				createFolder(targetFolder.getOriginalReference());
				return findFolder(exchangeService, targetFolder, false);
			}
			return null;
		}
		if (findFoldersResultsIn.getTotalCount() > 1) {
			if (log.isDebugEnabled()) {
				for (Folder folder:findFoldersResultsIn.getFolders()) {
					try {
						log.debug("found folder ["+folder.getDisplayName()+"]");
					} catch (ServiceLocalException e) {
						log.warn("could not display foldername", e);
					}
				}
			}
			throw new FileSystemException("multiple folders found with name ["+ targetFolder.getObjectName() + "]");
		}
		return findFoldersResultsIn.getFolders().get(0).getId();
	}

	@Override
	public EmailMessage toFile(String filename) throws FileSystemException {
		ExchangeObjectReference reference = asObjectReference(filename);
		ExchangeService exchangeService = getConnection(reference);
		boolean invalidateConnectionOnRelease = false;
		try {
			ItemId itemId = ItemId.getItemIdFromString(reference.getObjectName());
			// TODO: check if this bind can be left out
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
			setMailboxOnService(exchangeService, getReceivedBy(f));
			// TODO: check if this bind can be left out
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
		ExchangeObjectReference reference = asObjectReference(folder);
		ExchangeService exchangeService = getConnection(reference);
		boolean invalidateConnectionOnRelease = false;
		boolean closeConnectionOnExit = true;
		try {
			FolderId folderId = findFolder(reference);
			if (folderId==null) {
				throw new FileSystemException("Cannot find folder ["+folder+"]");
			}
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
		ExchangeObjectReference reference = asObjectReference(foldername);
		ExchangeService exchangeService = getConnection(reference);
		FolderId folderId = findFolder(exchangeService, reference);
		if (folderId==null) {
			throw new FileSystemException("Cannot find folder ["+foldername+"]");
		}
		boolean invalidateConnectionOnRelease = false;
		try {
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
		FolderId folderId = findFolder(asObjectReference(folder));
		return folderId!=null;
	}

	@Override
	public Message readFile(EmailMessage f, String charset) throws FileSystemException, IOException {
		EmailMessage emailMessage = f;

		try {
			if (emailMessage.getId()!=null) {
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
				emailMessage.load(ps);
			}
			if (isReadMimeContents()) {
				MimeContent mc = emailMessage.getMimeContent();
				return new Message(mc.getContent(), charset);
			}
			return new Message(MessageBody.getStringFromMessageBody(emailMessage.getBody()), FileSystemUtils.getContext(this, emailMessage));
		} catch (FileSystemException e) {
			throw e;
		} catch (Exception e) {
			throw new FileSystemException(e);
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
	public EmailMessage moveFile(EmailMessage f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		ExchangeObjectReference reference = asObjectReference(destinationFolder);
		ExchangeService exchangeService = getConnection(reference);
		boolean invalidateConnectionOnRelease = false;
		try {
			FolderId destinationFolderId = findFolder(exchangeService, reference, createFolder);
			return (EmailMessage)f.move(destinationFolderId);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}

	@Override
	public EmailMessage copyFile(EmailMessage f, String destinationFolder, boolean createFolder, boolean resultantMustBeReturned) throws FileSystemException {
		ExchangeObjectReference reference = asObjectReference(destinationFolder);
		ExchangeService exchangeService = getConnection(reference);
		boolean invalidateConnectionOnRelease = false;
		try {
			FolderId destinationFolderId = findFolder(exchangeService, reference, createFolder);
			return (EmailMessage)f.copy(destinationFolderId);
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
	public String getParentFolder(EmailMessage f) throws FileSystemException {
		ExchangeService exchangeService = getConnection();
		boolean invalidateConnectionOnRelease = false;
		try {
			FolderId folderId = f.getParentFolderId();
			Folder folder = Folder.bind(exchangeService, folderId);
			return folder.getDisplayName();
		} catch(Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
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
		EmailMessage emailMessage = f;
		try {
			if (emailMessage.getId()!=null) {
				emailMessage.load(PropertySet.FirstClassProperties);
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
							values = new LinkedList<>();
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
			throw new FileSystemException(e);
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
		List<Attachment> result = new LinkedList<Attachment>();
		EmailMessage emailMessage = f;
		try {
			if (emailMessage.getId()!=null) {
				PropertySet ps = new PropertySet(EmailMessageSchema.Attachments);
				emailMessage.load(ps);
			}
			AttachmentCollection attachmentCollection = emailMessage.getAttachments();
			for (Attachment attachment : attachmentCollection) {
				result.add(attachment);
			}
			return result.iterator();
		} catch (Exception e) {
			throw new FileSystemException("cannot read attachments",e);
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



	@Override
	public void createFolder(String folderName) throws FileSystemException {
		ExchangeObjectReference reference = asObjectReference(folderName);
		ExchangeService exchangeService = getConnection(reference);
		boolean invalidateConnectionOnRelease = false;
		try {
			Folder folder = new Folder(exchangeService);
			folder.setDisplayName(reference.getObjectName());
			folder.save(reference.getBaseFolderId());
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException("cannot create folder ["+reference.getObjectName()+"]", e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}


	@Override
	public void removeFolder(String folderName, boolean removeNonEmptyFolder) throws FileSystemException {
		ExchangeObjectReference reference = asObjectReference(folderName);
		ExchangeService exchangeService = getConnection(reference);
		boolean invalidateConnectionOnRelease = false;
		try {
			FolderId folderId = findFolder(exchangeService, reference);
			Folder folder = Folder.bind(exchangeService, folderId);
			if(removeNonEmptyFolder) {
				folder.empty(DeleteMode.HardDelete, true);
			}
			folder.delete(DeleteMode.HardDelete);
		} catch (Exception e) {
			invalidateConnectionOnRelease = true;
			throw new FileSystemException(e);
		} finally {
			releaseConnection(exchangeService, invalidateConnectionOnRelease);
		}
	}


	protected String getFrom(EmailMessage emailMessage) {
		try {
			return cleanAddress(emailMessage.getFrom());
		} catch (ServiceLocalException e) {
			log.warn("Could not get From Address: "+ e.getMessage());
			return null;
		}
	}

	protected String getReceivedBy(EmailMessage emailMessage) throws ServiceResponseException, FileSystemException {
		try {
			emailMessage.load(PropertySet.FirstClassProperties);
			EmailAddress receivedBy = emailMessage.getReceivedBy();
			if (receivedBy == null) {
				SoapFaultDetails soapFaultDetails = new SoapFaultDetails() {
					@Override
					public ServiceError getResponseCode() {
						return ServiceError.ErrorItemNotFound;
					}
				};
				throw new ServiceResponseException(new ServiceResponse(soapFaultDetails));
			}
			return receivedBy.getAddress();
		} catch (ServiceResponseException e) {
			ServiceError errorCode = e.getErrorCode();
			if (errorCode == ServiceError.ErrorItemNotFound) {
				throw e;
			}
			throw new FileSystemException(e);
		} catch (Exception e) {
			throw new FileSystemException("Could not extract ReceivedBy address", e);
		}
	}

	protected String getSender(EmailMessage emailMessage) {
		try {
			EmailAddress sender = emailMessage.getSender();
			return sender==null ? null : cleanAddress(sender);
		} catch (ServiceLocalException e) {
			log.warn("Could not get Sender Address: "+ e.getMessage());
			return null;
		}
	}

	protected List<String> getReplyTo(EmailMessage emailMessage) {
		try {
			return asList(emailMessage.getReplyTo());
		} catch (ServiceLocalException e) {
			log.warn("Could not get ReplyTo Addresses: "+ e.getMessage());
			return null;
		}
	}

	protected Date getDateTimeSent(EmailMessage emailMessage) {
		try {
			return emailMessage.getDateTimeSent();
		} catch (ServiceLocalException e) {
			log.warn("Could not get DateTimeSent: "+ e.getMessage());
			return null;
		}
	}

	protected Date getDateTimeReceived(EmailMessage emailMessage) {
		try {
			return emailMessage.getDateTimeReceived();
		} catch (ServiceLocalException e) {
			log.warn("Could not get getDateTimeReceived: "+ e.getMessage());
			return null;
		}
	}

	@Override
	public String getSubject(EmailMessage emailMessage) throws FileSystemException {
		ItemId id=null;
		try {
			id = emailMessage.getId();
			if (id!=null) { // attachments don't have an id, but appear to be loaded at the same time as the main message
				emailMessage.load(new PropertySet(ItemSchema.Subject));
			}
			return emailMessage.getSubject();
		} catch (Exception e) {
			if (id==null) {
				log.warn("Could not get Subject for message without ItemId", e);
				return null;
			}
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

	private void setMailboxOnService(ExchangeService service, String mailbox){
		service.getHttpHeaders().put(ANCHOR_HEADER, mailbox);

		// only set impersonated user in oauth situation
		if (client != null) {
			service.setImpersonatedUserId(new ImpersonatedUserId(ConnectingIdType.SmtpAddress, mailbox));
		}
	}

	private ExchangeObjectReference asObjectReference(String objectName) {
		return asObjectReference(objectName, basefolderId);
	}

	private ExchangeObjectReference asObjectReference(String objectName, FolderId baseFolderId){
		ExchangeObjectReference reference = new ExchangeObjectReference(objectName, getMailAddress(), baseFolderId, getMailboxObjectSeparator());
		if(!reference.isStatic()){
			reference.setBaseFolderId(baseFolderId);
		}
		return reference;
	}

	private ExchangeService getConnection(ExchangeObjectReference reference) throws FileSystemException {
		ExchangeService service = super.getConnection();
		setMailboxOnService(service, reference.getMailbox());
		return service;
	}

	@Override
	protected void releaseConnection(ExchangeService service, boolean invalidateConnectionOnRelease){
		service.getHttpHeaders().remove(ANCHOR_HEADER);
		super.releaseConnection(service, invalidateConnectionOnRelease);
	}

	private static class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {

		@Override
		public boolean autodiscoverRedirectionUrlValidationCallback(String redirectionUrl) {
			return redirectionUrl.toLowerCase().startsWith("https://"); //TODO: provide better test on how to trust this url
		}
	}

	@IbisDoc({"The mail address of the mailbox connected to (also used for auto discovery)", ""})
	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}

	@IbisDoc({"When <code>true</code>, all redirect uris are accepted when connecting to the server", "true"})
	public void setValidateAllRedirectUrls(boolean validateAllRedirectUrls) {
		this.validateAllRedirectUrls = validateAllRedirectUrls;
	}

	@IbisDoc({"Url of the Exchange server. Set to e.g. https://outlook.office365.com/EWS/Exchange.asmx to speed up start up, leave empty to use autodiscovery", ""})
	public void setUrl(String url) {
		this.url = url;
	}


	@IbisDoc({"Client ID that represents a registered application in Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Overview.", ""})
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	@IbisDoc({"Client secret that belongs to registered application in Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Certificates and Secrets", ""})
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	@IbisDoc({"Tenant ID that represents the tenant in which the registered application exists within Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Overview.", ""})
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	@IbisDoc({"Alias used to obtain client ID and secret or username and password for authentication to Exchange mail server. " +
			"If the attribute tenantId is empty, the deprecated Basic Authentication method is used. " +
			"If the attribute tenantId is not empty, the username and password are treated as the client ID and secret.", ""})
	@Override
	public void setAuthAlias(String authAlias) {
		super.setAuthAlias(authAlias);
	}

	@IbisDoc({"Username for authentication to Exchange mail server. Ignored when tenantId is also specified", ""})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret!")
	@Override
	public void setUsername(String username) {
		super.setUsername(username);
		setClientId(username);
	}

	@IbisDoc({"Password for authentication to Exchange mail server. Ignored when tenantId is also specified", ""})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret!")
	@Override
	public void setPassword(String password) {
		super.setPassword(password);
		setClientSecret(password);
	}

	@IbisDoc({"If empty, all mails are retrieved. If set to <code>NDR</code> only Non-Delivery Report mails ('bounces') are retrieved", ""})
	public void setFilter(String filter) {
		this.filter = filter;
	}


	@IbisDoc({"proxy host", ""})
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	@IbisDoc({"proxy port", "8080"})
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	@IbisDoc({"proxy username", ""})
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	@IbisDoc({"proxy password", ""})
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	@IbisDoc({"proxy authAlias", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}

	@IbisDoc({"proxy domain", ""})
	public void setProxyDomain(String proxyDomain) {
		this.proxyDomain = proxyDomain;
	}

	/**
	 * Separator character used when working with multiple mailboxes, specified before the separator in the object name <code>test@organisation.com|My sub folder</code> or <code>test@organisation.com|AAMkADljZDMxYzIzLTFlMjYtNGY4Mi1hM2Y1LTc2MjE5ZjIyZmMyNABGAAAAAAAu/9EmV5M6QokBRZwID1Q6BwDXQXY+F44hRbDfTB9v8jRfAAAEUqUVAADXQXY+F44hRbDfTB9v8jRfAAKA4F+pAAA=</code>.
	 * Please consider when moving emails across mailboxes that there will be a null value returned instead of the newly created identifier.
	 * @ff.default |
	 */
	public void setMailboxObjectSeparator(String separator) {
		this.mailboxObjectSeparator = separator;
	}
}
