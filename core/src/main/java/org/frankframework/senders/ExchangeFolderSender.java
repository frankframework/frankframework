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
package org.frankframework.senders;

import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.doc.ReferTo;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.filesystem.ExchangeFileSystem;
import org.frankframework.filesystem.ExchangeMessageReference;
import org.frankframework.filesystem.FileSystemSender;
/**
 * Implementation of a {@link FileSystemSender} that enables to manipulate messages in an Exchange folder.
 *
 * @author Gerrit van Brakel
 */
public class ExchangeFolderSender extends FileSystemSender<ExchangeMessageReference,ExchangeFileSystem> {

	public ExchangeFolderSender() {
		setFileSystem(new ExchangeFileSystem());
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setMailAddress(String mailAddress) {
		getFileSystem().setMailAddress(mailAddress);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setClientId(String clientId) {
		getFileSystem().setClientId(clientId);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setClientSecret(String clientSecret) {
		getFileSystem().setClientSecret(clientSecret);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setTenantId(String tenantId) {
		getFileSystem().setTenantId(tenantId);
	}

	@ReferTo(ExchangeFileSystem.class)
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret. N.B. username no longer defaults to mailaddress")
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret")
	@ReferTo(ExchangeFileSystem.class)
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setMailboxObjectSeparator(String separator) {
		getFileSystem().setMailboxObjectSeparator(separator);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setKeystore(String keystore) {
		getFileSystem().setKeystore(keystore);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeystoreType(KeystoreType keystoreType) {
		getFileSystem().setKeystoreType(keystoreType);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getFileSystem().setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeystorePassword(String keystorePassword) {
		getFileSystem().setKeystorePassword(keystorePassword);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeystoreAlias(String keystoreAlias) {
		getFileSystem().setKeystoreAlias(keystoreAlias);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getFileSystem().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getFileSystem().setKeystoreAliasPassword(keystoreAliasPassword);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setTruststoreType(KeystoreType truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		getFileSystem().setAllowSelfSignedCertificates(allowSelfSignedCertificates);
	}
	@ReferTo(ExchangeFileSystem.class)
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		getFileSystem().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}

}
