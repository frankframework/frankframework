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
import nl.nn.adapterframework.filesystem.SambaFileSystem;

/**
 *  To run this ignore should be removed if all fields are filled.
 *  
 * @author alisihab
 *
 */

public class SambaFileSystemSenderTest extends FileSystemSenderTest<SmbFile, SambaFileSystem> {
	protected String shareName = "Shared"; // the path of smb network must start with "smb://"
	protected String username = "";
	protected String password = "";
	protected String domain = "localhost";
	private SmbFile context;
	protected String share = "smb://" + domain + "/Users/alisihab/Desktop/" + shareName + "/";
	private int waitMilis = 0;

	@Override
	public void setup() throws ConfigurationException, IOException, FileSystemException {
		super.setup();
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication("", username, password);
		context = new SmbFile(share, auth);
		setWaitMilis(waitMilis);
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
		_deleteFile(folderName);
	}

}
