package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.Directory;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.util.CredentialFactory;
import nl.nn.adapterframework.util.XmlBuilder;

public class Samba2FileSystem implements IFileSystem<String> {

	private String domain = null;
	private String username = null;
	private String password = null;
	private String authAlias = null;
	/** Share name is required*/
	private String share = null;
	private boolean isForce;
	private boolean listHiddenFiles = true;

	private AuthenticationContext auth;

	private static class SmbClient {

		private static SmbClient smbClient;
		private static DiskShare share;
		private static Session session;
		private static Connection connection;
		private static SMBClient smbclient = null;

		private SmbClient(AuthenticationContext auth, String domain, String shareName) {
			smbclient = new SMBClient();
			try {
				connection = smbclient.connect(domain);
				session = connection.authenticate(auth);
				share = (DiskShare) session.connectShare(shareName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public static SmbClient getInstance(AuthenticationContext auth, String domain,
				String share) {
			if (share == null) {
				smbClient = new SmbClient(auth, domain, share);
			}
			return smbClient;
		}

		public static DiskShare getInstance() {
			return share;
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		if (getShare() == null)
			throw new ConfigurationException("server share endpoint is required");

		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new AuthenticationContext(username, password.toCharArray(), domain);
		}
		open();
	}

	public void open() {
		SmbClient.getInstance(auth, domain, share);
		//		if (smbClient == null) {
		//			smbClient = new SMBClient();
		//		}
		//		try {
		//			if (connection == null) {
		//				connection = smbClient.connect(domain);
		//			}
		//			if (session == null) {
		//				session = connection.authenticate(auth);
		//			}
		//			if (smbShare == null) {
		//				smbShare = (DiskShare) session.connectShare(share);
		//			}
		//		} catch (IOException e) {
		//			e.printStackTrace();
		//		}
	}

	@Override
	public String toFile(String filename) throws FileSystemException {
		return filename;
	}

	@Override
	public Iterator<String> listFiles() throws FileSystemException {
		List<FileIdBothDirectoryInformation> list = SmbClient.getInstance().list("");
		String[] arr = new String[list.size()];
		int i = 0;
		for (FileIdBothDirectoryInformation info : list) {
			arr[i++] = info.getFileName();
		}

		return new FilesIterator(arr);
	}

	@Override
	public boolean exists(String f) throws FileSystemException {
		return SmbClient.getInstance().fileExists(f);
	}

	@Override
	public OutputStream createFile(String f) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>(
				EnumSet.of(AccessMask.MAXIMUM_ALLOWED, AccessMask.FILE_ADD_FILE));

		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>(EnumSet.of(
				SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH));

		return SmbClient.getInstance().openFile(f, accessMask, null, SMB2ShareAccess.ALL,
				SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions).getOutputStream();
	}

	@Override
	public OutputStream appendFile(String f) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>(
				EnumSet.of(AccessMask.FILE_APPEND_DATA));
		File file = getFile(f, accessMask, SMB2CreateDisposition.FILE_OPEN_IF);
		return file.getOutputStream();
	}

	@Override
	public InputStream readFile(String filename) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>();
		accessMask.add(AccessMask.GENERIC_READ);
		return getFile(filename, accessMask, SMB2CreateDisposition.FILE_OPEN).getInputStream();
	}

	@Override
	public void deleteFile(String f) throws FileSystemException {
		SmbClient.getInstance().rm(f);
	}

	@Override
	public void renameTo(String f, String destination) throws FileSystemException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>();
		accessMask.add(AccessMask.GENERIC_ALL);
		File file = getFile(f, accessMask, SMB2CreateDisposition.FILE_OPEN);
		file.rename(destination, true);
	}

	@Override
	public void augmentDirectoryInfo(XmlBuilder dirInfo, String f) {
		dirInfo.addAttribute("name", f);
	}

	@Override
	public boolean isFolder(String f) throws FileSystemException {
		return SmbClient.getInstance().getFileInformation(f).getStandardInformation().isDirectory();
	}

	@Override
	public void createFolder(String f) throws FileSystemException {
		if (SmbClient.getInstance().folderExists(f)) {
			throw new FileSystemException(
					"Create directory for [" + f + "] has failed. Directory already exits.");
		} else {
			SmbClient.getInstance().mkdir(f);
		}
	}

	@Override
	public void removeFolder(String f) throws FileSystemException {
		if (!SmbClient.getInstance().folderExists(f)) {
			throw new FileSystemException(
					"Remove directory for [" + f + "] has failed. Directory does not exist.");
		} else {
			SmbClient.getInstance().rmdir(f, true);
		}
	}

	private File getFile(String filename, Set<AccessMask> accessMask,
			SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>();
		createOptions.add(SMB2CreateOptions.FILE_WRITE_THROUGH);
		File file;

		file = SmbClient.getInstance().openFile(filename, accessMask, null, shareAccess,
				createDisposition, createOptions);
		return file;
	}

	private Directory getFolder(String filename, Set<AccessMask> accessMask,
			SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Directory file;
		file = SmbClient.getInstance().openDirectory(filename, accessMask, null, shareAccess,
				createDisposition, null);
		return file;
	}

	@Override
	public long getFileSize(String f, boolean isFolder) throws FileSystemException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>();
		accessMask.add(AccessMask.FILE_READ_ATTRIBUTES);
		if (isFolder) {
			return getFolder(f, accessMask, SMB2CreateDisposition.FILE_OPEN).getFileInformation()
					.getStandardInformation().getAllocationSize();
		} else {
			return getFile(f, accessMask, SMB2CreateDisposition.FILE_OPEN).getFileInformation()
					.getStandardInformation().getAllocationSize();
		}
	}

	@Override
	public String getName(String f) throws FileSystemException {
		return f;
	}

	@Override
	public String getCanonicalName(String f, boolean isFolder) throws FileSystemException {
		return f;
	}

	@Override
	public Date getModificationTime(String f, boolean isFolder) throws FileSystemException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>();
		accessMask.add(AccessMask.FILE_READ_ATTRIBUTES);
		if (isFolder) {
			return getFolder(f, accessMask, SMB2CreateDisposition.FILE_OPEN).getFileInformation()
					.getBasicInformation().getLastWriteTime().toDate();

		} else {
			return getFile(f, accessMask, SMB2CreateDisposition.FILE_OPEN).getFileInformation()
					.getBasicInformation().getLastWriteTime().toDate();

		}
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

	class FilesIterator implements Iterator<String> {

		private String[] files;
		private int i = 0;

		public FilesIterator(String[] arr) {
			files = arr;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public String next() {
			return files[i++];
		}

	}

}
