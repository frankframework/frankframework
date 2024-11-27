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
package org.frankframework.senders;

import jakarta.mail.Message;

import org.frankframework.doc.Category;
import org.frankframework.doc.ReferTo;
import org.frankframework.filesystem.AbstractFileSystemSender;
import org.frankframework.filesystem.ImapFileSystem;

@Category(Category.Type.ADVANCED)
public class ImapSender extends AbstractFileSystemSender<Message, ImapFileSystem> {

	public ImapSender() {
		setFileSystem(new ImapFileSystem());
	}

	@ReferTo(ImapFileSystem.class)
	public void setHost(String host) {
		getFileSystem().setHost(host);
	}

	@ReferTo(ImapFileSystem.class)
	public void setPort(int port) {
		getFileSystem().setPort(port);
	}


	@ReferTo(ImapFileSystem.class)
	public void setAuthAlias(String authAlias) {
		getFileSystem().setAuthAlias(authAlias);
	}

	@ReferTo(ImapFileSystem.class)
	public void setUsername(String username) {
		getFileSystem().setUsername(username);
	}

	@ReferTo(ImapFileSystem.class)
	public void setPassword(String password) {
		getFileSystem().setPassword(password);
	}


	@ReferTo(ImapFileSystem.class)
	public void setBaseFolder(String baseFolder) {
		getFileSystem().setBaseFolder(baseFolder);
	}


	@ReferTo(ImapFileSystem.class)
	public void setReplyAddressFields(String replyAddressFields) {
		getFileSystem().setReplyAddressFields(replyAddressFields);
	}


}
