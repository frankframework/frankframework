package org.frankframework.filesystem;

import java.nio.file.Path;

import org.frankframework.receivers.DirectoryListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

public class LocalFileSystemListenerTest extends FileSystemListenerTest<Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	public FileSystemListener<Path, LocalFileSystem> createFileSystemListener() {
		DirectoryListener result=new DirectoryListener();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
	}

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}



}
