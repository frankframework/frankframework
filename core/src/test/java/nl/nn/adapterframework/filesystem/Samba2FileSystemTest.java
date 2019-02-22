package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;

import com.hierynomus.msdtyp.AccessMask;
import com.hierynomus.mserref.NtStatus;
import com.hierynomus.mssmb2.SMB2CreateDisposition;
import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.mssmb2.SMBApiException;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class Samba2FileSystemTest extends FileSystemTest<String, IFileSystemBase<String>> {

	private String shareName = "Shared";
	private String username = "";
	private String password = "";
	private String domain = "";
	private DiskShare client;
	private Session session;
	Connection connection;

	@Before
	@Override
	public void setup() throws IOException, ConfigurationException, FileSystemException {
		super.setup();
		SMBClient smbClient = new SMBClient();

		AuthenticationContext auth = new AuthenticationContext(username, password.toCharArray(),
				domain);

		try {
			connection = smbClient.connect(domain);
			session = connection.authenticate(auth);
			client = (DiskShare) session.connectShare(shareName);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@After
	public void tearDown() throws IOException {
		if (client != null) {
			client.close();
		}
		if (session != null) {
			session.close();
		}
		if (connection != null) {
			connection.close();
		}
	}

	@Override
	protected IFileSystemBase<String> getFileSystem() throws ConfigurationException {
		Samba2FileSystem fileSystem = new Samba2FileSystem();
		fileSystem.setDomain(domain);
		fileSystem.setPassword(password);
		fileSystem.setUsername(username);
		fileSystem.setShare(shareName);

		return fileSystem;
	}

	@Override
	protected boolean _fileExists(String filename) throws Exception {
		try {

			return client.fileExists(filename);
		} catch (SMBApiException e) {
			if (e.getStatus().equals(NtStatus.STATUS_DELETE_PENDING))
				return false;
			throw e;
		}
	}

	@Override
	protected void _deleteFile(String filename) throws Exception {
		client.rm(filename);

	}

	@Override
	protected OutputStream _createFile(String filename) throws Exception {
		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>();
		createOptions.add(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE);
		Set<AccessMask> accessMask = new HashSet<AccessMask>(EnumSet.of(AccessMask.GENERIC_ALL));

		File file = client.openFile(filename, accessMask, null, SMB2ShareAccess.ALL,
				SMB2CreateDisposition.FILE_OVERWRITE_IF, createOptions);
		return file.getOutputStream();
	}

	@Override
	protected InputStream _readFile(String filename) throws Exception {
		Set<AccessMask> accessMask = new HashSet<AccessMask>();
		accessMask.add(AccessMask.GENERIC_READ);
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);
		File file;
		file = client.openFile(filename, accessMask, null, shareAccess,
				SMB2CreateDisposition.FILE_OPEN, null);

		return file.getInputStream();

		//		Set<SMB2CreateOptions> createOptions = new HashSet<SMB2CreateOptions>(
		//				EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE));
		//		return client.openFile(filename, null, null, SMB2ShareAccess.ALL,
		//				SMB2CreateDisposition.FILE_OPEN, createOptions).getInputStream();
	}

	@Override
	public void _createFolder(String filename) throws Exception {
		Set<AccessMask> accessMask = new HashSet<AccessMask>();
		accessMask.add(AccessMask.FILE_ADD_FILE);
		Set<SMB2ShareAccess> shareAccess = new HashSet<SMB2ShareAccess>();
		shareAccess.addAll(SMB2ShareAccess.ALL);
		client.openDirectory(filename, accessMask, null, shareAccess,
				SMB2CreateDisposition.FILE_OPEN_IF, null);
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return client.folderExists(folderName);
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		client.rmdir(folderName, true);

	}

}
