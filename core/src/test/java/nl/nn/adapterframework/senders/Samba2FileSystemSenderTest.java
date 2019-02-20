package nl.nn.adapterframework.senders;

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
import nl.nn.adapterframework.filesystem.Samba2FileSystem;

public class Samba2FileSystemSenderTest extends FileSystemSenderTest<String, Samba2FileSystem> {

	protected String shareName = "Shared";
	protected String username = "";
	protected String password = "";
	protected String domain = "localhost";
	private DiskShare client = null;
	private Session session = null;
	private Connection connection = null;
	private SMBClient smbClient = null;

	@Before
	@Override
	public void setup() throws IOException, ConfigurationException {
		super.setup();

		AuthenticationContext auth = new AuthenticationContext(username, password.toCharArray(),
				domain);
		open(auth);
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

	public void open(AuthenticationContext auth) {
		if (smbClient == null) {
			smbClient = new SMBClient();
		}
		try {
			if (connection == null) {
				connection = smbClient.connect(domain);
			}
			if (session == null) {
				session = connection.authenticate(auth);
			}
			if (client == null) {
				client = (DiskShare) session.connectShare(shareName);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected Samba2FileSystem getFileSystem() throws ConfigurationException {
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
	protected void _deleteFolder(String folderName) {
		client.rmdir(folderName, true);
	}

}
