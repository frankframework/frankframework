package nl.nn.adapterframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.filesystem.FileSystemActorTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.LocalFileServer;
import nl.nn.adapterframework.filesystem.LocalFileSystemMock;
import nl.nn.adapterframework.filesystem.LocalFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;

public class Samba2FileSystemActorTest extends FileSystemActorTest<SmbFileRef, Samba2FileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private int port = 445;

	private final String shareName = "home";
	private final String kdc = "localhost";
	private final String realm = "DUMMYDOMAIN.NL";
	private final String domain = "dummyDomain.nl";

	@LocalFileSystemMock
	private static LocalFileServer fs;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		if("localhost".equals(host)) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new Samba2FileSystemTestHelper(host, port, shareName, username, password, domain);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			fs.startServer(LocalFileServer.FileSystemType.SMB2);
			port = fs.getPort();
		}
		super.setUp();
	}

	@Override
	public Samba2FileSystem createFileSystem() {
		Samba2FileSystem result = new Samba2FileSystem();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		if("localhost".equals(host)) { // test stub only works with NTLM
			result.setAuthType(Samba2FileSystem.Samba2AuthType.NTLM);
		}
		result.setHostname(host);
		result.setPort(port);
		result.setKdc(kdc);
		result.setRealm(realm);
		result.setDomainName(domain);
		return result;
	}

	@Test
	@Disabled("does not work for SMB2")
	@Override
	public void fileSystemActorCopyActionTestRootToFolder() throws Exception {
		//Ignore this test
	}

	@Override
	@Test
	@Disabled("does not work for SMB2")
	public void fileSystemActorCopyActionTestWithExcludeWildCard() throws Exception {
		//Ignore this test
	}

	@Test
	@Disabled("does not work for SMB2")
	@Override
	public void fileSystemActorCopyActionTestWithWildCard() throws Exception {
		//Ignore this test
	}
}
