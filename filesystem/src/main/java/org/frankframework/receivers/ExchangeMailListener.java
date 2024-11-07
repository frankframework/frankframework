/*
   Copyright 2016, 2019 Nationale-Nederlanden, 2020-2024 WeAreFrank!

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
package org.frankframework.receivers;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarning;
import org.frankframework.doc.Category;
import org.frankframework.doc.ReferTo;
import org.frankframework.encryption.HasKeystore;
import org.frankframework.encryption.HasTruststore;
import org.frankframework.encryption.KeystoreType;
import org.frankframework.filesystem.ExchangeAttachmentReference;
import org.frankframework.filesystem.ExchangeFileSystem;
import org.frankframework.filesystem.ExchangeMessageReference;
import org.frankframework.filesystem.AbstractMailFileSystem;
import org.frankframework.filesystem.AbstractMailListener;

/**
 * Microsoft Exchange Implementation of a mail filesystem.
 *
 * @author Gerrit van Brakel
 */
@Category(Category.Type.ADVANCED)
public class ExchangeMailListener extends AbstractMailListener<ExchangeMessageReference, ExchangeAttachmentReference,ExchangeFileSystem> {

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		String separator = getFileSystem().getMailboxObjectSeparator();
		if (StringUtils.isNotEmpty(getInputFolder()) && getInputFolder().contains(separator) ||
			StringUtils.isNotEmpty(getInProcessFolder()) && getInProcessFolder().contains(separator)){
			throw new ConfigurationException("Moving items across mailboxes is not supported by ExchangeMailListener for attributes [inputFolder,inProcessFolder]. " +
				"Please do not use dynamic mailboxes / folders separated by ["+separator+"].");
		}
	}

	@Override
	protected ExchangeFileSystem createFileSystem() {
		log.debug("Creating new ExchangeFileSystem");
		return new ExchangeFileSystem();
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

	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret. N.B. username no longer defaults to mailaddress")
	@ReferTo(ExchangeFileSystem.class)
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret.")
	@ReferTo(ExchangeFileSystem.class)
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@ReferTo(AbstractMailFileSystem.class)
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@ReferTo(ExchangeFileSystem.class)
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@ReferTo(AbstractMailFileSystem.class)
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
	@ReferTo(HasKeystore.class)
	public void setKeystore(String keystore) {
		getFileSystem().setKeystore(keystore);
	}
	@ReferTo(HasKeystore.class)
	public void setKeystoreType(KeystoreType keystoreType) {
		getFileSystem().setKeystoreType(keystoreType);
	}
	@ReferTo(HasKeystore.class)
	public void setKeystoreAuthAlias(String keystoreAuthAlias) {
		getFileSystem().setKeystoreAuthAlias(keystoreAuthAlias);
	}
	@ReferTo(HasKeystore.class)
	public void setKeystorePassword(String keystorePassword) {
		getFileSystem().setKeystorePassword(keystorePassword);
	}
	@ReferTo(HasKeystore.class)
	public void setKeyManagerAlgorithm(String keyManagerAlgorithm) {
		getFileSystem().setKeyManagerAlgorithm(keyManagerAlgorithm);
	}
	@ReferTo(HasKeystore.class)
	public void setKeystoreAlias(String keystoreAlias) {
		getFileSystem().setKeystoreAlias(keystoreAlias);
	}
	@ReferTo(HasKeystore.class)
	public void setKeystoreAliasAuthAlias(String keystoreAliasAuthAlias) {
		getFileSystem().setKeystoreAliasAuthAlias(keystoreAliasAuthAlias);
	}
	@ReferTo(HasKeystore.class)
	public void setKeystoreAliasPassword(String keystoreAliasPassword) {
		getFileSystem().setKeystoreAliasPassword(keystoreAliasPassword);
	}

	@ReferTo(HasTruststore.class)
	public void setTruststore(String truststore) {
		getFileSystem().setTruststore(truststore);
	}
	@ReferTo(HasTruststore.class)
	public void setTruststoreType(KeystoreType truststoreType) {
		getFileSystem().setTruststoreType(truststoreType);
	}
	@ReferTo(HasTruststore.class)
	public void setTruststoreAuthAlias(String truststoreAuthAlias) {
		getFileSystem().setTruststoreAuthAlias(truststoreAuthAlias);
	}
	@ReferTo(HasTruststore.class)
	public void setTruststorePassword(String truststorePassword) {
		getFileSystem().setTruststorePassword(truststorePassword);
	}
	@ReferTo(HasTruststore.class)
	public void setTrustManagerAlgorithm(String trustManagerAlgorithm) {
		getFileSystem().setTrustManagerAlgorithm(trustManagerAlgorithm);
	}
	@ReferTo(HasTruststore.class)
	public void setVerifyHostname(boolean verifyHostname) {
		getFileSystem().setVerifyHostname(verifyHostname);
	}
	@ReferTo(HasTruststore.class)
	public void setAllowSelfSignedCertificates(boolean allowSelfSignedCertificates) {
		getFileSystem().setAllowSelfSignedCertificates(allowSelfSignedCertificates);
	}
	@ReferTo(HasTruststore.class)
	public void setIgnoreCertificateExpiredException(boolean ignoreCertificateExpiredException) {
		getFileSystem().setIgnoreCertificateExpiredException(ignoreCertificateExpiredException);
	}

}
