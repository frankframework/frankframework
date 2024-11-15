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

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import jakarta.annotation.Nonnull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;

import org.frankframework.core.IMessageBrowsingIteratorItem;
import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.ProcessState;
import org.frankframework.receivers.PullingListenerContainer;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.receivers.Receiver;
import org.frankframework.statistics.MetricsInitializer;

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

		assertThat(fileSystemListener.getFileSystem().getName(movedFile.getRawMessage()), startsWith(filename + "-"));
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

	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToErrorFolderThenRetry() throws Exception {
		// Arrange
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		String errorFolder = "errorFolder";
		String inProcessFolder = "inProcessFolder";

		createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		_createFolder(errorFolder);
		_createFolder(inProcessFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.setErrorFolder(fileAndFolderPrefix + errorFolder);
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + inProcessFolder);
		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.setWildcard("*.txt");
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		Receiver<F> receiver = new Receiver<>();
		receiver.setListener(fileSystemListener);
		MetricsInitializer metrics = mock();
		when(metrics.createCounter(any(), any())).thenAnswer(invocation -> mock(Counter.class));
		when(metrics.createDistributionSummary(any(), any())).thenAnswer(invocation -> mock(DistributionSummary.class));
		when(metrics.createThreadBasedDistributionSummary(any(), any(), anyInt())).thenAnswer(invocation -> mock(DistributionSummary.class));
		receiver.setConfigurationMetrics(metrics);
		PlatformTransactionManager transactionManager = mock();
		when(transactionManager.getTransaction(any())).thenAnswer(invocation -> mock(TransactionStatus.class));
		ApplicationContext applicationContext = mock();
		when(applicationContext.getBean("listenerContainer", PullingListenerContainer.class)).thenAnswer((Answer<PullingListenerContainer<F>>) invocation -> new PullingListenerContainer<F>());
		when(applicationContext.getBean("transactionManager")).thenReturn(transactionManager);
		receiver.setApplicationContext(applicationContext);
		receiver.setAdapter(adapter);
		receiver.setTxManager(transactionManager);
		receiver.configure();

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(PipeLine.ExitState.ERROR);

		// Act 1 -- Move to ErrorStorage
		RawMessageWrapper<F> resultFile = fileSystemListener.changeProcessState(rawMessage, ProcessState.ERROR, "test");// this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		// Assert 1
		assertTrue(_folderExists(errorFolder), "Error folder must exist");
		assertTrue(_fileExists(errorFolder, fileName), "Destination must exist in error folder");
		assertFalse(_fileExists(inProcessFolder, fileName), "Destination must not exist in in-process folder");
		assertFalse(_fileExists(processedFolder, fileName), "Destination must not exist in processed folder");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");

		// Act 2 -- Retry
		IMessageBrowsingIteratorItem item = fileSystemListener.getMessageBrowser(ProcessState.ERROR).getIterator().next();
		ListenerException listenerException = assertThrows(ListenerException.class, () -> receiver.retryMessage(item.getId()));

		// Assert 2
		assertThat(listenerException.getMessage(), containsString(" in state [STOPPED]"));
		String fileNameWithTimeStamp = getUpdatedFilename(fileName, resultFile);
		assertTrue(_fileExists(errorFolder, fileNameWithTimeStamp), "Destination must exist in error folder");
		assertFalse(_fileExists(inProcessFolder, fileName), "Destination must not exist in in-process folder");
		assertFalse(_fileExists(inProcessFolder, fileNameWithTimeStamp), "Destination must not exist in in-process folder");
		assertFalse(_fileExists(processedFolder, fileName), "Destination must not exist in processed folder");
		assertFalse(_fileExists(processedFolder, fileNameWithTimeStamp), "Destination must not exist in processed folder");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");
	}

	private static <F> @Nonnull String getUpdatedFilename(String originalFilename, RawMessageWrapper<F> fileMessage) {
		String extension = FilenameUtils.getExtension(originalFilename);
		String baseName = FilenameUtils.getBaseName(originalFilename);
		String timeStamp = fileMessage.getId().replace(':', '_').replace(originalFilename, "");
		return baseName + timeStamp + (StringUtils.isNotEmpty(extension) ? "." + extension : "");
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
