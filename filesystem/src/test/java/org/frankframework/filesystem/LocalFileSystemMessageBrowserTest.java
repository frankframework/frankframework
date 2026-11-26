package org.frankframework.filesystem;

import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemMessageBrowserTest extends FileSystemMessageBrowserTest<Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem(mock());
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
}
