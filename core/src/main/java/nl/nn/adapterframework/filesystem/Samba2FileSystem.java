package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.XmlBuilder;

public class Samba2FileSystem implements IFileSystem<File> {

	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	private String share = null;
	private boolean isForce;
	private boolean listHiddenFiles = true;

	private SMBClient smbClient;
	private AuthenticationContext auth;
	private Session session;
	private DiskShare smbShare;

	@Override
	public void configure() throws ConfigurationException {
		if (getShare() == null)
			throw new ConfigurationException("server share endpoint is required");
		if (!getShare().startsWith("smb://"))
			throw new ConfigurationException("url must begin with [smb://]");

		//Setup credentials if applied, may be null.
		//NOTE: When using NtmlPasswordAuthentication without username it returns GUEST
		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new AuthenticationContext(username, password.toCharArray(), domain);
		}
		smbClient = new SMBClient();
		try {
			Connection connection = smbClient.connect(domain);
			session = connection.authenticate(auth);
			smbShare = (DiskShare) session.connectShare(share);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	public File toFile(String filename) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Iterator<File> listFiles() throws FileSystemException {
		return null;
	}

	@Override
	public boolean exists(File f) throws FileSystemException {
		return smbShare.fileExists(f.getFileName());
	}

	@Override
	public OutputStream createFile(File f) throws FileSystemException, IOException {
		Set accessMask = new HashSet(
				EnumSet.of(AccessMask.MAXIMUM_ALLOWED, AccessMask.FILE_ADD_FILE));

		Set createOptions = new HashSet(EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE));

		return smbShare.openFile(f.getFileName(), accessMask, null, SMB2ShareAccess.ALL,
				SMB2CreateDisposition.FILE_CREATE, createOptions).getOutputStream();
	}

	@Override
	public OutputStream appendFile(File f) throws FileSystemException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InputStream readFile(File f) throws FileSystemException, IOException {
		Set<SMB2ShareAccess> s = new HashSet();
		s.add(SMB2ShareAccess.ALL.iterator().next()); // this is to get READ only
		return smbShare
				.openFile(f.getFileName(), EnumSet.of(AccessMask.GENERIC_READ), null, s, null, null)
				.getInputStream();

	}

	@Override
	public void deleteFile(File f) throws FileSystemException {
		smbShare.rm(f.getFileName());

	}

	@Override
	public String getInfo(File f) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void renameTo(File f, String destination) throws FileSystemException {
		f.rename(destination, false);
	}

	@Override
	public XmlBuilder getFileAsXmlBuilder(File f) throws FileSystemException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void augmentDirectoryInfo(XmlBuilder dirInfo, File f) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isFolder(File f) throws FileSystemException {
		return smbShare.getFileInformation(f.getFileName()).getStandardInformation().isDirectory();
	}

	@Override
	public void createFolder(File f) throws FileSystemException {
		smbShare.mkdir(f.getFileName());

	}

	@Override
	public void removeFolder(File f) throws FileSystemException {
		smbShare.rmdir(f.getFileName(), true);

	}

	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getAuthAlias() {
		return authAlias;
	}

	public void setAuthAlias(String authAlias) {
		this.authAlias = authAlias;
	}

	public String getShare() {
		return share;
	}

	public void setShare(String share) {
		this.share = share;
	}

	public boolean isForce() {
		return isForce;
	}

	public void setForce(boolean isForce) {
		this.isForce = isForce;
	}

	public boolean isListHiddenFiles() {
		return listHiddenFiles;
	}

	public void setListHiddenFiles(boolean listHiddenFiles) {
		this.listHiddenFiles = listHiddenFiles;
	}

}
