package nl.nn.adapterframework.senders;

import java.io.File;
import java.net.MalformedURLException;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.SambaFileSystem;

import org.junit.Ignore;

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
	protected File getFileHandle(String filename) {
		return new File(localFilePath, filename);
	}

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

}
