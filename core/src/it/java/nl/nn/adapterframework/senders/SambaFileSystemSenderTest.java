package nl.nn.adapterframework.senders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.SambaFileSystem;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class SambaFileSystemSenderTest extends FileSystemSenderTest<SmbFile, SambaFileSystem> {
	private String shareName = "Share";
	private String username = "";
	private String password = "";
	private String domain = "";
	private SmbFile context;
	private String share = "smb://" + domain + "/" + shareName + "/"; // the path of smb network must start with "smb://"
	private int waitMillis = 0;

	{
		setWaitMillis(waitMillis);
	};
	
	@Override
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		super.setUp();
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(domain, username, password);
		context = new SmbFile(share, auth);
		
	}

	@Override
	protected SambaFileSystem getFileSystem() throws ConfigurationException {
		SambaFileSystem sfs = new SambaFileSystem();
		sfs.setShare(share);
		sfs.setUsername(username);
		sfs.setPassword(password);
		sfs.setDomain(domain);
		return sfs;
	}

	@Override
	protected boolean _fileExists(String filename) throws Exception {
		return new SmbFile(context, filename).exists();
	}

	@Override
	protected void _deleteFile(String filename) throws Exception {
		SmbFile f = null;
		f = new SmbFile(context, filename);
		f.delete();
	}

	@Override
	protected OutputStream _createFile(String filename) throws Exception {
		return new SmbFileOutputStream(new SmbFile(context, filename));
	}

	@Override
	protected InputStream _readFile(String filename) throws FileNotFoundException, Exception {
		SmbFileInputStream is = null;
		is = new SmbFileInputStream(new SmbFile(context, filename));
		return is;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		try {
			if(_folderExists(filename)) {
				throw new FileSystemException("Create directory for [" + filename + "] has failed. Directory already exists.");
			}
			new SmbFile(context, filename).mkdir();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		return _fileExists(folderName);
	}

	@Override
	protected void _deleteFolder(String folderName) throws Exception {
		if(!folderName.endsWith("/")) {
			folderName += "/";
		}
		_deleteFile(folderName);
	}
}
