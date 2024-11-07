package org.frankframework.filesystem;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

import org.frankframework.receivers.DirectoryListener;

public class LocalFileSystemListenerTest extends FileSystemListenerExtraTest<Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	public AbstractFileSystemListener<Path, LocalFileSystem> createFileSystemListener() {
		DirectoryListener result=new DirectoryListener();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelperFullControl getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
}
