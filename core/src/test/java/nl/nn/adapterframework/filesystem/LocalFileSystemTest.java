package nl.nn.adapterframework.filesystem;

import java.io.File;

public class LocalFileSystemTest extends LocalFileSystemTestBase<File, LocalFileSystem> {

	@Override
	protected LocalFileSystem getFileSystem() {
		LocalFileSystem result = new LocalFileSystem();
		result.setDirectory(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	protected boolean _folderExists(String folderName) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
}