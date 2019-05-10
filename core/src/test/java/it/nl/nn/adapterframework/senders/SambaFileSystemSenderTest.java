package it.nl.nn.adapterframework.senders;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba1FileSystem;
import nl.nn.adapterframework.senders.Samba1Sender;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class SambaFileSystemSenderTest extends FileSystemSenderTest<SmbFile, Samba1FileSystem> {
	private String shareName = "Share";
	private String username = "";
	private String password = "";
	private String domain = "";


	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SambaFileSystemTestHelper(shareName,username,password,domain);
	}

	@Override
	public FileSystemSender<SmbFile, Samba1FileSystem> createFileSystemSender() {
		Samba1Sender result = new Samba1Sender();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(domain);
		return result;
	}

}
