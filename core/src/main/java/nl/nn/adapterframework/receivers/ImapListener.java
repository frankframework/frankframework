/*
   Copyright 2020, 2022 WeAreFrank!

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

import jakarta.mail.Message;
import jakarta.mail.internet.MimeBodyPart;

import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.filesystem.ImapFileSystem;
import nl.nn.adapterframework.filesystem.MailListener;

@Category("Advanced")
public class ImapListener extends MailListener<Message, MimeBodyPart, ImapFileSystem> {

	public final String IMAP_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ImapFileSystem";

	@Override
	protected ImapFileSystem createFileSystem() {
		return new ImapFileSystem();
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}


	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}


	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}


	/** @ff.ref nl.nn.adapterframework.filesystem.ImapFileSystem */
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}


}
