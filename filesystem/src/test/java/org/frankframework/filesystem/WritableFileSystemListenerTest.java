/*
   Copyright 2019-2023 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.core.ListenerException;
import org.frankframework.core.ProcessState;
import org.frankframework.receivers.RawMessageWrapper;

public abstract class WritableFileSystemListenerTest<F, S extends IWritableFileSystem<F>> extends BasicFileSystemListenerTest<F, S> {

	@Test
	public void fileListenerTestGetRawMessageWithInProcessTimeSensitive() throws Exception {
		String folderName = "inProcessFolder";

		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, filename, contents);

		rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when a file is available");

		RawMessageWrapper<F> movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);

		assertTrue(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()).startsWith(filename + "-"));
	}

	@Test
	public void changeProcessStateForTwoFilesWithTheSameName() throws Exception {
		String folderName = "inProcessFolder";

		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, filename, contents);

		rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when a file is available");

		RawMessageWrapper<F> movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()).startsWith(filename + "-"));

		createFile(null, filename, contents);
		RawMessageWrapper<F> rawMessage2 = fileSystemListener.getRawMessage(threadContext);
		RawMessageWrapper<F> movedFile2 = fileSystemListener.changeProcessState(rawMessage2, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile2.getRawMessage()).startsWith(filename + "-"));

		assertNotEquals(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), fileSystemListener.getFileSystem()
				.getName(movedFile2.getRawMessage()));
	}

	@Test
	public void fileListenerTestMoveToInProcessMustFailIfFileAlreadyExistsInInProcessFolder() throws Exception {
		String inProcessFolder = "inProcessFolder";
		String filename = "rawMessageFile";
		_createFolder(inProcessFolder);
		waitForActionToFinish();
		createFile(null, filename, "fakeNewFileContents");
		createFile(inProcessFolder, filename, "fakeExistingFileContents");
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + inProcessFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertThrows(ListenerException.class, () -> fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, "test"));
	}

	@Disabled("TODO: mock getModificationTime (This fails in some operating systems since copying file may change the modification date)")
	@Test
	public void changeProcessStateForTwoFilesWithTheSameNameAndTimestamp() throws Exception {
		String folderName = "inProcessFolder";
		String copiedFileFolderName = "copiedFile";

		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, filename, contents);
		F f = fileSystemListener.getFileSystem().toFile(fileAndFolderPrefix + filename);
		F copiedFile = fileSystemListener.getFileSystem().copyFile(f, fileAndFolderPrefix + copiedFileFolderName, true);

		rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when a file is available");

		RawMessageWrapper<F> movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()).startsWith(filename + "-"));

		F movedCopiedFile = fileSystemListener.getFileSystem().moveFile(copiedFile, fileAndFolderPrefix, true);

		Date modificationDateFile = fileSystemListener.getFileSystem().getModificationTime(movedCopiedFile);
		Date modificationDateSecondFile = fileSystemListener.getFileSystem().getModificationTime(f);
		assertEquals(modificationDateFile, modificationDateSecondFile);

		RawMessageWrapper<F> movedFile2 = fileSystemListener.changeProcessState(new RawMessageWrapper<>(movedCopiedFile), ProcessState.INPROCESS, null);

		String nameOfFirstFile = fileSystemListener.getFileSystem().getName(movedFile.getRawMessage());
		String nameOfSecondFile = fileSystemListener.getFileSystem().getName(movedFile2.getRawMessage());

		assertEquals(nameOfFirstFile, nameOfSecondFile.substring(0, nameOfSecondFile.lastIndexOf("-")));
	}

	@Disabled("TODO: mock getModificationTime (This fails in some operating systems since copying file may change the modification date)")
	@Test
	public void changeProcessStateFor6FilesWithTheSameNameAndTimestamp() throws Exception {
		String folderName = "inProcessFolder";
		String copiedFileFolderName = "copiedFile";

		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + folderName);
		_createFolder(folderName);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, filename, contents);
		F f = fileSystemListener.getFileSystem().toFile(fileAndFolderPrefix + filename);
		Date modificationDateFirstFile = fileSystemListener.getFileSystem().getModificationTime(f);
		// copy file
		for (int i = 1; i <= 6; i++) {
			fileSystemListener.getFileSystem().copyFile(f, fileAndFolderPrefix + copiedFileFolderName + i, true);
		}

		rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when a file is available");

		RawMessageWrapper<F> movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertTrue(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()).startsWith(filename + "-"));

		String nameOfFirstFile = fileSystemListener.getFileSystem().getName(movedFile.getRawMessage());


		for (int i = 1; i <= 6; i++) {
			F movedCopiedFile = fileSystemListener.getFileSystem()
					.moveFile(fileSystemListener.getFileSystem().toFile(fileAndFolderPrefix + copiedFileFolderName + i, filename), fileAndFolderPrefix, true);

			Date modificationDate = fileSystemListener.getFileSystem().getModificationTime(movedCopiedFile);
			assertEquals(modificationDateFirstFile.getTime(), modificationDate.getTime());

			RawMessageWrapper<F> movedFile2 = fileSystemListener.changeProcessState(new RawMessageWrapper<>(movedCopiedFile), ProcessState.INPROCESS, null);

			String nameOfSecondFile = fileSystemListener.getFileSystem().getName(movedFile2.getRawMessage());

			if (i == 6) {
				assertEquals(filename, nameOfSecondFile);
			} else {
				assertEquals(nameOfFirstFile + "-" + i, nameOfSecondFile);
			}
		}
	}
}
