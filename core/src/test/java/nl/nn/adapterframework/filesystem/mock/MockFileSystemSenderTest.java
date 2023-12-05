package nl.nn.adapterframework.filesystem.mock;

import nl.nn.adapterframework.filesystem.FileSystemSender;
import nl.nn.adapterframework.filesystem.FileSystemSenderTest;
import nl.nn.adapterframework.filesystem.IFileSystemTestHelper;

public class MockFileSystemSenderTest extends FileSystemSenderTest <FileSystemSender<MockFile, MockFileSystem<MockFile>>, MockFile,MockFileSystem<MockFile>>{

	
	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new MockFileSystemTestHelper<MockFile>();
	}

	@Override
	public FileSystemSender<MockFile, MockFileSystem<MockFile>> createFileSystemSender() {
		FileSystemSender<MockFile,MockFileSystem<MockFile>> result=new FileSystemSender<MockFile,MockFileSystem<MockFile>>() {};
		result.setFileSystem(((MockFileSystemTestHelper<MockFile>)helper).getFileSystem());
		return result;
	}
}
