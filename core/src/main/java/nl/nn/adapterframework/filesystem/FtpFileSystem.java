package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;

import org.apache.commons.net.ftp.FTPFile;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.ftp.FtpConnectException;
import nl.nn.adapterframework.ftp.FtpSession;
import nl.nn.adapterframework.util.XmlBuilder;

public class FtpFileSystem implements IFileSystem<FTPFile> {

	private FtpSession ftpSession;

	private String remoteDirectory;
	private String remoteFilenamePattern = null;

	public FtpFileSystem() {
		ftpSession = new FtpSession();
	}

	@Override
	public void configure() throws ConfigurationException {
		ftpSession.configure();
		open();
	}

	@Override
	public void open() {
		try {
			ftpSession.openClient("");
		} catch (FtpConnectException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void close() {
		ftpSession.closeClient();

	}

	@Override
	public FTPFile toFile(String filename) throws FileSystemException {
		FTPFile ftpFile;
		ftpFile = new FTPFile();
		ftpFile.setName(filename);

		return ftpFile;
	}

	@Override
	public Iterator<FTPFile> listFiles() throws FileSystemException {
		try {
			return new FTPFilePathIterator(ftpSession.ftpClient.listFiles(remoteDirectory));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean exists(FTPFile f) throws FileSystemException {
		try {
			FTPFile[] files = ftpSession.ftpClient.listFiles();
			for (FTPFile o : files) {
				if (o.getName().equals(f.getName())) {
					return true;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public OutputStream createFile(FTPFile f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpSession.ftpClient.storeFileStream(f.getName());
		return outputStream;
	}

	@Override
	public OutputStream appendFile(FTPFile f) throws FileSystemException, IOException {
		OutputStream outputStream = ftpSession.ftpClient.appendFileStream(f.getName());
		return outputStream;
	}

	@Override
	public InputStream readFile(FTPFile f) throws FileSystemException, IOException {
		InputStream inputStream = ftpSession.ftpClient.retrieveFileStream(f.getName());
		return inputStream;
	}

	@Override
	public void deleteFile(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.deleteFile(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean isFolder(FTPFile f) throws FileSystemException {
		return f.isDirectory();
	}

	@Override
	public void createFolder(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.makeDirectory(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void removeFolder(FTPFile f) throws FileSystemException {
		try {
			ftpSession.ftpClient.removeDirectory(f.getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void renameTo(FTPFile f, String destination) throws FileSystemException {
		try {
			ftpSession.ftpClient.rename(f.getName(), destination);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public long getFileSize(FTPFile f, boolean isFolder) throws FileSystemException {
		return f.getSize();
	}

	@Override
	public String getName(FTPFile f) throws FileSystemException {
		return f.getName();
	}

	@Override
	public String getCanonicalName(FTPFile f, boolean isFolder) throws FileSystemException {
		return f.getName();
	}

	@Override
	public Date getModificationTime(FTPFile f, boolean isFolder) throws FileSystemException {
		if (f.getTimestamp() != null) {
			return f.getTimestamp().getTime();
		}
		return null;
	}

	@Override
	public void augmentDirectoryInfo(XmlBuilder dirInfo, FTPFile f) {
		dirInfo.addAttribute("user", f.getUser());
		dirInfo.addAttribute("group", f.getGroup());
		dirInfo.addAttribute("type", f.getType());
		dirInfo.addAttribute("rawListing", f.getRawListing());
		dirInfo.addAttribute("link", f.getLink());
		dirInfo.addAttribute("hardLinkCount", f.getHardLinkCount());
	}

	public FtpSession getFtpSession() {
		return ftpSession;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public String getRemoteDirectory() {
		return remoteDirectory;
	}

	public void setRemoteFilenamePattern(String string) {
		this.remoteFilenamePattern = string;
	}

	public String getRemoteFilenamePattern() {
		return remoteFilenamePattern;
	}

	private class FTPFilePathIterator implements Iterator<FTPFile> {

		private FTPFile files[];
		int i = 0;

		FTPFilePathIterator(FTPFile files[]) {
			Vector<FTPFile> fList = new Vector<FTPFile>();
			for (int i = 0; i < files.length; i++) {
				String filename = files[i].getName();
				if (!filename.equals(".") && !filename.equals("..")) {
					fList.addElement(files[i]);
				}
			}

			this.files = new FTPFile[fList.size()];
			fList.copyInto(this.files);
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public FTPFile next() {
			return files[i++];
		}

		@Override
		public void remove() {
			try {
				deleteFile(files[i++]);
			} catch (FileSystemException e) {
				e.printStackTrace();
			}
		}
	}

}