/*
   Copyright 2020 WeAreFrank!

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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.ListenerException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.xml.SaxElementBuilder;
import nl.nn.adapterframework.xml.XmlWriter;

/**
 * Implementation of a {@link nl.nn.adapterframework.filesystem.FileSystemListener
 * FileSystemListener} that enables a
 * {@link nl.nn.adapterframework.receivers.Receiver} to look in a folder
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
public abstract class MailListener<M, A, S extends IMailFileSystem<M,A>> extends FileSystemListener<M,S> {

	public final String EMAIL_MESSAGE_TYPE="email";
	public final String MIME_MESSAGE_TYPE="mime";
	
	private String storeEmailAsStreamInSessionKey;
	private boolean simple = false;
	
	{
		setMessageType(EMAIL_MESSAGE_TYPE);
		setMessageIdPropertyKey(IMailFileSystem.MAIL_MESSAGE_ID);
	}
	

	@Override
	public Message extractMessage(M rawMessage, Map<String,Object> threadContext) throws ListenerException {
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
				MailFileSystemUtils.addEmailInfoSimple(getFileSystem(), rawMessage, emailXml);
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

	@IbisDoc({"1", "when set to <code>true</code>, the xml string passed to the pipeline only contains the subject of the mail (to save memory)", ""})
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