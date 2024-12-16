/*
   Copyright 2019-2024 WeAreFrank!

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
package org.frankframework.filesystem.exchange;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.filesystem.AbstractFileSystem;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.FileSystemUtils;
import org.frankframework.filesystem.IBasicFileSystem;
import org.frankframework.filesystem.MsalClientAdapter;
import org.frankframework.filesystem.MsalClientAdapter.GraphClient;
import org.frankframework.filesystem.TypeFilter;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
import org.frankframework.util.CredentialFactory;
import org.frankframework.util.DateFormatUtils;
import org.frankframework.util.SpringUtils;
import org.frankframework.util.StringUtil;

/**
 * Implementation of a {@link IBasicFileSystem} of an Exchange Mailbox.
 * <br/>
 * To make use of oauth authentication:
 * <ol>
 *     	<li>Create an application in Azure AD -> App Registrations. For more information please read {@link "https://learn.microsoft.com/en-us/exchange/client-developer/exchange-web-services/how-to-authenticate-an-ews-application-by-using-oauth"}</li>
 *     	<li>Request the required API permissions within desired scope <code>https://outlook.office365.com/</code> in Azure AD -> App Registrations -> MyApp -> API Permissions.</li>
 *     	<li>Create a secret for your application in Azure AD -> App Registrations -> MyApp -> Certificates and Secrets</li>
 *     	<li>Configure the clientSecret directly as password or as the password of a JAAS entry referred to by authAlias. Only available upon creation of your secret in the previous step.</li>
 *     	<li>Configure the clientId directly as username or as the username of a JAAS entry referred to by authAlias which could be retrieved from Azure AD -> App Registrations -> MyApp -> Overview</li>
 *     	<li>Configure the tenantId which could be retrieved from Azure AD -> App Registrations -> MyApp -> Overview</li>
 * 		<li>Make sure your application is able to reach <code>https://login.microsoftonline.com</code>. Required for token retrieval. </li>
 * </ol>
 */
public class ExchangeFileSystem extends AbstractFileSystem<MailItemId> implements HasKeystore, HasTruststore {
	private final @Getter String domain = "Exchange";
	private final @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;
	private final @Getter String name = "ExchangeFileSystem";

	private @Getter String filter;
	private @Getter String mailAddress;
	private @Getter String url;

	private @Getter String proxyHost = null;
	private @Getter int proxyPort = 8080;
	private @Getter String proxyUsername = null;
	private @Getter String proxyPassword = null;
	private @Getter String proxyAuthAlias = null;
	private @Getter String proxyDomain = null;

	private @Getter String authAlias = null;
	private @Getter String clientId = null;
	private @Getter String clientSecret = null;
	private @Getter String tenantId = null;
	private @Getter String baseFolder;

	/* SSL */
	private @Getter @Setter String keystore;
	private @Getter @Setter String keystoreAuthAlias;
	private @Getter @Setter String keystorePassword;
	private @Getter @Setter KeystoreType keystoreType = KeystoreType.PKCS12;
	private @Getter @Setter String keystoreAlias;
	private @Getter @Setter String keystoreAliasAuthAlias;
	private @Getter @Setter String keystoreAliasPassword;
	private @Getter @Setter String keyManagerAlgorithm = null;

	private @Getter @Setter String truststore = null;
	private @Getter @Setter String truststoreAuthAlias;
	private @Getter @Setter String truststorePassword = null;
	private @Getter @Setter KeystoreType truststoreType = KeystoreType.JKS;
	private @Getter @Setter String trustManagerAlgorithm = null;
	private @Getter @Setter boolean allowSelfSignedCertificates = false;
	private @Getter @Setter boolean verifyHostname = true;
	private @Getter @Setter boolean ignoreCertificateExpiredException = false;
	private @Getter @Setter boolean enableConnectionTracing = false;

	private @Getter CredentialFactory credentials = null;
	private @Getter CredentialFactory proxyCredentials = null;

