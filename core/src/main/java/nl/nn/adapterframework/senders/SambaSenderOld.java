/*
   Copyright 2018 Nationale-Nederlanden

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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang3.StringUtils;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateFormatUtils;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.StreamUtil;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Samba Sender: The standard Windows interoperability suite for Linux and Unix.
 *
 *
 * <p><b>Parameters:</b></p>
 * <p>The <code>upload</code> action requires the file parameter to be set which should contain the fileContent to upload in either Stream, Bytes or String format</p>
 * <p>The <code>rename</code> action requires the destination parameter to be set which should contain the full path relative to the share + filename</p>
 *
 * <p><b>AuthAlias: (WebSphere based application servers)</b></p>
 * <p>If you do not want to specify the username/password used to authenticate with the Samba Share, you can use the authalias property.</p>
 *
 * <p><b>NOTES:</b></p>
 * <p>Folders must always end with a slash.</p>
 * <p>It is possible to move files with the <code>rename</code> action. When doing so, make sure that the target folder exists!</p>
 * <p>It is possible to create multiple directories at once, when the <code>force</code> argument is set to <code>true</code>.</p>
 * <p>The <code>download</code> action returns a base64 encoded string containing the file content.</p>
 *
 * <br/>
 * <br/>
 * <br/>
 *
 * @author	Niels Meijer
 * @since	7.1-B4
 */
@Deprecated
public class SambaSenderOld extends SenderWithParametersBase {

	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	private NtlmPasswordAuthentication auth = null;

	private String action = null;
	private List<String> actions = Arrays.asList("delete", "upload", "mkdir", "rmdir", "rename",
			"download", "list");
	private String share = null;
	private SmbFile smbContext = null;
	private boolean force = false;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();

		if (getShare() == null)
			throw new ConfigurationException(getLogPrefix() + "server share endpoint is required");
		if (!getShare().startsWith("smb://"))
			throw new ConfigurationException(getLogPrefix() + "url must begin with [smb://]");

		if (getAction() == null)
			throw new ConfigurationException(getLogPrefix() + "action must be specified");
		if (!actions.contains(getAction()))
			throw new ConfigurationException(getLogPrefix() + "unknown or invalid action ["
					+ getAction() + "] supported actions are " + actions.toString() + "");

