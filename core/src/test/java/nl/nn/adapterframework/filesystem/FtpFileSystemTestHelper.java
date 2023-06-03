package nl.nn.adapterframework.filesystem;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.util.FilenameUtils;
import nl.nn.adapterframework.util.LogUtil;

public class FtpFileSystemTestHelper implements IFileSystemTestHelper{

	private final String username;
	private final String password;
	private final String host;
	private final int port;
	private final String remoteDirectory;

	private FtpSession ftpSession;

	public FtpFileSystemTestHelper(String username, String password, String host, String remoteDirectory, int port) {
		this.username = username;
		this.password = password;
		this.host = host;
		this.port = port;
		this.remoteDirectory = remoteDirectory;
	}

	@Override
	@Before
	public void setUp() throws ConfigurationException, IOException, FileSystemException {
		open();
		cleanFolder();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		ftpSession.closeClient();
		ftpSession = null;
	}

	private void cleanFolder() {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.isDirectory() && !o.getName().equals(".") && !o.getName().equals("..")) {
					FTPFile[] filesInFolder = ftpSession.ftpClient.listFiles(o.getName());
					for (FTPFile ftpFile : filesInFolder) {
						ftpSession.ftpClient.deleteFile(o.getName()+"/"+ftpFile.getName());
					}
					ftpSession.ftpClient.removeDirectory(o.getName());
				} else {
					ftpSession.ftpClient.deleteFile(o.getName());
				}
			}
		} catch (IOException e) {
			LogUtil.getLogger(this).error("unable to clean folder", e);
		}
	}
	private void open() throws FileSystemException, ConfigurationException {
		ftpSession = new FtpSession();
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);
		ftpSession.configure();

		try {
			ftpSession.openClient(remoteDirectory);
		} catch (FtpConnectException e) {
			throw new FileSystemException("Cannot connect to the FTP server with domain [" + host + "]", e);
		}
	}
	@Override
	public boolean _fileExists(String folder, String filename) throws IOException, FileSystemException {
		try {
			String path = folder != null ? folder + "/" + filename : filename;
			FTPFile[] files = ftpSession.ftpClient.listFiles(path, f -> fileNameFilter(f, filename));
			return files.length > 0;
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	// FTPFile might return a name with folder prefix.
	private static boolean fileNameFilter(FTPFile file, String match) {
		return FilenameUtils.getName(file.getName()).equals(match);
	}

	@Override
	public void _deleteFile(String folder, String filename) throws FileSystemException {
		try {
			String path = folder != null ? folder + "/" + filename : filename;
			ftpSession.ftpClient.deleteFile(path);
		} catch (IOException e) {
			throw new FileSystemException("", e);
		}
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		FilterOutputStream fos = new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				ftpSession.ftpClient.completePendingCommand();
			}
		};
		return fos;
	}

	@Override
	public OutputStream _createFile(String folder, String filename) throws IOException, FileSystemException {
		String path = folder != null ? folder + "/" + filename : filename;
		OutputStream out = ftpSession.ftpClient.storeFileStream(path);
		return completePendingCommand(out);
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws IOException, FileSystemException {
		String path = folder != null ? folder + "/" + filename : filename;
		InputStream is = ftpSession.ftpClient.retrieveFileStream(path);
		ftpSession.ftpClient.completePendingCommand();
		return is;
	}

	@Override
	public void _createFolder(String filename) throws FileSystemException {
		try {
			ftpSession.ftpClient.makeDirectory(filename);
		} catch (IOException e) {
			throw new FileSystemException("Cannot create directory", e);
		}
	}

	@Override
	public boolean _folderExists(String folder) throws Exception {
		String pwd = null;
		try {
			pwd = ftpSession.ftpClient.printWorkingDirectory();
			try {
				return ftpSession.ftpClient.changeWorkingDirectory(pwd + "/" + folder);
			} finally {
				ftpSession.ftpClient.changeWorkingDirectory(pwd);
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		ftpSession.ftpClient.rmd(folderName);
	}
}
