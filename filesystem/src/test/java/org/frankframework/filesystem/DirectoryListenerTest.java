package org.frankframework.filesystem;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

import org.frankframework.receivers.DirectoryListener;

public class DirectoryListenerTest extends WritableFileSystemListenerTest<Path, LocalFileSystem> {

	@TempDir
	public Path folder;

	@Override
	public AbstractFileSystemListener<Path, LocalFileSystem> createFileSystemListener() {
		DirectoryListener result=new DirectoryListener();
		result.setInputFolder(folder.toAbsolutePath().toString());
		fileAndFolderPrefix=folder.toAbsolutePath()+"/";
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
}
