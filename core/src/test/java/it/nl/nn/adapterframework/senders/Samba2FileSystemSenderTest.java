package it.nl.nn.adapterframework.senders;

import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.senders.Samba2Sender;
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
public class Samba2FileSystemSenderTest extends FileSystemSenderTest<String, Samba2FileSystem> {

	private String authType = "SPNEGO";
	private String realm = "";
	private String kdc = "";
	private String shareName = "Share";
	private String username = "";
	private String password = "";
	private String domain = "";
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(shareName, username, password, domain, realm, kdc);
	}


	@Override
	public FileSystemSender<String, Samba2FileSystem> createFileSystemSender() {
		Samba2Sender result = new Samba2Sender();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(domain);
		result.setAuthType(authType);
		result.setKdc(kdc);
		result.setRealm(realm);
		return result;
	}

//	@Test
//	@Override
//	public void fileSystemTestAppendFile() throws Exception {
//		// ("Smbj library does not support append at the moment: 3/8/2019")
//		super.fileSystemTestAppendFile();
//	}
}
