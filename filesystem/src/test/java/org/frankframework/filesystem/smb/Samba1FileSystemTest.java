package org.frankframework.filesystem.smb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import jcifs.smb.SmbFile;

import org.frankframework.filesystem.FileSystemTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.filesystem.LocalFileSystemTestHelper;
import org.frankframework.testutil.junit.LocalFileServer;
import org.frankframework.testutil.junit.LocalFileServer.FileSystemType;
import org.frankframework.testutil.junit.LocalFileSystemMock;

@Tag("slow")
public class Samba1FileSystemTest extends FileSystemTest<SmbFile, Samba1FileSystem> {

	private final String username = "frankframework";
	private final String password = "pass_123";
	private final String host = "localhost";
	private int port = 445;

	private final String shareName = "home";
	private final String domain = "dummyDomain.NL";

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
		result.setDomainName(domain);
		return result;
	}
}
