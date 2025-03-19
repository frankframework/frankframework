package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.core.ProcessState;
import org.frankframework.receivers.DirectoryListener;
import org.frankframework.receivers.RawMessageWrapper;

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

	@Test
	void testWildcard() throws Exception {
		_createFolder("inputFolder");

		waitForActionToFinish();

		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + "inputFolder");
		fileSystemListener.setWildcard("*.csv");
		fileSystemListener.setMessageType(AbstractFileSystemListener.MessageType.CONTENTS);
		fileSystemListener.setOverwrite(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<Path> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, ".gitignore", "content");
		createFile(null, "dinges.csv", "een,twee,drie");

		RawMessageWrapper<Path> rawMessageWithFiles = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessageWithFiles, "raw message must be null when not available");

		// Only the file with the .csv extension should be picked up
		RawMessageWrapper<Path> movedFile = fileSystemListener.changeProcessState(rawMessageWithFiles, ProcessState.INPROCESS, null);
		assertThat(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), startsWith("dinges"));

		RawMessageWrapper<Path> rawMessageAfterMove = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessageAfterMove, "raw message must be null when not available");
	}
}
