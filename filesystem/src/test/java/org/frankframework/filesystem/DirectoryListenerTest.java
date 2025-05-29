package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ProcessState;
import org.frankframework.receivers.DirectoryListener;
import org.frankframework.receivers.RawMessageWrapper;

public class DirectoryListenerTest extends WritableFileSystemListenerTest<Path, LocalFileSystem> {

	@TempDir
	public Path folder;

	@Override
	public AbstractFileSystemListener<Path, LocalFileSystem> createFileSystemListener() {
		DirectoryListener result = new DirectoryListener();
		result.setInputFolder(folder.toAbsolutePath().toString());
		fileAndFolderPrefix = folder.toAbsolutePath() + "/";
		return result;
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}

	@Test
	void testWildcard() throws Exception {
		_createFolder("inputFolder");

		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + "inputFolder");
		fileSystemListener.setWildcard("*.csv");
		fileSystemListener.setMessageType(AbstractFileSystemListener.MessageType.CONTENTS);
		fileSystemListener.setOverwrite(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<Path> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, ".ignore", "content");
		createFile(null, "dinges.csv", "een,twee,drie");

		// We need a sec or two here to let the listener pick up the file
		setWaitMillis(2000);
		waitForActionToFinish();

		RawMessageWrapper<Path> rawMessageWithFiles = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessageWithFiles, "raw message must not be null when file is in input folder");

		// Only the file with the .csv extension should be picked up
		RawMessageWrapper<Path> movedFile = fileSystemListener.changeProcessState(rawMessageWithFiles, ProcessState.INPROCESS, null);
		assertThat(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), startsWith("dinges"));

		RawMessageWrapper<Path> rawMessageAfterMove = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessageAfterMove, "raw message must be null when not available");
	}

	@Test
	void testErrorStorageWithComment() throws Exception {
		// Arrange
		_createFolder("inputFolder");
		_createFolder("inProcessFolder");
		_createFolder("processedFolder");
		_createFolder("errorFolder");

		fileSystemListener.setInputFolder(fileAndFolderPrefix + "inputFolder");
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + "inProcessFolder");
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + "processedFolder");
		fileSystemListener.setErrorFolder(fileAndFolderPrefix + "errorFolder");
		fileSystemListener.setWildcard("*.csv");
		fileSystemListener.setOverwrite(true);

		fileSystemListener.configure();
		fileSystemListener.start();

		createFile("inputFolder", "datafile.csv", "een,twee,drie");

		// We need a sec or two here to let the listener pick up the file
		setWaitMillis(2000);
		waitForActionToFinish();

		// Act
		RawMessageWrapper<Path> rawMessageWithFiles = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessageWithFiles, "raw message must not be null when file is in input folder");

		RawMessageWrapper<Path> fileInProcess = fileSystemListener.changeProcessState(rawMessageWithFiles, ProcessState.INPROCESS, null);

		RawMessageWrapper<Path> fileInError = fileSystemListener.changeProcessState(fileInProcess, ProcessState.ERROR, "something bad happened");

		// Assert
		assertNotNull(fileInError, "File should have been moved to error folder");
		assertNotNull(fileInError.getRawMessage(), "File should have been moved to error folder");

		String comment = fileSystemListener.getFileSystem().getCustomFileAttribute(fileInError.getRawMessage(), "comment");

		assertEquals("something bad happened", comment);

		// Act
		IMessageBrowsingIteratorItem item = fileSystemListener.getMessageBrowser(ProcessState.ERROR).getIterator().next();
		String itemCommentString = item.getCommentString();
		assertEquals("something bad happened", itemCommentString);
	}
}
