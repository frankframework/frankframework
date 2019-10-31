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
import nl.nn.adapterframework.doc.IbisDoc;
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
 * <tr><td>{@link #setUsername(String) username}</td><td>username used in authentication to exchange mail server</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>&nbsp;</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setFilter(String) filter}</td><td>If empty, all mails are retrieved. If 'NDR' only Non-Delivery Report mails ('bounces') are retrieved</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setInputFolder(String) inputFolder}</td><td>folder (subfolder of inbox) to look for mails. If empty, the inbox folder is used</td><td>&nbsp;</td></tr>
 * </table>
 * </p>
 * 
 * @author Gerrit van Brakel
 */
public class ExchangeFolderSender extends FileSystemSender<Item,ExchangeFileSystem> implements HasPhysicalDestination {

	public ExchangeFolderSender() {
		setFileSystem(new ExchangeFileSystem());
	}

	@IbisDoc({"1", "mail address (also used for auto discovery)", "" })
	public void setMailAddress(String mailaddress) {
		getFileSystem().setMailAddress(mailaddress);
	}

	@IbisDoc({"2", "(only used when mailAddress is empty) url of the service", "" })
	public void setUrl(String url) {
		getFileSystem().setUrl(url);
	}

	@IbisDoc({"3", "username used in authentication to exchange mail server", "" })
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@IbisDoc({"4", "password used in authentication to exchange mail server", "" })
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	@IbisDoc({"5", "alias used to obtain credentials for authentication to exchange mail server", "" })
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}


	@IbisDoc({"7", "folder (subfolder of root or of inbox) to look for mails. If empty, the inbox folder is used", ""})
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}

	@IbisDoc({"8", "If empty, all mails are retrieved. If set to <code>NDR</code> only Non-Delivery Report mails ('bounces') are retrieved", ""})
	public void setFilter(String filter) {
		getFileSystem().setFilter(filter);
	}

	@IbisDoc({"9", "proxy host", ""})
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDoc({"10", "proxy port", ""})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDoc({"11", "proxy username", ""})
	public void setProxyUserName(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}

	@IbisDoc({"12", "proxy password", ""})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDoc({"12", "proxy authAlias", ""})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDoc({"13", "proxy domain", ""})
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

}
