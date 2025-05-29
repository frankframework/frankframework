package org.frankframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class LocalFileSystemTestHelper implements IFileSystemTestHelperFullControl {

	public Path folder;

	public LocalFileSystemTestHelper(Path folder2) {
		this.folder=folder2;
	}

	@Override
	public void setUp() throws Exception {
		if (Files.exists(folder)) {
			FileUtils.cleanDirectory(folder.toFile());
		}
	}

	@Override
	public void tearDown() throws Exception {
		// not necessary
	}

	protected Path getFileHandle(String filename) {
		return folder.resolve(filename);
	}
	protected Path getFileHandle(String subfolder, String filename) {
		if (subfolder==null) {
			return getFileHandle(filename);
		}
		return folder.resolve(subfolder).resolve(filename);
	}

	@Override
	public boolean _fileExists(String subfolder, String filename) {
		return Files.exists(getFileHandle(subfolder,filename));
	}

	@Override
	public void _deleteFile(String folder, String filename) throws IOException {
		Files.delete(getFileHandle(folder, filename));
	}

	@Override
	public String createFile(String folder, String filename, String contents) throws Exception {
		Path f = getFileHandle(folder, filename);
		try {
			if(folder != null && !Files.exists(f.getParent())) {
				Files.createDirectories(f.getParent());
			}

			Files.createFile(f);
		} catch (IOException e) {
			throw new IOException("Cannot create file ["+ f +"]",e);
		}
		try (OutputStream out = Files.newOutputStream(f)) {
			if(StringUtils.isNotEmpty(contents)) {
				out.write(contents.getBytes());
			}
		}
		return filename;
	}

	@Override
	public void _createFolder(String foldername) throws IOException {
		Path f = getFileHandle(foldername);
		Files.createDirectories(f);
	}

	@Override
	public InputStream _readFile(String folder, String filename) throws IOException {
		return Files.newInputStream(getFileHandle(folder, filename));
	}

	@Override
	public boolean _folderExists(String folderName) throws Exception {
		return _fileExists(null,folderName);
	}

	@Override
	public void _deleteFolder(String folderName) throws Exception {
		if (folderName != null) {
			try {
				_deleteFile(null, folderName);
			} catch (NoSuchFileException e) {
				// nothing to delete if the folder doesn't exist.
			}
		}
	}

	@Override
	public void setFileDate(String folderName, String filename, Date modifiedDate) throws Exception {
		Path f = getFileHandle(folderName, filename);
		Files.setLastModifiedTime(f, FileTime.fromMillis(modifiedDate.getTime()));
	}
}
