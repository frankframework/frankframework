package org.frankframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.filesystem.WritableFileSystemActorTest;
import org.frankframework.filesystem.smb.Samba2FileSystem.Samba2AuthType;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileServer.FileSystemType;
import org.frankframework.testutil.junit.LocalFileSystemMock;

@Tag("slow")
public class Samba2FileSystemActorTest extends WritableFileSystemActorTest<SmbFileRef, Samba2FileSystem> {

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
			fs.startServer(FileSystemType.SMB2);
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
			result.setAuthType(Samba2AuthType.NTLM);
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
	public void fileSystemActorCopyActionTestRootToFolder() {
		//Ignore this test
	}

	@Override
	@Test
	@Disabled("does not work for SMB2")
	public void fileSystemActorCopyActionTestWithExcludeWildCard() {
		//Ignore this test
	}

	@Test
	@Disabled("does not work for SMB2")
	@Override
	public void fileSystemActorCopyActionTestWithWildCard() {
		//Ignore this test
	}
}
