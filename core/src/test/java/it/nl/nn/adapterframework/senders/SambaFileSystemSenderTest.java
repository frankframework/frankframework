package it.nl.nn.adapterframework.senders;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
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




	@Override
	protected SambaFileSystem getFileSystem() {
		SambaFileSystem sfs = new SambaFileSystem();
		String share = "smb://" + domain + "/" + shareName + "/"; // the path of smb network must start with "smb://"
		sfs.setShare(share);
		sfs.setUsername(username);
		sfs.setPassword(password);
		sfs.setDomain(domain);
		return sfs;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SambaFileSystemTestHelper(shareName,username,password,domain);
	}

}
