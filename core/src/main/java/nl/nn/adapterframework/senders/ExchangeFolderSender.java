/*
   Copyright 2019 Integration Partners

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

import microsoft.exchange.webservices.data.core.service.item.Item;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.ExchangeFileSystem;
import nl.nn.adapterframework.filesystem.FileSystemSender;
/**
 * Implementation of a {@link FileSystemSender} that enables to manipulate messages in a Exchange folder.
 * 
 * <p>
 * <b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setMailAddress(String) mailAddress}</td><td>mail address (also used for auto discovery)</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUrl(String) url}</td><td>(only used when mailAddress is empty) url of the service</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for authentication to exchange mail server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilter(String) filter}</td><td>If empty, all mails are retrieved. If 'NDR' only Non-Delivery Report mails ('bounces') are retrieved</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFolder(String) inputFolder}</td><td>folder (subfolder of inbox) to look for mails. If empty, the inbox folder is used</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel
 */
public class ExchangeFolderSender extends FileSystemSender<Item,ExchangeFileSystem> implements HasPhysicalDestination {

	public final String EXCHANGE_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ExchangeFileSystem";

	public ExchangeFolderSender() {
		setFileSystem(new ExchangeFileSystem());
	}

	@IbisDocRef({"1", EXCHANGE_FILE_SYSTEM})
	public void setMailAddress(String mailAddress) {
		getFileSystem().setMailAddress(mailAddress);
	}

	@IbisDocRef({"3", EXCHANGE_FILE_SYSTEM})
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}
	

	@IbisDocRef({"4", EXCHANGE_FILE_SYSTEM})
	public void setAccessToken(String accessToken) {
		getFileSystem().setAccessToken(accessToken);
	}

	@IbisDocRef({"5", EXCHANGE_FILE_SYSTEM})
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDocRef({"6", EXCHANGE_FILE_SYSTEM})
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@IbisDocRef({"7", EXCHANGE_FILE_SYSTEM})
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@IbisDocRef({"8", EXCHANGE_FILE_SYSTEM})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDocRef({"9", EXCHANGE_FILE_SYSTEM})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDocRef({"10", EXCHANGE_FILE_SYSTEM})
	public void setProxyUserName(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@IbisDocRef({"11", EXCHANGE_FILE_SYSTEM})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDocRef({"12", EXCHANGE_FILE_SYSTEM})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDocRef({"13", EXCHANGE_FILE_SYSTEM})
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

}
