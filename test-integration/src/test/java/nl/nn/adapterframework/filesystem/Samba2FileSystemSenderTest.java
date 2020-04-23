package nl.nn.adapterframework.filesystem;

import org.junit.Before;

import nl.nn.adapterframework.senders.Samba2Sender;

/**
 *  This test class is created to test both SambaFileSystem and SambaFileSystemSender classes.
 * @author alisihab
 *
 */

public class Samba2FileSystemSenderTest extends FileSystemSenderTest<Samba2Sender, String, Samba2FileSystem> {

	private String shareName = "share";
	private String userName = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private Integer port = 139;
	private String domain = null;

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new Samba2FileSystemTestHelper(shareName, userName, password, host, port, domain);
	}

	@Override
	public Samba2Sender createFileSystemSender() {
		Samba2Sender result = new Samba2Sender();
		result.setShare(shareName);
		result.setUsername(userName);
		result.setPassword(password);
		result.setDomain(host);
		result.setPort(port);
		return result;
	}

}
