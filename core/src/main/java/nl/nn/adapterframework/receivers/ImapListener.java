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
package nl.nn.adapterframework.receivers;

import javax.mail.Message;
import javax.mail.internet.MimeBodyPart;

import nl.nn.adapterframework.doc.IbisDocRef;
import nl.nn.adapterframework.filesystem.ImapFileSystem;
import nl.nn.adapterframework.filesystem.MailListener;

public class ImapListener extends MailListener<Message, MimeBodyPart, ImapFileSystem> {

	public final String IMAP_FILE_SYSTEM ="nl.nn.adapterframework.filesystem.ImapFileSystem";
	
	@Override
	protected ImapFileSystem createFileSystem() {
		return new ImapFileSystem();
	}

	@IbisDocRef({"1", IMAP_FILE_SYSTEM })
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@IbisDocRef({"2", IMAP_FILE_SYSTEM })
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}
	
	
	@IbisDocRef({"3", IMAP_FILE_SYSTEM})
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@IbisDocRef({"4", IMAP_FILE_SYSTEM})
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}
	
	@IbisDocRef({"5", IMAP_FILE_SYSTEM})
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}

	
	@IbisDocRef({"6", IMAP_FILE_SYSTEM})
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}


	@IbisDocRef({"7", IMAP_FILE_SYSTEM})
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}
	

}
