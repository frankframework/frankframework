package it.nl.nn.adapterframework.filesystems;

import nl.nn.adapterframework.filesystem.FileSystemListener;
import nl.nn.adapterframework.filesystem.FileSystemListenerTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.receivers.Samba2Listener;
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
public class Samba2ListenerTest extends FileSystemListenerTest<String, Samba2FileSystem> {

	private String authType = "SPNEGO";
	private String realm = "";
	private String kdc = "";
	private String shareName = "Share";
	private String username = "";
	private String password = "";
	private String domain = "";
	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(shareName, username, password, domain, realm, kdc, authType);
	}


	@Override
	public FileSystemListener<String, Samba2FileSystem> createFileSystemListener() {
		Samba2Listener result = new Samba2Listener();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setDomain(domain);
		result.setAuthType(authType);
		result.setKdc(kdc);
		result.setRealm(realm);
		return result;
	}

}
