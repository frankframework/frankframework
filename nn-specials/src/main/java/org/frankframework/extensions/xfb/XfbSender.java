/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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
package org.frankframework.extensions.xfb;

import java.io.File;
import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.AbstractSenderWithParameters;
import org.frankframework.stream.Message;
import org.frankframework.util.FileUtils;
import org.frankframework.util.ProcessUtil;

/**
 * Sender for transferring files using the XFB protocol. Assumes sender input is local filename.
 * <br/>
 * Some comments from Richard Maddox (FTO) about UNIX File permissions:
 * <br/>
 * <br/>
 * In case of AIX or SUN systems we advise the following user and directory permissions:
 * <br/>
 * <br/>
 * SENDING CFT:
 * <br/>
 * - App_user must have secondary group: xfbgw
 * <br/>
 * - Folder should have ownership: app_user:xfbgw   (owner:group)
 * <br/>
 * - Folder should have access rights: 770  (rwx.rwx.---)  so nobody other then app_user and group xfbgw can do something in this folder
 * <br/>
 * - Folder should have SGID bit set so that all files what is copied to this folder get group ownership xfbgw
 * <br/>
 * - send file must have rights 660 after putting the file in the send directory.
 * <br/>
 * <br/>
 * RECEIVING CFT:
 * <br/>
 * - App_user (the application user of customer) should have secondary group: xfbgw
 * <br/>
 * - Folder should have ownership:  app_user:xfbgw   (owner:group)
 * <br/>
 * - Folder should have access rights: 770  (rwx.rwx.---)  so nobody other then app_user and group xfbgw can do something in this folder
 * <br/>
 * - Folder should have SGID bit set, so that all files what is copied to this folder get group ownership xfbgw
 * <br/>
 * <br/>
 * There are of course more solutions to get the job done, but this is the solution we can guarantee.
 *
 * @author  Jaco de Groot
 * @since   4.11
 */
public class XfbSender extends AbstractSenderWithParameters {
	private String script;
	private String ft = "SEND_FF";
	private String flow;
	private String appli;
	private String noname;
	private boolean copy = true;
	private String copyPrefix = "IBIS_";

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("XfbSender ["+getName()+"] attribute script must be specified");
		}
		if (StringUtils.isEmpty(getFlow())) {
			throw new ConfigurationException("XfbSender ["+getName()+"] attribute flow must be specified");
		}
		if (StringUtils.isEmpty(getAppli())) {
			throw new ConfigurationException("XfbSender [" + getName() + "] attribute appli must be specified");
		}
	}

	@Override
	public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
		try {
			File file = new File(message.asString());
			if (getCopy()) {
				File fromFile = file;
				String name = fromFile.getName();
				if (name.startsWith(getCopyPrefix())) {
					name = name.substring(getCopyPrefix().length());
				} else {
					name = getCopyPrefix() + name;
				}
				File toFile = new File(fromFile.getParentFile(), name);
				file = toFile;
				if (toFile.exists()) {
					throw new SenderException("File " + toFile.getAbsolutePath() + " already exist");
				}
				if (!FileUtils.copyFile(fromFile, toFile, false)) {
					throw new SenderException("Could not copy file");
				}
			}
			String command = getScript() + " ft=" +getFt() + " flow=" +getFlow() + " appli="+getAppli();
			if (StringUtils.isNotEmpty(getNoname())) {
				command = command + " noname=" +getNoname();
			}
			command = command + " filename=" +file.getAbsolutePath();
			String output = ProcessUtil.executeCommand(command);
			return new SenderResult(output);
		} catch (IOException e) {
			throw new SenderException(getLogPrefix(),e);
		}
	}

	/** Full pathname to the XFB script to be executed to transfer the file */
	public void setScript(String script) {
		this.script = script;
	}

	public String getScript() {
		return script;
	}

	public void setFt(String ft) {
		this.ft = ft;
	}

	public String getFt() {
		return ft;
	}

	public void setFlow(String flow) {
		this.flow = flow;
	}

	public String getFlow() {
		return flow;
	}

	public void setAppli(String appli) {
		this.appli = appli;
	}

	public String getAppli() {
		return appli;
	}

	public void setNoname(String noname) {
		this.noname = noname;
	}

	public String getNoname() {
		return noname;
	}

	/**
	 * When set to <code>true</code>, the file is copied before calling the XFB script.
	 * Reasons to copy the file:
	 * - XFB will rename the file (prefix it with FXB_) and delete it.
	 * - On Linux the sticky bit (drwxrws--- wasadmin xfbgw) isn't honoured with a move (only with a copy) (on AIX the sticky bit works for both move and copy).
	 */
	public void setCopy(boolean copy) {
		this.copy = copy;
	}

	public boolean getCopy() {
		return copy;
	}

	/** Prefix for the name of the copied or original filename. When the name of the original file starts with this prefix, it is removed. Otherwise this prefix is added to the filename of the copied file. */
	public void setCopyPrefix(String copyPrefix) {
		this.copyPrefix = copyPrefix;
	}

	public String getCopyPrefix() {
		return copyPrefix;
	}

}
