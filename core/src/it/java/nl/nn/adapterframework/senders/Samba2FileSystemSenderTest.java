package nl.nn.adapterframework.senders;

import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
/**
 * This test class is created to test both Samba2FileSystem and Samba2FileSystemSender classes.
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
	protected Samba2FileSystem getFileSystem() {
		Samba2FileSystem fileSystem = new Samba2FileSystem();
		fileSystem.setDomain(domain);
		fileSystem.setPassword(password);
		fileSystem.setUsername(username);
		fileSystem.setShare(shareName);
		fileSystem.setAuthType(authType);
		fileSystem.setKdc(kdc);
		fileSystem.setRealm(realm);
		
		return fileSystem;
	}



//	@Test
//	@Override
//	public void fileSystemTestAppendFile() throws Exception {
//		// ("Smbj library does not support append at the moment: 3/8/2019")
//		super.fileSystemTestAppendFile();
//	}
}
