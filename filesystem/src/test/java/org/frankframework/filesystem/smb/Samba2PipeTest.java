package org.frankframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemActor;
import org.frankframework.filesystem.FileSystemPipeTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.filesystem.smb.Samba2FileSystem.Samba2AuthType;
import org.frankframework.pipes.Samba2Pipe;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileServer.FileSystemType;
import org.frankframework.testutil.junit.LocalFileSystemMock;

public class Samba2PipeTest extends FileSystemPipeTest<Samba2Pipe, SmbFileRef, Samba2FileSystem> {

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
	public Samba2Pipe createFileSystemPipe() {
		Samba2Pipe pipe = new Samba2Pipe();
		pipe.setShare(shareName);
		pipe.setUsername(username);
		pipe.setPassword(password);
		if("localhost".equals(host)) { // test stub only works with NTLM
			pipe.setAuthType(Samba2AuthType.NTLM);
		}
		pipe.setHostname(host);
		pipe.setPort(port);
		pipe.setKdc(kdc);
		pipe.setRealm(realm);
		pipe.setDomainName(domain);
		return pipe;
	}

	@Test
	void testRestartingPipe() throws Exception {
		fileSystemPipe.setAction(FileSystemActor.FileSystemAction.CREATE);
		fileSystemPipe.configure();
		fileSystemPipe.start();

		fileSystemPipe.stop();
		fileSystemPipe.start();
	}
}
