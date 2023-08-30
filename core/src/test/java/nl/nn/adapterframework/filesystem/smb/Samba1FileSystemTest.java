package nl.nn.adapterframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;

import jcifs.smb.SmbFile;
import nl.nn.adapterframework.filesystem.FileSystemTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.LocalFileServer;
import nl.nn.adapterframework.filesystem.LocalFileSystemMock;
import nl.nn.adapterframework.filesystem.LocalFileSystemTestHelper;
import nl.nn.adapterframework.filesystem.Samba1FileSystem;
import nl.nn.adapterframework.filesystem.LocalFileServer.FileSystemType;

public class Samba1FileSystemTest extends FileSystemTest<SmbFile, Samba1FileSystem> {

	private String username = "frankframework";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 445;

	private String shareName = "home";
	private String domain = "dummyDomain.NL";

	@LocalFileSystemMock
	private static LocalFileServer fs;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(fs.getTestDirectory());
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			fs.startServer(FileSystemType.SMB1);
			port = fs.getPort();
		}
		super.setUp();
	}

	@Override
	public Samba1FileSystem createFileSystem() {
		Samba1FileSystem result = new Samba1FileSystem();
		result.setShare("smb://localhost:"+port+"/"+shareName);
		result.setUsername(username);
		result.setPassword(password);
		result.setAuthenticationDomain(domain);
		return result;
	}
}
