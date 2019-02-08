package nl.nn.adapterframework.senders;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

import org.junit.Ignore;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.SambaFileSystem;

/**
 *  To run this ignore should be removed if all fields are filled.
 *  
 * @author alisihab
 *
 */
@Ignore
public class SambaFileSystemSenderTest extends FileSystemSenderTest<SmbFile, SambaFileSystem> {
	private String localFilePath = ""; // If working with local smb network
	private String share = ""; // the path of smb network must start with "smb://"
	private String username = "";
	private String password = "";

	SmbFile context;

	@Override
	protected SambaFileSystem getFileSystem() throws ConfigurationException {
		NtlmPasswordAuthentication auth = new NtlmPasswordAuthentication(null, username, password);
		try {
			context = new SmbFile(share, auth);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		return new SambaFileSystem(context, false);
	}

	@Override
	protected boolean _fileExists(String filename) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void _deleteFile(String filename) {
		// TODO Auto-generated method stub

	}

	@Override
	protected OutputStream _createFile(String filename) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected InputStream _readFile(String filename) throws FileNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		// TODO Auto-generated method stub

	}

}
