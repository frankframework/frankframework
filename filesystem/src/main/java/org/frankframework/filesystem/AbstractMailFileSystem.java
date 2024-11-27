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
package org.frankframework.filesystem;

import lombok.Getter;

/**
 * Baseclass for {@link IMailFileSystem MailFileSystems}.
 *
 * @author Gerrit van Brakel
 */
public abstract class AbstractMailFileSystem<M,A,C extends AutoCloseable> extends AbstractConnectedFileSystem<M,C> implements IMailFileSystem<M,A> {
	private @Getter String authAlias;
	private @Getter String username;
	private @Getter String password;
	private @Getter String baseFolder;
	private @Getter boolean readMimeContents=false;
	private @Getter String replyAddressFields = REPLY_ADDRESS_FIELDS_DEFAULT;

	@Override
	public String getPhysicalDestinationName() {
		return "baseFolder ["+getBaseFolder()+"]";
	}

	/**
	 * Alias used to obtain accessToken or username and password for authentication to Exchange mail server.
	 * If the alias refers to a combination of a username and a password, the deprecated Basic Authentication method is used.
	 * If the alias refers to a password without a username, the password is treated as the accessToken.
	 */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	/** Username for authentication to mail server. */
	public void setUsername(String username) {
		this.username = username;
	}

	/** Password for authentication to mail server. */
	public void setPassword(String password) {
		this.password = password;
	}

	/** Folder (subfolder of root or of inbox) to look for mails. If empty, the inbox folder is used */
	public void setBaseFolder(String baseFolder) {
		this.baseFolder = baseFolder;
	}

	/**
	 * If set <code>true</code>, the contents will be read in MIME format
	 * @ff.default false
	 */
	public void setReadMimeContents(boolean readMimeContents) {
		this.readMimeContents = readMimeContents;
	}

	/**
	 * Comma separated list of fields to try as response address
	 * @ff.default {@value #REPLY_ADDRESS_FIELDS_DEFAULT}
	 */
	public void setReplyAddressFields(String replyAddressFields) {
		this.replyAddressFields = replyAddressFields;
	}
}
