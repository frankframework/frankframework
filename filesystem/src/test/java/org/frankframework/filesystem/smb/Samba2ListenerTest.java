package org.frankframework.filesystem.smb;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.filesystem.WritableFileSystemListenerTest;
import org.frankframework.receivers.Samba2Listener;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileSystemMock;

public class Samba2ListenerTest extends WritableFileSystemListenerTest<SmbFileRef, Samba2FileSystem> {
	private static final boolean runWithDocker = false;
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
	@BeforeEach
	public void setUp() throws Exception {
		if (!runWithDocker) {
			fs.startServer(LocalFileServer.FileSystemType.SMB2);
			port = fs.getPort();
		}
		super.setUp();
	}

	@Override
	public Samba2Listener createFileSystemListener() {
		Samba2Listener result = new Samba2Listener();
		result.setShare(shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setAuthType(Samba2FileSystem.Samba2AuthType.NTLM); // test stub and Docker image only work with NTLM
		if (!runWithDocker) {
			result.setKdc(kdc);
			result.setRealm(realm);
		}
		result.setHostname(host);
		result.setPort(port);
		result.setDomainName(domain);
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() throws IOException {
		if (!runWithDocker) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new Samba2FileSystemTestHelper(host, port, shareName, username, password, domain);
	}

	@Test
	@Disabled("Does not work for SMB2")
	@Override
	public void fileListenerTestAfterMessageProcessedDeleteAndCopy() {
		// Ignore this test
	}
}
