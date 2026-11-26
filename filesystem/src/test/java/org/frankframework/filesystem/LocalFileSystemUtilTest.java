package org.frankframework.filesystem;

import static org.mockito.Mockito.mock;

import java.nio.file.Path;

import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemUtilTest extends FileSystemUtilsTest <Path, LocalFileSystem>{

	@TempDir
	Path temp;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem(mock());
		result.setRoot(temp.toString());
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(temp);
	}
}
