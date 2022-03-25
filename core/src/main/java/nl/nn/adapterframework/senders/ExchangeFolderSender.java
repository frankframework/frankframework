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

import lombok.Getter;
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

	private final @Getter String domain = "Exchange";
	public final String EXCHANGE_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ExchangeFileSystem";

	public ExchangeFolderSender() {
		setFileSystem(new ExchangeFileSystem());
	}

	@IbisDocRef({"1", EXCHANGE_FILE_SYSTEM})
	public void setMailAddress(String mailAddress) {
		getFileSystem().setMailAddress(mailAddress);
	}

	@IbisDocRef({"2", EXCHANGE_FILE_SYSTEM})
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}

	@IbisDocRef({"3", EXCHANGE_FILE_SYSTEM})
	public void setAccessToken(String accessToken) {
		getFileSystem().setAccessToken(accessToken);
	}

	@IbisDocRef({"4", EXCHANGE_FILE_SYSTEM})
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to authentication using an accessToken. N.B. username no longer defaults to mailaddress")
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}
	
	@Deprecated
	@ConfigurationWarning("Authentication to Exchange Web Services with username and password will be disabled 2021-Q3. Please migrate to authentication using an accessToken")
	@IbisDocRef({"5", EXCHANGE_FILE_SYSTEM})
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@IbisDocRef({"6", EXCHANGE_FILE_SYSTEM})
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDocRef({"7", EXCHANGE_FILE_SYSTEM})
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@IbisDocRef({"8", EXCHANGE_FILE_SYSTEM})
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@IbisDocRef({"9", EXCHANGE_FILE_SYSTEM})
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}
	
	@IbisDocRef({"10", EXCHANGE_FILE_SYSTEM})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDocRef({"11", EXCHANGE_FILE_SYSTEM})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDocRef({"12", EXCHANGE_FILE_SYSTEM})
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String proxyUsername) {
		setProxyUsername(proxyUsername);
	}
	@IbisDocRef({"13", EXCHANGE_FILE_SYSTEM})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDocRef({"14", EXCHANGE_FILE_SYSTEM})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDocRef({"15", EXCHANGE_FILE_SYSTEM})
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

}
