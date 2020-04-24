package nl.nn.adapterframework.filesystem;

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
public class Samba2FileSystemTest extends FileSystemTest<String, Samba2FileSystem> {

	private String shareName = "share";
	private String userName = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private Integer port = 139;
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(shareName, userName, password, host, port);
	}


	@Override
	public Samba2FileSystem createFileSystem() {
		Samba2FileSystem result = new Samba2FileSystem();
		result.setShare(shareName);
		result.setUsername(userName);
		result.setPassword(password);
		result.setDomain(host);
		result.setPort(port);
		return result;
	}


}
