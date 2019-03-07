package nl.nn.adapterframework.filesystem;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
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

/**
 * @author alisihab
 *
 */
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
		private static SMBClient client = null;

		public static SmbClient getInstance(AuthenticationContext auth, String domain, String shareName)
				throws FileSystemException {
			if (smbClient == null) {
				client = new SMBClient();
				try {
					connection = client.connect(domain);
					session = connection.authenticate(auth);
					share = (DiskShare) session.connectShare(shareName);
				} catch (IOException e) {
					throw new FileSystemException("Cannot connect to samba server", e);
				}
			}
			return smbClient;
		}

		public static DiskShare getDiskShare() {
			return share;
		}

		public static void close() throws IOException {
			try {
				share.close();
				session.close();
				connection.close();
				client.close();
				smbClient = null;
			} catch (IOException e) {
				throw e;
			}
		}
	}

	@Override
	public void configure() throws ConfigurationException {
		if (StringUtils.isEmpty(getShare()))
			throw new ConfigurationException("server share endpoint is required");

		CredentialFactory cf = new CredentialFactory(getAuthAlias(), getUsername(), getPassword());
		if (StringUtils.isNotEmpty(cf.getUsername())) {
			auth = new AuthenticationContext(username, password.toCharArray(), domain);
		}
	}

	@Override
	public void open() throws FileSystemException {
		SmbClient.getInstance(auth, domain, share);
	}

	@Override
	public void close() throws FileSystemException {
		try {
			SmbClient.close();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String toFile(String filename) throws FileSystemException {
		return filename;
	}

	@Override
	public Iterator<String> listFiles() throws FileSystemException {
		return new FilesIterator(SmbClient.getDiskShare().list("").toArray());
	}

	@Override
	public boolean exists(String f) throws FileSystemException {
		boolean exists = SmbClient.getDiskShare().fileExists(f);
		return exists;
	}

	@Override
	public OutputStream createFile(String f) throws FileSystemException, IOException {
		Set<AccessMask> accessMask = new HashSet<AccessMask>(
				EnumSet.of(AccessMask.MAXIMUM_ALLOWED, AccessMask.FILE_ADD_FILE));

		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>(
				EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE, SMB2CreateOptions.FILE_WRITE_THROUGH));
		final File file = SmbClient.getDiskShare().openFile(f, accessMask, null, SMB2ShareAccess.ALL,
				SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions);
		OutputStream out = file.getOutputStream();

		FilterOutputStream fos = new FilterOutputStream(out) {

			@Override
			public void close() throws IOException {
				super.close();
				file.close();
			}

		};
		return fos;
	}

	@Override
	public OutputStream appendFile(String f) throws FileSystemException, IOException {
		final File file = getFile(f, AccessMask.FILE_APPEND_DATA, SMB2CreateDisposition.FILE_OPEN_IF);
		OutputStream out = file.getOutputStream();
		
		FilterOutputStream fos = new FilterOutputStream(out) {
			@Override
			public void close() throws IOException {
				super.close();
				file.close();
			}
		};

		return fos;
	}

	@Override
	public InputStream readFile(String filename) throws FileSystemException, IOException {
		final File file = getFile(filename, AccessMask.GENERIC_READ, SMB2CreateDisposition.FILE_OPEN);
		InputStream is = file.getInputStream();
		FilterInputStream fis = new FilterInputStream(is) {

			@Override
			public void close() throws IOException {
				super.close();
				file.close();
			}

		};

		return fis;
	}

	@Override
	public void deleteFile(String f) {
		SmbClient.getDiskShare().rm(f);

	}

	@Override
	public void renameTo(String f, String destination) throws FileSystemException {
		File file = getFile(f, AccessMask.GENERIC_ALL, SMB2CreateDisposition.FILE_OPEN);
		file.rename(destination, true);
		file.close();
	}

	@Override
	public void augmentFileInfo(XmlBuilder fileInfo, String f) {
		fileInfo.addAttribute("name", f);
	}

	@Override
	public boolean isFolder(String f) throws FileSystemException {
		boolean isFolder = SmbClient.getDiskShare().getFileInformation(f).getStandardInformation().isDirectory();
		return isFolder;
	}

	@Override
	public void createFolder(String f) throws FileSystemException {
		if (SmbClient.getDiskShare().folderExists(f)) {
			throw new FileSystemException("Create directory for [" + f + "] has failed. Directory already exits.");
		} else {
			SmbClient.getDiskShare().mkdir(f);
		}
	}

	@Override
	public void removeFolder(String f) throws FileSystemException {
		if (!SmbClient.getDiskShare().folderExists(f)) {
			throw new FileSystemException("Remove directory for [" + f + "] has failed. Directory does not exist.");
		} else {
			SmbClient.getDiskShare().rmdir(f, true);
		}
	}

	private File getFile(String filename, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>();
		createOptions.add(SMB2CreateOptions.FILE_WRITE_THROUGH);
		
		Set<AccessMask> accessMaskSet = new HashSet<AccessMask>();
		accessMaskSet.add(accessMask);
		File file;

		file = SmbClient.getDiskShare().openFile(filename, accessMaskSet, null, shareAccess, createDisposition,
				createOptions);

		return file;
	}

	private Directory getFolder(String filename, AccessMask accessMask, SMB2CreateDisposition createDisposition) {
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);

		Set<AccessMask> accessMaskSet = new HashSet<AccessMask>();
		accessMaskSet.add(accessMask);
		
		Directory file;
		file = SmbClient.getDiskShare().openDirectory(filename, accessMaskSet, null, shareAccess, createDisposition, null);
		return file;
	}

	@Override
	public long getFileSize(String f, boolean isFolder) throws FileSystemException {
		long size;
		if (isFolder) {
			Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
			size = dir.getFileInformation().getStandardInformation().getAllocationSize();
			dir.close();
			return size;
		} else {
			File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
			size = file.getFileInformation().getStandardInformation().getAllocationSize();
			file.close();
			return size;
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
		if (isFolder) {
			Directory dir = getFolder(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
			Date date = dir.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
			dir.close();
			return date;
		} else {
			File file = getFile(f, AccessMask.FILE_READ_ATTRIBUTES, SMB2CreateDisposition.FILE_OPEN);
			Date date = file.getFileInformation().getBasicInformation().getLastWriteTime().toDate();
			file.close();
			return date;
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

		private Object[] files;
		private int i = 0;

		public FilesIterator(Object[] objects) {
			files = objects;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public String next() {
			return ((FileIdBothDirectoryInformation)files[i++]).getFileName();
		}

		@Override
		public void remove() {
			deleteFile(((FileIdBothDirectoryInformation)files[i++]).getFileName());
		}
	}
}
