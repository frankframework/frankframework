/*
   Copyright 2013 Nationale-Nederlanden

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
/*
 * $Log: XfbSender.java,v $
 * Revision 1.1  2012-01-10 09:49:15  m00f069
 * Added XfbSender
 *
 */
package nl.nn.adapterframework.extensions.xfb;

import java.io.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderWithParametersBase;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.ProcessUtil;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Sender for transferring files using the XFB protocol. Assumes sender input is local filename.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>className</td><td>nl.nn.adapterframework.extensions.xfb.XfbSender</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setScript(String) script}</td><td>full pathname to the XFB script to be executed to transfer the file</td><td></td></tr>
 * <tr><td>{@link #setFt(String) ft}</td><td>XFB ft parameter</td><td>SEND_FF</td></tr>
 * <tr><td>{@link #setFlow(String) flow}</td><td>XFB flow parameter</td><td></td></tr>
 * <tr><td>{@link #setAppli(String) appli}</td><td>XFB appli parameter</td><td></td></tr>
 * <tr><td>{@link #setNoname(String) noname}</td><td>XFB noname parameter</td><td></td></tr>
 * <tr><td>{@link #setCopy(boolean) copy}</td><td>when set <code>true</code>, the file is copied before calling the XFB script. Reasons to copy the file: - XFB will rename the file (prefix it with FXB_) and delete it. - On Linux the sticky bit (drwxrws--- wasadmin xfbgw) isn't honoured with a move (only with a copy) (on AIX the sticky bit works for both move and copy)</td><td>true</td></tr>
 * <tr><td>{@link #setCopyPrefix(String) copyPrefix}</td><td>prefix for the name of the copied or original filename. When the name of the original file starts with this prefix this prefix is removed otherwise this prefix is added to the filename of the copied file </td><td>IBIS_</td></tr>
 * </table>
 * </p>
 *
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
 * @version $Id$
 */
public class XfbSender extends SenderWithParametersBase {
	private Logger log = LogUtil.getLogger(this);

	private String script;
	private String ft = "SEND_FF";
	private String flow;
	private String appli;
	private String noname;
	private boolean copy = true;
	private String copyPrefix = "IBIS_";

	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getScript())) {
			throw new ConfigurationException("XfbSender ["+getName()+"] attribute script must be specified");
		}
		if (StringUtils.isEmpty(getFlow())) {
			throw new ConfigurationException("XfbSender ["+getName()+"] attribute flow must be specified");
		}
		if (StringUtils.isEmpty(getAppli())) {
			throw new ConfigurationException("XfbSender ["+getName()+"] attribute appli must be specified");
		}
 	} 

	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
		File file = new File(message);
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
		return output;
	}

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

	public void setCopy(boolean copy) {
		this.copy = copy;
	}
	
	public boolean getCopy() {
		return copy;
	}

	public void setCopyPrefix(String copyPrefix) {
		this.copyPrefix = copyPrefix;
	}
	
	public String getCopyPrefix() {
		return copyPrefix;
	}

}
