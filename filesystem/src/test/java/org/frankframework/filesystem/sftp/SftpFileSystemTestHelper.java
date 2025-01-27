package org.frankframework.filesystem.sftp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.Vector;
import java.util.random.RandomGenerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.sshd.common.file.FileSystemFactory;
import org.apache.sshd.common.file.virtualfs.VirtualFileSystemFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.hostbased.StaticHostBasedAuthenticator;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.sftp.server.SftpSubsystemFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.util.LogUtil;

@Log4j2
public class SftpFileSystemTestHelper implements IFileSystemTestHelper {

	private final String username;
	private final String password;
	private final String host;
	private final int port;
	private final String remoteDirectory;

	private ChannelSftp ftpClient;

	public SftpFileSystemTestHelper(String username, String password, String host, String remoteDirectory, int port) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.remoteDirectory = remoteDirectory;
	}

	@Override
	@BeforeEach
	public void setUp() throws ConfigurationException, FileSystemException {
		open();
		cleanFolder();
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		SftpSession.close(ftpClient);
	}

	static SshServer createSshServer(String username, String password, int port) throws IOException {
		if (port == 22) { // If previous random port did work fine, keep it
			port = getFreePortNumber();
		}

		SshServer sshd = SshServer.setUpDefaultServer();
		sshd.setHost("localhost");
		sshd.setPort(port); // Setting port = 0 should work to find an available port, but it doesn't on the GitHub builds.
		sshd.setPasswordAuthenticator((uname, psswrd, session) -> username.equals(uname) && password.equals(psswrd));
		sshd.setHostBasedAuthenticator(new StaticHostBasedAuthenticator(true));

		SftpSubsystemFactory sftpFactory = new SftpSubsystemFactory();
		sshd.setSubsystemFactories(Collections.singletonList(sftpFactory));
		sshd.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
		sshd.setFileSystemFactory(getTestDirectoryFS());
		return sshd;
	}

	private static int getFreePortNumber() {
		for (int i = 0; i <= 5; i++) {
			int portNumber = RandomGenerator.getDefault().nextInt(1024, 65535);
			try {
				Socket s = new Socket();
				log.debug("Trying to bind to port: {}", portNumber);
				s.bind(new InetSocketAddress("localhost", portNumber));
				s.close();
			} catch (IOException e) {
				log.debug("Can't bind to port {}: {}", portNumber, e.getMessage());
				continue;
			}
			return portNumber;
		}
		throw new IllegalStateException("Could not find a free port number, after a few tries.");
	}

	/**
	 * Creates the folder '../target/sftpTestFS' in which the tests will be executed.
	 * This 'virtual FS' will pretend that the mentioned folder is the SFTP HOME directory.
	 */
	private static FileSystemFactory getTestDirectoryFS() throws IOException {
		File targetFolder = new File(".", "target");
		File sftpTestFS = new File(targetFolder.getCanonicalPath(), "sftpTestFS");
		sftpTestFS.mkdir();
		assertTrue(sftpTestFS.exists());

		return new VirtualFileSystemFactory(sftpTestFS.toPath());
	}

	private void cleanFolder() {
		try {
			removeDirectoryContent(null);
		} catch (SftpException e) {
			LogUtil.getLogger(this).error("unable to clean folder", e);
		}
	}

	private void removeDirectoryContent(String folder) throws SftpException {
		Vector<LsEntry> files = ftpClient.ls(folder==null ? "*" : folder);
		for (LsEntry ftpFile : files) {
			String fileName = ftpFile.getFilename();
			if (".".equals(fileName) || "..".equals(fileName)) {
				continue;
			}
			String recursiveName = folder != null ? folder + "/" + ftpFile.getFilename() : ftpFile.getFilename();
			if(ftpFile.getAttrs().isDir()) {
				removeDirectoryContent(recursiveName);
			} else {
				ftpClient.rm(recursiveName);
			}
		}
		if(folder != null) {
			ftpClient.rmdir(folder);
		}
	}

	private void open() throws FileSystemException, ConfigurationException {
		SftpSession ftpSession = new SftpSession();
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);
		ftpSession.setStrictHostKeyChecking(false);
		ftpSession.configure();

		ftpClient = ftpSession.openClient(remoteDirectory);
	}

	@Override
	public boolean _fileExists(String folder, String filename) throws FileSystemException {
		try {
			String path = folder != null ? folder + "/" + filename : filename;
			ftpClient.ls(path);
			return true;
		} catch (SftpException e) {
			if(e.id == 2) {
				return false;
			}
			throw new FileSystemException(e);
		}
	}

	@Override
	public void _deleteFile(String folder, String filename) throws FileSystemException {
		try {
			String path = folder != null ? folder + "/" + filename : filename;
			ftpClient.rm(path);
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public String createFile(String folder, String filename, String contents) throws Exception {
		if(folder != null && !_folderExists(folder)) {
			_createFolder(folder);
		}
		String path = folder != null ? folder + "/" + filename : filename;
		try (OutputStream out = ftpClient.put(path)) {
			if(StringUtils.isNotEmpty(contents)) {
				out.write(contents.getBytes());
			}
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
		return filename;
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws FileSystemException {
		String path = folder != null ? folder + "/" + filename : filename;
		try {
			return ftpClient.get(path);
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void _createFolder(String folder) throws Exception {
		try {
			String[] folders = folder.split("/");
			for(int i = 1; i < folders.length; i++) {
				folders[i] = folders[i - 1] + "/" + folders[i];
			}
			for(String f : folders) {
				if(f.length() != 0 && !_folderExists(f)) {
					ftpClient.mkdir(f);
				}
			}
		} catch (SftpException | ArrayIndexOutOfBoundsException e) {
			throw new FileSystemException("Cannot create directory", e);
		}
	}

	@Override
	public boolean _folderExists(String folder) throws Exception {
		String pwd = null;
		try {
			pwd = ftpClient.pwd();
			try {
				ftpClient.cd(pwd + "/" + folder);
				return true;
			} finally {
				ftpClient.cd(pwd);
			}
		} catch (SftpException e) {
			if(e.id == 2) {
				return false;
			}
			throw new FileSystemException(e);
		}
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if(folderName != null && _folderExists(folderName)) {
			ftpClient.rmdir(folderName);
		}
	}
}
