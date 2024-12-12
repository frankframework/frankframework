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
import java.util.Date;
import java.util.List;
import java.util.Map;

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
import org.frankframework.filesystem.IBasicFileSystem;
import org.frankframework.filesystem.MsalClientAdapter;
import org.frankframework.filesystem.MsalClientAdapter.GraphClient;
import org.frankframework.filesystem.TypeFilter;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.stream.Message;
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
public class ExchangeFileSystem extends AbstractFileSystem<MailMessage> implements HasKeystore, HasTruststore {
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

	private MailFolder findSubFolder(MailFolder parentFolder, String childFolderName) throws IOException {
		List<MailFolder> subFolders = client.getMailFolders(parentFolder);
		MailFolder subMailFolder = subFolders.stream()
				.filter(t -> childFolderName.equalsIgnoreCase(t.getName()))
				.findFirst()
				.orElseThrow(() -> {
			throw new LifecycleException("unable to find sub-folder [%s/%s] in mailbox [%s]".formatted(parentFolder.getName(), childFolderName, mailAddress));
		});
		log.trace("found id [{}] beloging to subFolder [{}]", subMailFolder::getId, parentFolder::getName);
		return subMailFolder;
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
	public DirectoryStream<MailMessage> list(String folder, TypeFilter filter) throws FileSystemException {
		// TODO Auto-generated method stub

		try {
			List<MailMessage> messages = client.getMailMessages(this.mailFolder);
			messages.stream().map(MailMessage::getId).forEach(System.err::println);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String getName(MailMessage msg) {
		return msg.getId();
	}

	@Override
	public String getParentFolder(MailMessage msg) throws FileSystemException {
		return msg.getParentFolderId();
	}

	@Override
	public MailMessage toFile(String id) throws FileSystemException {
		return new MailMessage(mailFolder, id);
	}

	@Override
	public MailMessage toFile(String defaultFolder, String id) throws FileSystemException {
		throw new IllegalStateException();
	}

	@Override
	public boolean exists(MailMessage file) throws FileSystemException {
		try {
			return client.getMailMessage(file) != null;
		} catch (IOException e) {
			throw new FileSystemException(e);
			// or return false?
		}
	}

	@Override
	public boolean isFolder(MailMessage file) throws FileSystemException {
		return false;
	}

	@Override
	public boolean folderExists(String folder) throws FileSystemException {
		try {
			return findSubFolder(mailFolder, folder) != null;
		} catch (IOException e) {
			throw new FileSystemException("unable ", e);
			//or return false?
		}
	}

	@Override
	public Message readFile(MailMessage file, String charset) throws FileSystemException, IOException {
		MailMessage msg = client.getMailMessage(file);
		return new Message(msg.getBody().getContent());
	}

	@Override
	public void deleteFile(MailMessage file) throws FileSystemException {
		client.deleteMailMessage(file);
	}

	@Override
	public MailMessage moveFile(MailMessage f, String destinationFolder, boolean createFolder) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public MailMessage copyFile(MailMessage f, String destinationFolder, boolean createFolder) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
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
	public long getFileSize(MailMessage file) throws FileSystemException {
		return file.getBody().getContent().length();
	}

	@Override
	public String getCanonicalName(MailMessage file) throws FileSystemException {
//		return file.getUrl();
		return null; //full path
	}

	@Override
	public Date getModificationTime(MailMessage file) throws FileSystemException {
		//format: 2024-12-10T13:18:03Z
		Instant instant = DateFormatUtils.parseToInstant(file.getLastModifiedDateTime(), DateFormatUtils.FULL_ISO_FORMATTER);
		return Date.from(instant);
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(MailMessage f) throws FileSystemException {
		return null;
	}

	@Override
	public String getPhysicalDestinationName() {
		return mailFolder.getUrl();
	}
}
