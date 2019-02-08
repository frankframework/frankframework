package nl.nn.adapterframework.filesystem;

import java.io.File;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.configuration.ConfigurationException;

/**
 *  To run this ignore should be removed if all fields are filled.
 *  
 * @author alisihab
 *
 */

public class SambaFileSystemTest extends LocalFileSystemTestBase<SmbFile, SambaFileSystem> {

	private String localFilePath = ""; // If working with local smb network
	private String share = ""; // the path of smb network must start with "smb://"
	private String username = "";
	private String password = "";

	@Override
	protected File getFileHandle(String filename) {
		return new File(localFilePath, filename);
	}

	@Override
	protected SambaFileSystem getFileSystem() throws ConfigurationException {
		SambaFileSystem sfs = new SambaFileSystem();
		sfs.setShare(share);
		sfs.setUsername(username);
		sfs.setPassword(password);

		return sfs;
	}

}
