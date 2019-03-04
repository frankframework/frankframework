package nl.nn.adapterframework.filesystems;

import java.io.File;

import nl.nn.adapterframework.filesystem.LocalFileSystem;

public class LocalFileSystemTest extends LocalFileSystemTestBase<File, LocalFileSystem> {

	@Override
	protected LocalFileSystem getFileSystem() {
		LocalFileSystem result = new LocalFileSystem();
		result.setDirectory(folder.getRoot().getAbsolutePath());
		return result;
	}

}