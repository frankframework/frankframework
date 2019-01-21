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
package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.codec.binary.Base64InputStream;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.DateUtils;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Samba Sender: The standard Windows interoperability suite for Linux and Unix.
 * 
 * <p><b>Configuration:</b>
 * <table border="1">
 * <tr><th>attributes</th><th>description</th><th>default</th></tr>
 * <tr><td>{@link #setAction(String) action}</td><td>possible values: delete, download, list, mkdir, rename, rmdir, upload</td><td>&nbsp;</td></tr>
 * 
 * <tr><td>{@link #setShare(String) share}</td><td>The destination, aka smb://xxx/yyy share</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setForce(boolean) force}</td><td>Used when creating folders or overwriting existing files (when renaming or moving)</td><td>false</td></tr>
 * 
 * <tr><td>{@link #setAuthDomain(String) domain}</td><td>in case the user account is bound to a domain</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setUsername(String) username}</td><td>the smb share username</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setPassword(String) password}</td><td>the smb share password</td><td>&nbsp;</td></tr>
 * <tr><td>{@link #setAuthAlias(String) authAlias}</td><td>alias used to obtain credentials for the smb share</td><td>&nbsp;</td></tr>
 * 
 * </table>
 * </p>
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
public class SambaFileSystem implements IFileSystem<SmbFile> {
	protected Logger log = LogUtil.getLogger(this);

	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	private NtlmPasswordAuthentication auth = null;

	private String action = null;
	private List<String> actions = Arrays.asList("delete", "upload", "mkdir", "rmdir", "rename", "download", "list");
	private String share = null;
	private SmbFile smbContext = null;
	private boolean force = false;

	public void configure() throws ConfigurationException {

		if(getShare() == null)
			throw new ConfigurationException("server share endpoint is required");
		if(!getShare().startsWith("smb://"))
			throw new ConfigurationException("url must begin with [smb://]");

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if(StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new NtlmPasswordAuthentication(getAuthDomain(), cf.getUsername(), cf.getPassword());
			log.debug("setting authentication to ["+ auth.toString() +"]");
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
	public SmbFile toFile(String filename) throws FileSystemException {
		try {
			return new SmbFile(smbContext, filename);
		} catch (IOException e) {
			throw new FileSystemException("unable to get SMB file ["+filename+"]", e);
		}
	}

	@Override
	public Iterator<SmbFile> listFiles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists(SmbFile f) throws FileSystemException {
		try {
			return f.exists();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public OutputStream createFile(SmbFile f) throws FileSystemException {
		try {
			return new SmbFileOutputStream( f );
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public OutputStream appendFile(SmbFile f) throws FileSystemException {
		try {
			return new SmbFileOutputStream( f );
		} catch (Exception e) {
			throw new FileSystemException(e);
		}
	} 

	@Override
	public InputStream readFile(SmbFile f) throws IOException {
		SmbFileInputStream is = new SmbFileInputStream( f );
		InputStream base64 = new Base64InputStream(is, true);
		return base64;
	}

	@Override
	public void deleteFile(SmbFile f) throws FileSystemException {
		try {
			if(!f.exists()) {
				throw new FileSystemException("file not found");
			}
			if(f.isFile()) {
				f.delete();
			} else {
				throw new FileSystemException("trying to remove ["+f.getName()+"] which is a directory instead of a file");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String getInfo(SmbFile f) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isFolder(SmbFile f) throws FileSystemException {
		try {
			return f.isDirectory();
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void createFolder(SmbFile f) throws FileSystemException {
		try {
			if(isForced()) {
				f.mkdirs();
			}
			else {
				f.mkdir();
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void removeFolder(SmbFile f) throws FileSystemException {
		try {
			if(f.isDirectory()) {
				f.delete();
			} else {
				throw new FileSystemException("trying to remove file ["+f.getName()+"] which is a file instead of a directory");
			}
		} catch (SmbException e) {
			throw new FileSystemException(e);
		}
	}

//	@Override
//	public String sendMessage(String correlationID, String message, ParameterResolutionContext prc) throws SenderException, TimeOutException {
//		ParameterValueList pvl = null;
//		try {
//			if (prc != null && paramList != null) {
//				pvl = prc.getValues(paramList);
//			}
//		} catch (ParameterException e) {
//			throw new SenderException("Caught exception evaluating parameters",e);
//		}
//
//		SmbFile file;
//		try {
//			file = new SmbFile(smbContext, message);
//		} catch (IOException e) {
//			throw new SenderException("unable to get SMB file", e);
//		}
//
//		try {
//			if (getAction().equalsIgnoreCase("download")) {
//				SmbFileInputStream is = new SmbFileInputStream( file );
//				InputStream base64 = new Base64InputStream(is, true);
//				return Misc.streamToString(base64);
//			}
//			else if (getAction().equalsIgnoreCase("list")) {
//				return listFilesInDirectory(file);
//			}
//			else if (getAction().equalsIgnoreCase("upload")) {
//				Object paramValue = pvl.getParameterValue("file").getValue();
//				byte[] fileBytes = null;
//				if(paramValue instanceof InputStream)
//					fileBytes = Misc.streamToBytes((InputStream) paramValue);
//				else if(paramValue instanceof byte[])
//					fileBytes = (byte[]) paramValue;
//				else if(paramValue instanceof String)
//					fileBytes = ((String) paramValue).getBytes(Misc.DEFAULT_INPUT_STREAM_ENCODING);
//				else
//					throw new SenderException("expected InputStream, ByteArray or String but got ["+paramValue.getClass().getName()+"] instead");
//
//				SmbFileOutputStream out = new SmbFileOutputStream( file );
//				out.write(fileBytes);
//				out.close();
//
//				return getFileAsXmlBuilder(new SmbFile(smbContext, message)).toXML();
//			}
//			else if (getAction().equalsIgnoreCase("delete")) {
//				if(!file.exists())
//					throw new SenderException("file not found");
//
//				if(file.isFile())
//					file.delete();
//				else
//					throw new SenderException("trying to remove a directory instead of a file");
//			}
//			else if (getAction().equalsIgnoreCase("mkdir")) {
//				if(isForced())
//					file.mkdirs();
//				else
//					file.mkdir();
//			}
//			else if (getAction().equalsIgnoreCase("rmdir")) {
//				if(!file.exists())
//					throw new SenderException("folder not found");
//
//				if(file.isDirectory())
//					file.delete();
//				else
//					throw new SenderException("trying to remove a file instead of a directory");
//			}
//			else if (getAction().equalsIgnoreCase("rename")) {
//				String destination = pvl.getParameterValue("destination").asStringValue(null);
//				if(destination == null)
//					throw new SenderException("unknown destination[+destination+]");
//
//				SmbFile dest = new SmbFile(smbContext, destination);
//				if(isForced() && dest.exists())
//					dest.delete();
//
//				file.renameTo(dest);
//			}
//		}
//		catch(Exception e) { //Different types of SMB exceptions can be thrown, no exception means success.. Got to catch them all!
//			throw new SenderException("unable to process action for SmbFile ["+file.getCanonicalPath()+"]", e);
//		}
//
//		return "<result>ok</result>";
//	}

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
		fileXml.addAttribute("fSize", "" + Misc.toFileSize(fileSize,true));
		fileXml.addAttribute("directory", "" + file.isDirectory());
		fileXml.addAttribute("canonicalName", file.getCanonicalPath());

		// Get the modification date of the file
		Date modificationDate = new Date(file.lastModified());
		//add date
		String date = DateUtils.format(modificationDate, DateUtils.FORMAT_DATE);
		fileXml.addAttribute("modificationDate", date);
	
		// add the time
		String time = DateUtils.format(modificationDate, DateUtils.FORMAT_TIME_HMS);
		fileXml.addAttribute("modificationTime", time);
	
		return fileXml;
	}

	public void setShare(String share) {
		if(!share.endsWith("/")) share += "/";
		this.share = share;
	}
	public String getShare() {
		return share;
	}

	public void setAction(String action) {
		this.action = action.toLowerCase();
	}
	public String getAction() {
		return action;
	}

	public void setForce(boolean force) {
		this.force = force;
	}
	public boolean isForced() {
		return force;
	}

	public void setAuthDomain(String domain) {
		this.domain = domain;
	}
	public String getAuthDomain() {
		return domain;
	}

	public void setUsername(String username) {
		this.username = username;
	}
	public String getUsername() {
		return username;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	public String getPassword() {
		return password;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}
	public String getAuthAlias() {
		return authAlias;
	}

}
