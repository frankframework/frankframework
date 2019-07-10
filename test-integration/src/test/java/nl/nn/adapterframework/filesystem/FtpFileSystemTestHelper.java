package nl.nn.adapterframework.filesystem;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPFile;
import org.junit.After;
import org.junit.Before;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;

public class FtpFileSystemTestHelper implements IFileSystemTestHelper{

	private String username = "";
	private String password = "";
	private String host = "";
	private String remoteDirectory = "";
	private int port = 21;
	
	private FtpSession ftpSession;
	
	public FtpFileSystemTestHelper(String username, String password, String host, String remoteDirectory,
			int port) {
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
			System.err.println(e);
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
		// TODO: must implement folder searching, probably via change-directory
		String path = folder != null ? folder + "/" + filename : filename;
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles(path);
			for (FTPFile o : files) {
				if (o.isDirectory()) {
					if ((filename.endsWith("/") ? o.getName() + "/" : o.getName()).equals(filename)) {
						return true;
					}
				} else if (o.getName().equals(filename)) {
					return true;
				}
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		return false;
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
	public boolean _folderExists(String folderName) throws Exception {
		boolean isDirectory = false;
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles(folderName);
			if(files.length > 1) 
				isDirectory = true;
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
		
		return isDirectory;
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		ftpSession.ftpClient.rmd(folderName);
	}
}
