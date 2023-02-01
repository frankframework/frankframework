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
package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.encryption.KeystoreType;
import nl.nn.adapterframework.filesystem.ExchangeFileSystem;
import nl.nn.adapterframework.filesystem.ExchangeMessageReference;
import nl.nn.adapterframework.filesystem.FileSystemSender;
/**
 * Implementation of a {@link FileSystemSender} that enables to manipulate messages in an Exchange folder.
 *
 * @author Gerrit van Brakel
 */
public class ExchangeFolderSender extends FileSystemSender<ExchangeMessageReference,ExchangeFileSystem> {

	public final String EXCHANGE_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ExchangeFileSystem";

	public ExchangeFolderSender() {
		setFileSystem(new ExchangeFileSystem());
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setMailAddress(String mailAddress) {
		getFileSystem().setMailAddress(mailAddress);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setClientId(String clientId) {
		getFileSystem().setClientId(clientId);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setClientSecret(String clientSecret) {
		getFileSystem().setClientSecret(clientSecret);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setTenantId(String tenantId) {
		getFileSystem().setTenantId(tenantId);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret. N.B. username no longer defaults to mailaddress")
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret")
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String proxyUsername) {
		setProxyUsername(proxyUsername);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setMailboxObjectSeparator(String separator) {
		getFileSystem().setMailboxObjectSeparator(separator);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystore(String keystore) {
		getFileSystem().setKeystore(keystore);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystoreType(KeystoreType keystoreType) {
		getFileSystem().setKeystoreType(keystoreType);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getFileSystem().setKeystoreAuthAlias(keystoreAuthAlias);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystorePassword(String keystorePassword) {
		getFileSystem().setKeystorePassword(keystorePassword);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystoreAlias(String keystoreAlias) {
		getFileSystem().setKeystoreAlias(keystoreAlias);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getFileSystem().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getFileSystem().setKeystoreAliasPassword(keystoreAliasPassword);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setTruststoreType(KeystoreType truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		getFileSystem().setAllowSelfSignedCertificates(allowSelfSignedCertificates);
	}
	/** @ff.ref nl.nn.adapterframework.filesystem.ExchangeFileSystem */
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		getFileSystem().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}

}
