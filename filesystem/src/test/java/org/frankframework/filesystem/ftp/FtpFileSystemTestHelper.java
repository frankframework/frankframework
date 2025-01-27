package org.frankframework.filesystem.ftp;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import lombok.extern.log4j.Log4j2;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.filesystem.FileSystemException;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.util.LogUtil;

@Log4j2
public class FtpFileSystemTestHelper implements IFileSystemTestHelper {

	private final String username;
	private final String password;
	private final String host;
	private final int port;
	private final String remoteDirectory;

	private FTPClient ftpClient;

	public FtpFileSystemTestHelper(String username, String password, String host, String remoteDirectory, int port) {
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
		cleanFolder(null);
	}

	@Override
	@AfterEach
	public void tearDown() throws Exception {
		FtpSession.close(ftpClient);
	}

	private void cleanFolder(String folder) {
		try {
			if (folder != null) {
				folder = folder + "/";
			} else folder = "";
			log.debug("cleaning folder [{}]", folder);
			FTPFile[] files = ftpClient.listFiles(folder);
			for (FTPFile o : files) {
				if (o.isDirectory() && !".".equals(o.getName()) && !"..".equals(o.getName())) {
					FTPFile[] filesInFolder = ftpClient.listFiles(folder + o.getName());
					for (FTPFile ftpFile : filesInFolder) {
						if (ftpFile.isDirectory()) {
							cleanFolder(o.getName() + "/" + ftpFile.getName());
						} else {
							log.debug("deleting file [{}]", folder + o.getName() + "/" + ftpFile.getName());
							ftpClient.deleteFile(folder + o.getName() + "/" + ftpFile.getName());
						}
					}
					log.debug("deleting folder [{}]", folder + o.getName());
					boolean removed = ftpClient.removeDirectory(folder + o.getName());
					if (!removed) {
						log.info("unable to clean folder: " + o.getName());
					}
				} else {
					ftpClient.deleteFile(o.getName());
				}
			}
		} catch (IOException e) {
			LogUtil.getLogger(this).error("unable to clean folder: " + folder, e);
		}
	}

	private void open() throws FileSystemException, ConfigurationException {
		FtpSession ftpSession = new FtpSession() {};
		ftpSession.setUsername(username);
		ftpSession.setPassword(password);
		ftpSession.setHost(host);
		ftpSession.setPort(port);
		ftpSession.configure();

		ftpClient = ftpSession.openClient(remoteDirectory);
	}

	@Override
	public boolean _fileExists(String folder, String filename) throws IOException, FileSystemException {
		try {
			String path = folder != null ? folder + "/" + filename : filename;
			FTPFile[] files = ftpClient.listFiles(path, f -> fileNameFilter(f, filename));
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
			ftpClient.deleteFile(path);
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	private FilterOutputStream completePendingCommand(OutputStream os) {
		FilterOutputStream fos = new FilterOutputStream(os) {
			@Override
			public void close() throws IOException {
				super.close();
				ftpClient.completePendingCommand();
			}
		};
		return fos;
	}

	@Override
	public String createFile(String folder, String filename, String contents) throws Exception {
		String path = folder != null ? folder + "/" + filename : filename;
		try (OutputStream out = completePendingCommand(ftpClient.storeFileStream(path))) {
			if(StringUtils.isNotEmpty(contents)) {
				out.write(contents.getBytes());
			}
		}
		return filename;
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws IOException {
		String path = folder != null ? folder + "/" + filename : filename;
		InputStream is = ftpClient.retrieveFileStream(path);
		ftpClient.completePendingCommand();
		return is;
	}

	@Override
	public void _createFolder(String filename) throws FileSystemException {
		try {
			log.debug("creating folder [{}]", filename);
			ftpClient.makeDirectory(filename);
		} catch (IOException e) {
			throw new FileSystemException("Cannot create directory", e);
		}
	}

	@Override
	public boolean _folderExists(String folder) throws Exception {
		try {
			String pwd = ftpClient.printWorkingDirectory();
			try {
				return ftpClient.changeWorkingDirectory(pwd + "/" + folder);
			} finally {
				ftpClient.changeWorkingDirectory(pwd);
			}
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if (folderName != null && _folderExists(folderName)) {
			log.debug("deleting folder [{}]", folderName);
			boolean removed = ftpClient.removeDirectory(folderName);
			if (!removed) {
				log.info("folder [{}] not removed, cleaning it", folderName);
				cleanFolder(folderName);
				ftpClient.removeDirectory(folderName);
			}
		}
	}
}
