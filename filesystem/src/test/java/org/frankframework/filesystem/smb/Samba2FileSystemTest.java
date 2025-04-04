package org.frankframework.filesystem.smb;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.FileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileSystemMock;

/**
 * Test for the Samba2FileSystem. Without changes, it runs with a Java Samba implementation.
 * <p>
 * To run with a separate Samba 2 server on Docker, set runWithDocker to true, and start the docker container as follows:
 * <pre>docker run -p 139:139 -p 137:137/udp -p 138:138/udp -p 445:445 private.docker.nexus.frankframework.org/ff-test/filesystems/samba2</pre>
 * Or read the docker/README.MD and checkout the ci-images repo and proceed inside directory filesystems/samba2
 * Note: 4 unit tests fail, and 45 passed with the Docker images.
 */
@Tag("slow")
public class Samba2FileSystemTest extends FileSystemTest<SmbFileRef, Samba2FileSystem> {
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
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		if (!runWithDocker) {
			return new LocalFileSystemTestHelper(fs.getTestDirectory());
		}
		return new Samba2FileSystemTestHelper(host, port, shareName, username, password, domain);
	}

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
	public Samba2FileSystem createFileSystem() {
		Samba2FileSystem result = new Samba2FileSystem();
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

	@Test
	@Override
	public void basicFileSystemTestCopyFile() throws Exception {
		assumeFalse("localhost".equals(host)); //Returns 'STATUS_NOT_SUPPORTED (0xc00000bb): IOCTL failed' in combination with JFileServer
		super.basicFileSystemTestCopyFile();
	}

	@Test
	@Override
	public void writableFileSystemTestCopyFileToNonExistentDirectoryCreateFolderTrue() throws Exception {
		assumeFalse("localhost".equals(host)); //Returns 'STATUS_NOT_SUPPORTED (0xc00000bb): IOCTL failed' in combination with JFileServer
		super.writableFileSystemTestCopyFileToNonExistentDirectoryCreateFolderTrue();
	}
}
