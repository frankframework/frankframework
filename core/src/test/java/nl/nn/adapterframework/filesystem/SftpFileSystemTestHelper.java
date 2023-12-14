package nl.nn.adapterframework.filesystem;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.SftpSession;
import nl.nn.adapterframework.util.LogUtil;

public class SftpFileSystemTestHelper implements IFileSystemTestHelper{

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

	private void cleanFolder() {
		try {
			removeDirectoryContent(null);
		} catch (SftpException e) {
			LogUtil.getLogger(this).error("unable to clean folder", e);
		}
	}

	private void removeDirectoryContent(String folder) throws SftpException {
		Vector<LsEntry> files = ftpClient.ls((folder==null) ? "*" : folder);
		for (LsEntry ftpFile : files) {
			String fileName = ftpFile.getFilename();
			if (fileName.equals(".") || fileName.equals("..")) {
				continue;
			}
			String recursiveName = (folder != null) ? folder + "/" + ftpFile.getFilename() : ftpFile.getFilename();
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
			throw new FileSystemException("", e);
		}
	}

	@Override
	public OutputStream _createFile(String folder, String filename) throws Exception {
		if(folder != null && !_folderExists(folder)) {
			_createFolder(folder);
		}
		String path = folder != null ? folder + "/" + filename : filename;
		try {
			return ftpClient.put(path);
		} catch (SftpException e) {
			throw new FileSystemException(e);
		}
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
	public void _createFolder(String folder) throws FileSystemException {
		try {
			ftpClient.mkdir(folder);
		} catch (SftpException e) {
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
