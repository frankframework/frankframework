package nl.nn.adapterframework.filesystem;

import java.io.File;

public class LocalFileSystemTest extends LocalFileSystemTestBase<File,LocalFileSystem> {

	@Override
	protected LocalFileSystem getFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setDirectory(folder.getRoot().getAbsolutePath());
		return result;
	}


}
