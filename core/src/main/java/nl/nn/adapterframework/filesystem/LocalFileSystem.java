package nl.nn.adapterframework.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class LocalFileSystem implements IFileSystem<File> {

	private String directory;
	private boolean isForce;

	private class FilePathIterator implements Iterator<File> {

		private File files[];
		int i = 0;

		FilePathIterator(File files[]) {
			this.files = files;
		}

		@Override
		public boolean hasNext() {
			return files != null && i < files.length;
		}

		@Override
		public File next() {
			return files[i++];
		}

		@Override
		public void remove() {
			deleteFile(files[i++]);
		}

	}

	@Override
	public void configure() throws ConfigurationException {
	}

	@Override
	public File toFile(String filename) {
		return new File(getDirectory(), filename);
	}

	@Override
	public Iterator<File> listFiles() {
		File dir = new File(getDirectory());
		return new FilePathIterator(dir.listFiles());
	}

	@Override
	public boolean exists(File f) {
		return f.exists();
	}

	@Override
	public OutputStream createFile(File f) throws IOException {
		return new FileOutputStream(f, false);
	}

	@Override
	public OutputStream appendFile(File f) throws FileNotFoundException {
		return new FileOutputStream(f, true);
	}

	@Override
	public InputStream readFile(File f) throws FileNotFoundException {
		return new FileInputStream(f);
	}

	@Override
	public void deleteFile(File f) {
		f.delete();
	}

	@Override
	public boolean isFolder(File f) {
		return f.isDirectory();
	}

	@Override
	public void createFolder(File f) throws FileSystemException {
		if (!f.exists()) {
			f.mkdir();
		}else {
			throw new FileSystemException("Create directory for [" + f + "] has failed. Directory already exists.");
		}
	}

	@Override
	public void removeFolder(File f) throws FileSystemException {
		if (f.exists()) {
			f.delete();
		}else {
			throw new FileSystemException("Remove directory for [" + f.getName() + "] has failed. Directory does not exist.");
		}
	}

	public String getDirectory() {
		return directory;
	}

	public void setDirectory(String directory) {
		this.directory = directory;
	}

	@Override
	public void renameTo(File f, String destination) throws FileSystemException {
		File dest;

		dest = new File(destination);
		if (dest.exists()) {
			if (isForce)
				dest.delete();
			else {
				throw new FileSystemException("Cannot rename file. Destination file already exists.");
			}
		}
		f.renameTo(dest);

	}

	@Override
	public long getFileSize(File f, boolean isFolder) throws FileSystemException {
		return f.length();
	}

	@Override
	public String getName(File f) throws FileSystemException {
		return f.getName();
	}

	@Override
	public String getCanonicalName(File f, boolean isFolder) throws FileSystemException {
		try {
			return f.getCanonicalPath();
		} catch (IOException e) {
			throw new FileSystemException(e);
		}
	}

	@Override
	public Date getModificationTime(File f, boolean isFolder) throws FileSystemException {
		return new Date(f.lastModified());
	}

	@Override
	public Map<String, Object> getAdditionalFileProperties(File f) {
		return null;
	}

	@Override
	public void open() {
		// No Action is required

	}

	@Override
	public void close() {
		// No Action is required

	}

	public void setIsForce(boolean force) {
		this.isForce = force;
	}

}
