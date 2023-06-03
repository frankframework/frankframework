package nl.nn.adapterframework.filesystem;

import org.junit.jupiter.api.BeforeEach;
import org.mockftpserver.fake.FakeFtpServer;
import org.mockftpserver.fake.UserAccount;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

import nl.nn.adapterframework.ftp.FTPFileRef;
import nl.nn.adapterframework.senders.FtpFileSystemSender;

/**
 * This test class is created to test both FtpFileSystem and FtpFileSystemSender classes.
 * 
 * @author Ali Sihab
 */
public class FtpFileSystemSenderTest extends FileSystemSenderTest<FtpFileSystemSender, FTPFileRef, FtpFileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 21;
	private String remoteDirectory = "/home/wearefrank/dir";

	private FakeFtpServer ftpServer;

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new FtpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			ftpServer = new FakeFtpServer();
			ftpServer.setServerControlPort(0); // use any free port

			UnixFakeFileSystem fileSystem = new UnixFakeFileSystem();
			fileSystem.add(new DirectoryEntry(remoteDirectory));
			ftpServer.setFileSystem(fileSystem);

			UserAccount userAccount = new UserAccount(username, password, remoteDirectory);
			ftpServer.addUserAccount(userAccount);

			ftpServer.start();
			port = ftpServer.getServerControlPort();
		}

		super.setUp();
	}

	@Override
	public void tearDown() throws Exception {
		ftpServer.stop();
		ftpServer = null;

		super.tearDown();
	}

	@Override
	public FtpFileSystemSender createFileSystemSender() {
		FtpFileSystemSender fileSystemSender = new FtpFileSystemSender();
		fileSystemSender.setHost(host);
		fileSystemSender.setUsername(username);
		fileSystemSender.setPassword(password);
		fileSystemSender.setRemoteDirectory(remoteDirectory);
		fileSystemSender.setPort(port);

		return fileSystemSender;
	}
}
