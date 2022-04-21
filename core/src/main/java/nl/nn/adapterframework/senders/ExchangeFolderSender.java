/*
   Copyright 2019 Integration Partners, 2020 WeAreFrank!

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

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.ExchangeFileSystem;
import nl.nn.adapterframework.filesystem.FileSystemSender;
/**
 * Implementation of a {@link FileSystemSender} that enables to manipulate messages in a Exchange folder.
 *
 * @author Gerrit van Brakel
 */
public class ExchangeFolderSender extends FileSystemSender<EmailMessage,ExchangeFileSystem> {

	public final String EXCHANGE_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ExchangeFileSystem";

	public ExchangeFolderSender() {
		setFileSystem(new ExchangeFileSystem());
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setMailAddress(String mailAddress) {
		getFileSystem().setMailAddress(mailAddress);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}

	@IbisDocRef({ EXCHANGE_FILE_SYSTEM})
	public void setClientId(String clientId) {
		getFileSystem().setClientId(clientId);
	}

	@IbisDocRef({ EXCHANGE_FILE_SYSTEM})
	public void setClientSecret(String clientSecret) {
		getFileSystem().setClientSecret(clientSecret);
	}

	@IbisDocRef({ EXCHANGE_FILE_SYSTEM})
	public void setTenantId(String tenantId) {
		getFileSystem().setTenantId(tenantId);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret. N.B. username no longer defaults to mailaddress")
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to modern authentication using clientId and clientSecret")
	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String proxyUsername) {
		setProxyUsername(proxyUsername);
	}
	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDocRef({EXCHANGE_FILE_SYSTEM})
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

}
