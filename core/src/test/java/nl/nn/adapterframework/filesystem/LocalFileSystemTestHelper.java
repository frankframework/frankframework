package nl.nn.adapterframework.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalFileSystemTestHelper implements IFileSystemTestHelper {

	public Path folder;


	public LocalFileSystemTestHelper(Path folder2) {
		this.folder=folder2;
	}

	@Override
	public void setUp() throws Exception {
		// not necessary
	}

	@Override
	public void tearDown() throws Exception {
		// not necessary
	}

	protected Path getFileHandle(String filename) {
		return Paths.get(folder.toAbsolutePath().toString(), filename);
	}
	protected Path getFileHandle(String subfolder, String filename) {
		if (subfolder==null) {
			return getFileHandle(filename);
		}
		return Paths.get(folder.toAbsolutePath()+"/"+subfolder, filename);
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
	public OutputStream _createFile(String folder, String filename) throws IOException {
		Path f = getFileHandle(folder, filename);
		try {
			Files.createFile(f);
		} catch (IOException e) {
			throw new IOException("Cannot create file ["+f.toString()+"]",e);
		}
		return Files.newOutputStream(f);
	}

	@Override
	public void _createFolder(String filename) throws IOException {
		Path f = getFileHandle(filename);
		Files.createDirectory(f);
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
		_deleteFile(null, folderName);
	}
}