		//Check if necessarily parameters are available
		ParameterList parameterList = getParameterList();
		if (getAction().equals("upload")
				&& (parameterList == null || parameterList.findParameter("file") == null))
			throw new ConfigurationException(getLogPrefix()
					+ "the upload action requires the file parameter to be present");
		if (getAction().equals("rename")
				&& (parameterList == null || parameterList.findParameter("destination") == null))
			throw new ConfigurationException(getLogPrefix()
					+ "the rename action requires a destination parameter to be present");

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getAuthDomain(), cf.getUsername(),
					cf.getPassword());
			log.debug("setting authentication to [" + auth.toString() + "]");
		}

		try {
			//Try to initially connect to the host and create the SMB session.
			//The session automatically closes and re-creates when required.
			smbContext = new SmbFile(getShare(), auth);
		} catch (MalformedURLException e) {
			throw new ConfigurationException(e);
		}
	}

	@Override
	public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
		ParameterValueList pvl = null;
		try {
			if (paramList != null) {
				pvl = paramList.getValues(message, session);
			}
		} catch (ParameterException e) {
			throw new SenderException(getLogPrefix() + "Sender [" + getName() + "] caught exception evaluating parameters", e);
		}

		SmbFile file;
		try {
			file = new SmbFile(smbContext, message.asString());
		} catch (IOException e) {
			throw new SenderException(getLogPrefix() + "unable to get SMB file", e);
		}

		try {
			if (getAction().equalsIgnoreCase("download")) {
				SmbFileInputStream is = new SmbFileInputStream(file);
				InputStream base64 = new Base64InputStream(is, true);
				return new SenderResult(StreamUtil.streamToString(base64));
			} else if (getAction().equalsIgnoreCase("list")) {
				return new SenderResult(listFilesInDirectory(file));
			} else if (getAction().equalsIgnoreCase("upload")) {
				Message paramValue = pvl.get("file").asMessage();

				try(SmbFileOutputStream out = new SmbFileOutputStream(file)) {
					out.write(paramValue.asByteArray());
				}

				return new SenderResult(getFileAsXmlBuilder(new SmbFile(smbContext, message.asString())).toXML());
			} else if (getAction().equalsIgnoreCase("delete")) {
				if (!file.exists())
					throw new SenderException("file not found");

				if (file.isFile())
					file.delete();
				else
					throw new SenderException("trying to remove a directory instead of a file");
			} else if (getAction().equalsIgnoreCase("mkdir")) {
				if (isForced())
					file.mkdirs();
				else
					file.mkdir();
			} else if (getAction().equalsIgnoreCase("rmdir")) {
				if (!file.exists())
					throw new SenderException("folder not found");

				if (file.isDirectory())
					file.delete();
				else
					throw new SenderException("trying to remove a file instead of a directory");
			} else if (getAction().equalsIgnoreCase("rename")) {
				String destination = pvl.get("destination").asStringValue();
				if (destination == null)
					throw new SenderException("unknown destination[+destination+]");

				SmbFile dest = new SmbFile(smbContext, destination);
				if (isForced() && dest.exists())
					dest.delete();

				file.renameTo(dest);
			}
		} catch (Exception e) { //Different types of SMB exceptions can be thrown, no exception means success.. Got to catch them all!
			throw new SenderException(getLogPrefix() + "unable to process action for SmbFile ["
					+ file.getCanonicalPath() + "]", e);
		}

		return new SenderResult("<result>ok</result>");
	}

	private String listFilesInDirectory(SmbFile directory) throws IOException {
		SmbFile[] files = directory.listFiles();

		int count = (files == null ? 0 : files.length);
		XmlBuilder dirXml = new XmlBuilder("directory");
		dirXml.addAttribute("name", directory.getCanonicalPath());
		dirXml.addAttribute("count", count);
		for (int i = 0; i < count; i++) {
			SmbFile file = files[i];
			dirXml.addSubElement(getFileAsXmlBuilder(file));
		}
		return dirXml.toXML();
	}

	private XmlBuilder getFileAsXmlBuilder(SmbFile file) throws SmbException {
		XmlBuilder fileXml = new XmlBuilder("file");
		fileXml.addAttribute("name", file.getName());
		long fileSize = file.length();
		fileXml.addAttribute("size", "" + fileSize);
		fileXml.addAttribute("fSize", Misc.toFileSize(fileSize, true));
		fileXml.addAttribute("directory", "" + file.isDirectory());
		fileXml.addAttribute("canonicalName", file.getCanonicalPath());

		// Get the modification date of the file
		Date modificationDate = new Date(file.lastModified());
		//add date
		String date = DateFormatUtils.format(modificationDate, DateFormatUtils.SHORT_DATE_FORMATTER);
		fileXml.addAttribute("modificationDate", date);

		// add the time
		String time = DateFormatUtils.format(modificationDate, DateFormatUtils.TIME_HMS_FORMATTER);
		fileXml.addAttribute("modificationTime", time);

		return fileXml;
	}

	/** the destination, aka smb://xxx/yyy share */
	public void setShare(String share) {
		if (!share.endsWith("/"))
			share += "/";
		this.share = share;
	}

	public String getShare() {
		return share;
	}

	/** possible values: delete, download, list, mkdir, rename, rmdir, upload */
	public void setAction(String action) {
		this.action = action.toLowerCase();
	}

	public String getAction() {
		return action;
	}

	/**
	 * used when creating folders or overwriting existing files (when renaming or moving)
	 * @ff.default false
	 */
	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean isForced() {
		return force;
	}

	/** in case the user account is bound to a domain */
	public void setAuthDomain(String domain) {
		this.domain = domain;
	}

	public String getAuthDomain() {
		return domain;
	}

	/** the smb share username */
	public void setUsername(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	/** the smb share password */
	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	/** alias used to obtain credentials for the smb share */
	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getAuthAlias() {
		return authAlias;
	}
}
