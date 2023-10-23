package nl.nn.adapterframework.filesystem.smb;

import static org.junit.jupiter.api.Assumptions.assumeFalse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.filesystem.FileSystemTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.LocalFileServer;
import nl.nn.adapterframework.filesystem.LocalFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba2FileSystem;
import nl.nn.adapterframework.filesystem.LocalFileServer.FileSystemType;
import nl.nn.adapterframework.filesystem.Samba2FileSystem.Samba2AuthType;
import nl.nn.adapterframework.testutil.junit.LocalFileSystemMock;

public class Samba2FileSystemTest extends FileSystemTest<SmbFileRef, Samba2FileSystem> {

	private String username = "frankframework";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 445;

	private String shareName = "home";
	private String kdc = "localhost";
	private String realm = "DUMMYDOMAIN.NL";
	private String domain = "dummyDomain.nl";

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
	@Override
	public void basicFileSystemTestExists() throws Exception {
		super.basicFileSystemTestExists();
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
