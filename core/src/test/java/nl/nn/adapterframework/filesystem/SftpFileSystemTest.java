package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;

import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.AsyncAuthException;
import org.apache.sshd.server.auth.hostbased.StaticHostBasedAuthenticator;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.PasswordChangeRequiredException;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import nl.nn.adapterframework.ftp.SftpFileRef;

/**
 * This test class is created to test both SFtpFileSystem and SFtpFileSystemSender classes.
 * 
 * @author Niels Meijer
 *
 */
public class SftpFileSystemTest extends FileSystemTest<SftpFileRef, SftpFileSystem> {

	private String username = "wearefrank";
	private String password = "pass_123";
	private String host = "localhost";
	private int port = 6123;
	private String remoteDirectory = "/home/wearefrank/sftp";

	private SshServer sshd;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		if("localhost".equals(host)) {
			remoteDirectory = "/";

			sshd = SshServer.setUpDefaultServer();
			sshd.setPort(port);
			sshd.setHost("localhost");
			sshd.setPasswordAuthenticator(new PasswordAuthenticator() {

				@Override
				public boolean authenticate(String username, String password, ServerSession session) throws PasswordChangeRequiredException, AsyncAuthException {
					return "wearefrank".equals(username) && "pass_123".equals(password);
				}

			});
			sshd.setHostBasedAuthenticator(new StaticHostBasedAuthenticator(true));

			SftpSubsystemFactory sftpFactory = new SftpSubsystemFactory();
			sshd.setSubsystemFactories(Collections.singletonList(sftpFactory));
			sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());

			Path sftpTestFS = getTestDirectory();
			sshd.setFileSystemFactory(new VirtualFileSystemFactory(sftpTestFS));

			sshd.start();
		}

		super.setUp();
	}

	private static Path getTestDirectory() throws IOException {
		File targetFolder = new File(".", "target");
		File sftpTestFS = new File(targetFolder.getCanonicalPath(), "sftpTestFS");
		sftpTestFS.mkdir();
		assertTrue(sftpTestFS.exists());
		return sftpTestFS.toPath();
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		if(sshd != null) {
			if(sshd.isStarted()) sshd.stop();
			sshd.close(true);
			sshd = null;
		}
		super.tearDown();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new SftpFileSystemTestHelper(username, password, host, remoteDirectory, port);
	}

	@Override
	public SftpFileSystem createFileSystem() {
		SftpFileSystem fileSystem = new SftpFileSystem();
		fileSystem.setHost(host);
		fileSystem.setUsername(username);
		fileSystem.setPassword(password);
		fileSystem.setRemoteDirectory(remoteDirectory);
		fileSystem.setPort(port);
		fileSystem.setStrictHostKeyChecking(false);

		return fileSystem;
	}

	@Test
	public void testSFTPFileRefSetRelative() {
		assertEquals("test123", new SftpFileRef("test123").getName());
		assertEquals("folder/test123", new SftpFileRef("folder/test123").getName());
	}

	@Test
	public void testSFTPFileRefSetFolder() {
		SftpFileRef ref1 = new SftpFileRef("test123");
		ref1.setFolder("folder");
		assertEquals("folder/test123", ref1.getName());
	}

	@Test
	public void testSFTPFileRefRelativeWithSetFolder() {
		SftpFileRef ref2 = new SftpFileRef("folder1/test123");
		ref2.setFolder("folder2");
		assertEquals("folder2/folder1/test123", ref2.getName());
	}

	@Test
	public void testSFTPFileRefWindowsSlash() {
		SftpFileRef ref2 = new SftpFileRef("folder1\\test123");
		ref2.setFolder("folder2");
		assertEquals("folder2/folder1/test123", ref2.getName());
	}
}