	// Configured fields
	private MsalClientAdapter msalClientAdapter;
	private GraphClient client;
	private MailFolder mailFolder;

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isNotEmpty(getFilter())) {
			if (!getFilter().equalsIgnoreCase("NDR")) {
				throw new ConfigurationException("illegal value for filter [" + getFilter() + "], must be 'NDR' or empty");
			}
		}
		if (StringUtils.isEmpty(getUrl()) && StringUtils.isEmpty(getMailAddress())) {
			throw new ConfigurationException("either url or mailAddress needs to be specified");
		}

		credentials = new CredentialFactory(getAuthAlias(), getClientId(), getClientSecret());
		if (StringUtils.isNotEmpty(getProxyHost()) && (StringUtils.isNotEmpty(getProxyAuthAlias()) || StringUtils.isNotEmpty(getProxyUsername()) || StringUtils.isNotEmpty(getProxyPassword()))) {
			proxyCredentials = new CredentialFactory(getProxyAuthAlias(), getProxyUsername(), getProxyPassword());
		}

		msalClientAdapter = SpringUtils.createBean(applicationContext, MsalClientAdapter.class);
		msalClientAdapter.setProxyHost(getProxyHost());
		msalClientAdapter.setProxyPort(getProxyPort());
		CredentialFactory proxyCf = getProxyCredentials();
		if (proxyCf != null) {
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

		if (StringUtils.isBlank(baseFolder)) {
			baseFolder = "Inbox";
		}
	}

	@Override
	public void open() {
		try {
			super.open();

			msalClientAdapter.start();
			client = msalClientAdapter.greateGraphClient(tenantId, getCredentials());

			List<String> folder = StringUtil.split(baseFolder, "/");
			if(folder.size() > 2) throw new LifecycleException("for now only 2 levels deep are supported"); //TODO

			String rootFolder = folder.remove(0);
			List<MailFolder> folders = client.getMailFolders(mailAddress);
			MailFolder mailFolder = folders.stream()
					.filter(t -> rootFolder.equalsIgnoreCase(t.getName()))
					.findFirst()
					.orElseThrow(() -> {
				throw new LifecycleException("unable to find folder [%s] in mailbox [%s]".formatted(rootFolder, mailAddress));
			});
			log.trace("found id [{}] beloging to rootFolder [{}]", mailFolder.getId(), rootFolder);

			if (folder.size() == 0) {
				this.mailFolder = mailFolder;
			} else {
				this.mailFolder = findSubFolder(mailFolder, folder.get(0));
			}
		} catch (FileSystemException | IOException e) {
			throw new LifecycleException("Failed to initialize Microsoft Authentication client.", e);
		}
	}

	private MailFolder findSubFolder(MailFolder parentFolder, String childFolderName) throws FileSystemException {
		List<String> folderNames = StringUtil.split(childFolderName, "/");
		if (folderNames.isEmpty()) {
			throw new FileSystemException("unable to find folder, no name specified");
		}

		try {
			String folderToLookFor = folderNames.remove(0);
			log.trace("attempt to find sub folder [{}] in parent folder [{}]", parentFolder, folderToLookFor);
			List<MailFolder> subFolders = client.getMailFolders(parentFolder);
			MailFolder folder = findSubFolder(subFolders, folderToLookFor);

			// No more sub-folders, we have found what we're looking for
			if (folderNames.isEmpty()) {
				return folder;
			}

			// More sub-folders to find, repeat our search
			return findSubFolder(folder, String.join(",", folderNames));
		} catch (IOException e) {
			throw new FileSystemException("unable to find folder ["+parentFolder+"]", e);
		}
	}

	private MailFolder findSubFolder(List<MailFolder> childFolders, String childFolderName) throws FileSystemException {
		for (MailFolder mailFolder : childFolders) {
			if (childFolderName.equalsIgnoreCase(mailFolder.getName())) {
				log.debug("found id [{}] beloging to subFolder [{}]", mailFolder::getId, ()->childFolderName);
				return mailFolder;
			}
		}
		throw new FileSystemException("unable to find sub-folder [%s] in mailbox [%s]".formatted(childFolderName, mailAddress));
	}

	@Override
	public void close() throws FileSystemException {
		super.close();
		if (msalClientAdapter != null) {
			msalClientAdapter.stop();
		}
	}

	/**
	 * The mail address of the mailbox connected to (also used for auto discovery)
	 */
	public void setMailAddress(String mailAddress) {
		this.mailAddress = mailAddress;
	}

	/** Folder (subfolder of root or of inbox) to look for mails. If empty, the inbox folder is used */
	public void setBaseFolder(String baseFolder) {
		this.baseFolder = baseFolder;
	}

	/**
	 * Client ID that represents a registered application in Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Overview.
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * Client secret that belongs to registered application in Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Certificates and Secrets
	 */
	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

	/**
	 * Tenant ID that represents the tenant in which the registered application exists within Azure AD which could be found at Azure AD -> App Registrations -> MyApp -> Overview.
	 */
	public void setTenantId(String tenantId) {
		this.tenantId = tenantId;
	}

	/**
	 * Alias used to obtain client ID and secret or username and password for authentication to Exchange mail server.
	 * If the attribute tenantId is empty, the deprecated Basic Authentication method is used.
	 * If the attribute tenantId is not empty, the username and password are treated as the client ID and secret.
	 */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/**
	 * If empty, all mails are retrieved. If set to <code>NDR</code> only Non-Delivery Report mails ('bounces') are retrieved
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}


	/** proxy host */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/**
	 * proxy port
	 *
	 * @ff.default 8080
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/** proxy username */
	public void setProxyUsername(String proxyUsername) {
		this.proxyUsername = proxyUsername;
	}

	/** proxy password */
	public void setProxyPassword(String proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	/** proxy authAlias */
	public void setProxyAuthAlias(String proxyAuthAlias) {
		this.proxyAuthAlias = proxyAuthAlias;
	}

	/** proxy domain */
	public void setProxyDomain(String proxyDomain) {
		this.proxyDomain = proxyDomain;
	}

	@Override
	public int getNumberOfFilesInFolder(String folder) throws FileSystemException {
		MailFolder f = findSubFolder(mailFolder, folder);
		return f.getTotalItemCount();
	}

	@Override
	public DirectoryStream<MailItemId> list(String folderName, TypeFilter filter) throws FileSystemException {
		List<MailItemId> items = new ArrayList<>();
		try {
			MailFolder folder = StringUtils.isBlank(folderName) ? mailFolder : findSubFolder(mailFolder, folderName);
			if (filter.includeFolders()) {
				List<MailFolder> folders = client.getMailFolders(folder);
				items.addAll(folders);
			}
			if (filter.includeFiles()) {
				List<MailMessage> messages = client.getMailMessages(folder);
				items.addAll(messages);
			}

			return FileSystemUtils.getDirectoryStream(items);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getName(MailItemId msg) {
		if (msg instanceof MailFolder folder) {
			return folder.getName();
		}

		return msg.getId();
	}

	@Override
	public String getParentFolder(MailItemId msg) throws FileSystemException {
		MailFolder parentMailFolder = msg.getMailFolder();
		if (parentMailFolder == null) {
			throw new FileSystemException("unknown");
		}
		return parentMailFolder.getName();
	}

	@Override
	public MailItemId toFile(String id) throws FileSystemException {
		if (StringUtils.isBlank(id)) {
			throw new FileSystemException("no id or folder name provided");
		}

		// Folder
		if (id.contains("/")) {
			log.trace("assuming id [{}] is a folder", id);
			return toFile(id, null);
		}

		// File
		log.trace("assuming id [{}] is a file", id);
		return toFile(null, id);
	}

	@Override
	public MailItemId toFile(String childFolderName, String id) throws FileSystemException {
		MailFolder folder = StringUtils.isBlank(childFolderName) ? mailFolder : findSubFolder(mailFolder, childFolderName);

		if (StringUtils.isBlank(id)) {
			return folder;
		}

		return new MailMessage(folder, id);
	}

	@Override
	public boolean exists(MailItemId file) throws FileSystemException {
		try {
			if (file instanceof MailMessage message) {
				return client.getMailMessage(message) != null;
			} else if (file instanceof MailFolder folder) {
				return client.getMailFolders(folder) != null;
			} else {
				return false;
			}
		} catch (IOException e) {
			return false;
		}
	}

	@Override
	public boolean isFolder(MailItemId file) throws FileSystemException {
		return file instanceof MailFolder;
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		return findSubFolder(mailFolder, folder) != null;
	}

	@Override
	public Message readFile(MailItemId file, String charset) throws FileSystemException, IOException {
		MailMessage msg = client.getMailMessage(getMailMessage(file));
		return new Message(msg.getBody().getContent(), new MessageContext().withCharset(charset));
	}

	/** resolves mail message, turns a pointer to an actual mail item. */
	private MailMessage getMailMessage(MailItemId id) throws FileSystemException {
		if (id instanceof MailMessage mailMessage) {
			try {
				if (StringUtils.isNotBlank(mailMessage.getParentFolderId())) {
					return mailMessage;
				}

				// else update the message
				return client.getMailMessage(mailMessage);
			} catch (IOException e) {
				throw new FileSystemException("message not found", e);
			}
		}

		throw new FileSystemException("item is not a mail message");
	}

	@Override
	public void deleteFile(MailItemId id) throws FileSystemException {
		try {
			MailMessage mailMessage = getMailMessage(id);
			client.deleteMailMessage(mailMessage);
		} catch (IOException e) {
			throw new FileSystemException("unable to delete message", e);
		}
	}

	@Override
	public MailMessage moveFile(MailItemId file, String destinationFolder, boolean createFolder) throws FileSystemException {
		MailMessage mailMessage = getMailMessage(file);
		MailFolder mailDestinationFolder = findSubFolder(mailFolder, destinationFolder);
		try {
			return client.moveMailMessage(mailMessage, mailDestinationFolder);
		} catch (IOException e) {
			throw new FileSystemException("unable to move message", e);
		}
	}

	@Override
	public MailMessage copyFile(MailItemId file, String destinationFolder, boolean createFolder) throws FileSystemException {
		MailMessage mailMessage = getMailMessage(file);
		MailFolder mailDestinationFolder = findSubFolder(mailFolder, destinationFolder);
		try {
			return client.copyMailMessage(mailMessage, mailDestinationFolder);
		} catch (IOException e) {
			throw new FileSystemException("unable to copy message", e);
		}
	}

	@Override
	public void createFolder(String folder) throws FileSystemException {
		client.createMailFolder(mailFolder, folder);
	}

	@Override
	public void removeFolder(String folder, boolean removeNonEmptyFolder) throws FileSystemException {
		client.deleteMailFolder(mailFolder, folder);
	}

	@Override
	public long getFileSize(MailItemId file) throws FileSystemException {
		return getMailMessage(file).getBody().getContent().length();
	}

	@Override
	public String getCanonicalName(MailItemId file) throws FileSystemException {
		return file.getMailFolder().getUrl();
	}

	@Override
	public Date getModificationTime(MailItemId file) throws FileSystemException {
		MailMessage mailMessage = getMailMessage(file);
		return toDate(mailMessage.getLastModifiedDateTime());
	}

	/**
	 * @param timestamp format: 2024-12-10T13:18:03Z
	 */
	private Date toDate(String timestamp) {
		Objects.requireNonNull(timestamp, "timestamp may not be null");
		Instant instant = DateFormatUtils.parseToInstant(timestamp, DateFormatUtils.FULL_ISO_FORMATTER);
		return Date.from(instant);
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(MailItemId f) throws FileSystemException {
		return null;
	}

	@Override
	public String getPhysicalDestinationName() {
		return mailFolder.getUrl();
	}
}
