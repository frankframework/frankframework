package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import com.hierynomus.mssmb2.SMB2CreateOptions;
import com.hierynomus.mssmb2.SMB2ShareAccess;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;
import com.hierynomus.smbj.share.File;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class Samba2FileSystemTest extends FileSystemTest<File, IFileSystemBase<File>> {

	private String share = "smb://DESKTOP-1MC6G8V/Users/alisihab/Desktop/Shared/"; // the path of smb network must start with "smb://"
	private String username = "alisihab";
	private String password = "";
	private String domain = "localhost";
	private DiskShare client;

	@Override
	protected IFileSystemBase<File> getFileSystem() throws ConfigurationException {
		SMBClient smbClient = new SMBClient();
		AuthenticationContext auth = new AuthenticationContext(username, password.toCharArray(),
				domain);
		Connection connection;

		try {
			connection = smbClient.connect(domain);
			Session session = connection.authenticate(auth);
			client = (DiskShare) session.connectShare(share);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Samba2FileSystem fileSystem = new Samba2FileSystem();
		fileSystem.setDomain(domain);
		fileSystem.setPassword(password);
		fileSystem.setUsername(username);
		fileSystem.setShare(share);

		return fileSystem;
	}

	@Override
	protected boolean _fileExists(String filename) throws Exception {

		return client.fileExists(filename);
	}

	@Override
	protected void _deleteFile(String filename) throws Exception {
		client.rm(filename);

	}

	@Override
	protected OutputStream _createFile(String filename) throws Exception {
		Set createOptions = new HashSet();
		createOptions.add(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE);
		return client.openFile(filename, null, null, SMB2ShareAccess.ALL, null, createOptions)
				.getOutputStream();
	}

	@Override
	protected InputStream _readFile(String filename) throws Exception {

		Set createOptions = new HashSet(EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE));
		return client.openFile(filename, null, null, SMB2ShareAccess.ALL, null, createOptions)
				.getInputStream();
	}

	@Override
	public void _createFolder(String filename) throws Exception {
		// TODO Auto-generated method stub

	}

}
