/*
   Copyright 2016, 2019 Nationale-Nederlanden, 2020 WeAreFrank!

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
package nl.nn.adapterframework.receivers;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import microsoft.exchange.webservices.data.core.service.item.EmailMessage;
import nl.nn.adapterframework.configuration.ConfigurationWarning;
import nl.nn.adapterframework.core.HasPhysicalDestination;
import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.ExchangeFileSystem;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.FileSystemListener;
import nl.nn.adapterframework.filesystem.FileSystemUtils;
import nl.nn.adapterframework.filesystem.IMailFileSystem;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.xml.SaxElementBuilder;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Implementation of a {@link nl.nn.adapterframework.filesystem.FileSystemListener
 * FileSystemListener} that enables a
 * {@link nl.nn.adapterframework.receivers.GenericReceiver} to look in a folder
 * for received mails. When a mail is found, it is moved to an output folder (or
 * it's deleted), so that it isn't found more then once. A xml string with
 * information about the mail is passed to the pipeline.
 * 
 * <p>
 * <b>example:</b> <code><pre>
 *   &lt;email&gt;
 *      &lt;recipients&gt;
 *         &lt;recipient type="to"&gt;***@nn.nl&lt;/recipient&gt;
 *         &lt;recipient type="cc"&gt;***@nn.nl&lt;/recipient&gt;
 *      &lt;/recipients&gt;
 *      &lt;from&gt;***@nn.nl&lt;/from&gt;
 *      &lt;subject&gt;this is the subject&lt;/subject&gt;
 *      &lt;headers&gt;
 *         &lt;header name="prop1"&gt;<i>value of first header property</i>&lt;/header&gt;
 *         &lt;header name="prop2"&gt;<i>value of second header property</i>&lt;/header&gt;
 *      &lt;/headers&gt;
 *      &lt;dateTimeSent&gt;2015-11-18T11:40:19.000+0100&lt;/dateTimeSent&gt;
 *      &lt;dateTimeReceived&gt;2015-11-18T11:41:04.000+0100&lt;/dateTimeReceived&gt;
 *   &lt;/email&gt;
 * </pre></code>
 * </p>
 * 
 * @author Peter Leeuwenburgh, Gerrit van Brakel
 */
public class ExchangeMailListener extends FileSystemListener<EmailMessage,ExchangeFileSystem> implements HasPhysicalDestination {

	public final String EMAIL_MESSAGE_TYPE="email";
	public final String MIME_MESSAGE_TYPE="mime";
	public final String EXCHANGE_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ExchangeFileSystem";
	
	private String storeEmailAsStreamInSessionKey;
	private boolean simple = false;
	
	{
		setMessageType(EMAIL_MESSAGE_TYPE);
		setMessageIdProperty(IMailFileSystem.MAIL_MESSAGE_ID);
	}
	
	@Override
	protected ExchangeFileSystem createFileSystem() {
		return new ExchangeFileSystem();
	}


	@Override
	public Message extractMessage(EmailMessage rawMessage, Map<String,Object> threadContext) throws ListenerException {
		if (MIME_MESSAGE_TYPE.equals(getMessageType())) {
			try {
				return getFileSystem().getMimeContent(rawMessage);
			} catch (FileSystemException e) {
				throw new ListenerException("cannot get MimeContents",e);
			}
		}
		if (!EMAIL_MESSAGE_TYPE.equals(getMessageType())) {
			return super.extractMessage(rawMessage, threadContext);
		}
		XmlWriter writer = new XmlWriter();
		try (SaxElementBuilder emailXml = new SaxElementBuilder("email",writer)) {
			if (isSimple()) {
				FileSystemUtils.addEmailInfoSimple(getFileSystem(), rawMessage, emailXml);
			} else {
				getFileSystem().extractEmail(rawMessage, emailXml);
			}
			if (StringUtils.isNotEmpty(getStoreEmailAsStreamInSessionKey())) {
				Message mimeContent = getFileSystem().getMimeContent(rawMessage);
				threadContext.put(getStoreEmailAsStreamInSessionKey(), mimeContent.asInputStream());
			}
		} catch (SAXException | IOException | FileSystemException e) {
			throw new ListenerException(e);
		}
		return new Message(writer.toString());
	}

	@Deprecated
	@ConfigurationWarning("attribute 'outputFolder' has been replaced by 'processedFolder'")
	public void setOutputFolder(String outputFolder) {
		setProcessedFolder(outputFolder);
	}

	@Deprecated
	@ConfigurationWarning("attribute 'tempFolder' has been replaced by 'inProcessFolder'")
	public void setTempFolder(String tempFolder) {
		setInProcessFolder(tempFolder);
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
	public void setProxyHost(String proxyHost) {
		getFileSystem().setProxyHost(proxyHost);
	}

	@IbisDocRef({"10", EXCHANGE_FILE_SYSTEM})
	public void setProxyPort(int proxyPort) {
		getFileSystem().setProxyPort(proxyPort);
	}

	@IbisDocRef({"11", EXCHANGE_FILE_SYSTEM})
	public void setProxyUsername(String proxyUsername) {
		getFileSystem().setProxyUsername(proxyUsername);
	}
	@Deprecated
	@ConfigurationWarning("Please use \"proxyUsername\" instead")
	public void setProxyUserName(String proxyUsername) {
		setProxyUsername(proxyUsername);
	}
	@IbisDocRef({"12", EXCHANGE_FILE_SYSTEM})
	public void setProxyPassword(String proxyPassword) {
		getFileSystem().setProxyPassword(proxyPassword);
	}

	@IbisDocRef({"13", EXCHANGE_FILE_SYSTEM})
	public void setProxyAuthAlias(String proxyAuthAlias) {
		getFileSystem().setProxyAuthAlias(proxyAuthAlias);
	}

	@IbisDocRef({"14", EXCHANGE_FILE_SYSTEM})
	public void setProxyDomain(String domain) {
		getFileSystem().setProxyDomain(domain);
	}

	
	@IbisDoc({"15", "when set to <code>true</code>, the xml string passed to the pipeline only contains the subject of the mail (to save memory)", ""})
	public void setSimple(boolean b) {
		simple = b;
	}
	public boolean isSimple() {
		return simple;
	}

	@Deprecated
	public void setStoreEmailAsStreamInSessionKey(String string) {
		storeEmailAsStreamInSessionKey = string;
	}
	public String getStoreEmailAsStreamInSessionKey() {
		return storeEmailAsStreamInSessionKey;
	}

}