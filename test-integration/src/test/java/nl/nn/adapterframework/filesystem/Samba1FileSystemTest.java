package nl.nn.adapterframework.filesystem;

import jcifs.smb.SmbFile;
/**
 * This test class is created to test both Samba2FileSystem and Samba2FileSystemSender classes.
 * 
 * Instructions to create a share on a windows system:
 * - First create a directory you want to share (location doesn't matter)
 * - Right click to that directory -> properties -> Sharing Tab -> Advanced Sharing Options -> Check Share this Folder option -> 
 * Click Permissions -> Set users to be shared if necessary -> Set permissions(Full Control, read, write) -> Click Apply.
 * To verify share:
 * - open file explorer -> write \\localhost on address bar. You will see the share.
 * 
 * @author alisihab
 *
 */
public class Samba1FileSystemTest extends FileSystemTest<SmbFile, Samba1FileSystem> {

	private String realm = "";
	private String kdc = "localhost";
	private String shareName = "Share";
	private String username = "wearefrank";
	private String password = "pass_123";
	private String domain = "";
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return null;
	}


	@Override
	public Samba1FileSystem createFileSystem() {
		Samba1FileSystem result = new Samba1FileSystem();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(domain);
		return result;
	}

}
