/*
   Copyright 2019-2024 WeAreFrank!

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;

import org.junit.jupiter.api.Test;

import org.frankframework.core.ProcessState;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.util.TimeProvider;

public abstract class FileSystemListenerExtraTest<F,S extends IWritableFileSystem<F>> extends WritableFileSystemListenerTest<F, S> {

	@Override
	protected abstract IFileSystemTestHelperFullControl getFileSystemTestHelper();

	private void setFileDate(String folder, String filename, Date date) throws Exception {
		((IFileSystemTestHelperFullControl)helper).setFileDate(folder, filename, date);
	}

	@Test
	public void fileListenerTestGetRawMessageDelayed() throws Exception {
		int stabilityTimeUnit=1000; // ms
		fileSystemListener.setMinStableTime(2*stabilityTimeUnit);
		String filename="rawMessageFile";
		String contents="Test Message Contents";

		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		// file just created, assume that stability time has not yet passed
		RawMessageWrapper<F> rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not yet stable for "+(2*stabilityTimeUnit)+"ms");

		// simulate that the file is older
		setFileDate(null, filename, new Date(TimeProvider.nowAsMillis()-3*stabilityTimeUnit));
		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when stable for "+(3*stabilityTimeUnit)+"ms");
	}

	@Test
	public void changeProcessStateForTwoFilesWithTheSameNameAndSameModTime() throws Exception {
		String inProcessFolder = "inProcessFolder";

		String filename = "rawMessageFile";
		String extension = ".txt";
		String fullName = filename + extension;
		String contents = "Test Message Contents";

		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + inProcessFolder);
		_createFolder(inProcessFolder);

		waitForActionToFinish();

		fileSystemListener.configure();
		fileSystemListener.start();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		// Act 1 -- create and move 1st file
		createFile(null, fullName, contents);

		rawMessage = fileSystemListener.getRawMessage(threadContext);
		Date modificationTime = fileSystemListener.getFileSystem().getModificationTime(rawMessage.getRawMessage());

		assertNotNull(rawMessage, "raw message must be not null when a file is available");

		RawMessageWrapper<F> movedFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null);
		assertThat(fileSystemListener.getFileSystem().getCanonicalName(movedFile.getRawMessage()), containsString(inProcessFolder));
		assertThat(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), startsWith(filename + "-"));
		assertThat(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), endsWith(extension));

		// Act 2 -- create and move 2nd file
		createFile(null, fullName, contents);
		// Update file modification time
		setFileDate(null, fullName, modificationTime);

		RawMessageWrapper<F> rawMessage2 = fileSystemListener.getRawMessage(threadContext);
		RawMessageWrapper<F> movedFile2 = fileSystemListener.changeProcessState(rawMessage2, ProcessState.INPROCESS, null);
		assertThat(fileSystemListener.getFileSystem().getCanonicalName(movedFile2.getRawMessage()), containsString(inProcessFolder));
		assertThat(fileSystemListener.getFileSystem().getName(movedFile2.getRawMessage()), startsWith(filename + "-"));
		assertThat(fileSystemListener.getFileSystem().getName(movedFile2.getRawMessage()), endsWith("-1" + extension));

		assertNotEquals(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), fileSystemListener.getFileSystem()
				.getName(movedFile2.getRawMessage()));
	}
}
