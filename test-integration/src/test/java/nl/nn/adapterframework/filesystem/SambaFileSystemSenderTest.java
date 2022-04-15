package nl.nn.adapterframework.filesystem;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.senders.Samba1Sender;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class SambaFileSystemSenderTest extends FileSystemSenderTest<Samba1Sender, SmbFile, Samba1FileSystem> {
	private String shareName = "Share";
	private String username = "";
	private String password = "";
	private String domain = "";


	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SambaFileSystemTestHelper(shareName,username,password,domain);
	}

	@Override
	public Samba1Sender createFileSystemSender() {
		Samba1Sender result = new Samba1Sender();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(domain);
		return result;
	}

}
