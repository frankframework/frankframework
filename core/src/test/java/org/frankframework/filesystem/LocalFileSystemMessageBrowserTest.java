package org.frankframework.filesystem;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemMessageBrowserTest extends FileSystemMessageBrowserTest<Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
}
