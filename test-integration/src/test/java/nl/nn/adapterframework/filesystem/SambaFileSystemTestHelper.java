package nl.nn.adapterframework.filesystem;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import nl.nn.adapterframework.configuration.ConfigurationException;

public class SambaFileSystemTestHelper implements IFileSystemTestHelper {

	private String shareName = "Share";
	private String username = "";
	private String password = "";
	private String domain = "";
	private SmbFile context;
	private String share = "smb://" + domain + "/" + shareName + "/"; // the path of smb network must start with "smb://"
		
	public SambaFileSystemTestHelper(String shareName, String username, String password, String domain) {
		this.shareName=shareName;
		this.username=username;
		this.password=password;
		this.domain=domain;
	}
	
	@Override
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(domain, username, password);
		context = new SmbFile(share, auth);
		
	}

	@Override
	public void tearDown() throws Exception {
		// not necessary
	}
	
	@Override
	public boolean _fileExists(String folder, String filename) throws Exception {
		String path=folder==null?filename:folder+"/"+filename;
		return new SmbFile(context, path).exists();
	}

	@Override
	public void _deleteFile(String folder, String filename) throws Exception {
		SmbFile f = null;
		f = new SmbFile(context, filename);
		f.delete();
	}

	@Override
	public OutputStream _createFile(String folder, String filename) throws Exception {
		return new SmbFileOutputStream(new SmbFile(context, filename));
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws FileNotFoundException, Exception {
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
	public boolean _folderExists(String folderName) throws Exception {
		return _fileExists(null,folderName);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if(!folderName.endsWith("/")) {
			folderName += "/";
		}
		_deleteFile(null, folderName);
	}
}
