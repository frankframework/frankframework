/*
   Copyright 2024 WeAreFrank!

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
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.ListenerException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.ProcessState;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.util.DateFormatUtils;

public abstract class BasicFileSystemListenerTest<F, S extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {

	protected String fileAndFolderPrefix = "";
	protected boolean testFullErrorMessages = true;

	protected FileSystemListener<F, S> fileSystemListener;
	protected Map<String, Object> threadContext;

	public abstract FileSystemListener<F, S> createFileSystemListener();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		fileSystemListener = createFileSystemListener();
		autowireByName(fileSystemListener);
		threadContext = new HashMap<>();
	}

	@Override
	@AfterEach
	public void tearDown() {
		try {
			fileSystemListener.stop();
		} catch (ListenerException e) {
			log.warn("Error closing filesystem listener", e);
		}
		super.tearDown();
	}

	@Test
	public void fileListenerTestConfigure() {
		assertDoesNotThrow(() -> fileSystemListener.configure());
	}

	@Test
	public void fileListenerTestStart() {
		assertDoesNotThrow(() -> fileSystemListener.configure());
		assertDoesNotThrow(() -> fileSystemListener.start());
	}

	@Test
	public void fileListenerTestInvalidInputFolder() {
		String folder = fileAndFolderPrefix + "xxx";
		fileSystemListener.setInputFolder(folder);
		assertDoesNotThrow(() -> fileSystemListener.configure());

		ListenerException e = assertThrows(ListenerException.class, fileSystemListener::start);
		if (testFullErrorMessages) {
			assertThat(e.getMessage(), startsWith("The value for inputFolder [" + folder + "], canonical name ["));
			assertThat(e.getMessage(), endsWith("It is not a folder."));
		} else {
			assertThat(e.getMessage(), endsWith("It is not a folder."));
		}
	}

	@Test
	public void fileListenerTestInvalidInProcessFolder() {
		String folder = fileAndFolderPrefix + "xxx";
		fileSystemListener.setInProcessFolder(folder);
		assertDoesNotThrow(() -> fileSystemListener.configure());

		ListenerException e = assertThrows(ListenerException.class, fileSystemListener::start);
		if (testFullErrorMessages) {
			assertThat(e.getMessage(), startsWith("The value for inProcessFolder [" + folder + "], canonical name ["));
			assertThat(e.getMessage(), endsWith("It is not a folder."));
		} else {
			assertThat(e.getMessage(), endsWith("It is not a folder."));
		}
	}

	@Test
	public void fileListenerTestInvalidProcessedFolder() {
		String folder = fileAndFolderPrefix + "xxx";
		fileSystemListener.setProcessedFolder(folder);
		assertDoesNotThrow(() -> fileSystemListener.configure());

		ListenerException e = assertThrows(ListenerException.class, fileSystemListener::start);
		if (testFullErrorMessages) {
			assertThat(e.getMessage(), startsWith("The value for processedFolder [" + folder + "], canonical name ["));
			assertThat(e.getMessage(), endsWith("It is not a folder."));
		} else {
			assertThat(e.getMessage(), endsWith("It is not a folder."));
		}
	}

	@Test
	public void fileListenerTestCreateInputFolder() {
		fileSystemListener.setInputFolder(fileAndFolderPrefix + "xxx1");
		fileSystemListener.setCreateFolders(true);
		assertDoesNotThrow(() -> fileSystemListener.configure());
		assertDoesNotThrow(() -> fileSystemListener.start());
	}

	@Test
	public void fileListenerTestCreateInProcessFolder() {
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix + "xxx2");
		fileSystemListener.setCreateFolders(true);
		assertDoesNotThrow(() -> fileSystemListener.configure());
		assertDoesNotThrow(() -> fileSystemListener.start());
	}

	@Test
	public void fileListenerTestCreateProcessedFolder() {
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + "xxx3");
		fileSystemListener.setCreateFolders(true);
		assertDoesNotThrow(() -> fileSystemListener.configure());
		assertDoesNotThrow(() -> fileSystemListener.start());
	}

	@Test
	public void fileListenerTestCreateLogFolder() {
		fileSystemListener.setLogFolder(fileAndFolderPrefix + "xxx4");
		fileSystemListener.setCreateFolders(true);
		assertDoesNotThrow(() -> fileSystemListener.configure());
		assertDoesNotThrow(() -> fileSystemListener.start());
	}

	/**
	 * vary this on:
	 * inputFolder
	 * inProcessFolder
	 */
	void fileListenerTestGetRawMessage(String inputFolder, String inProcessFolder) throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		if (inputFolder != null) {
			fileSystemListener.setInputFolder(inputFolder);
		}
		if (inProcessFolder != null) {
			fileSystemListener.setInProcessFolder(fileAndFolderPrefix + inProcessFolder);
			_createFolder(inProcessFolder);
			waitForActionToFinish();
		}
		assertDoesNotThrow(() -> fileSystemListener.configure());
		assertDoesNotThrow(() -> fileSystemListener.start());

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNull(rawMessage, "raw message must be null when not available");

		createFile(null, filename, contents);

		rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage, "raw message must be not null when a file is available");

		RawMessageWrapper<F> secondMessage = fileSystemListener.getRawMessage(threadContext);
		if (inProcessFolder != null) {
			boolean movedFirst = fileSystemListener.changeProcessState(rawMessage, ProcessState.INPROCESS, null) != null;
			boolean movedSecond = fileSystemListener.changeProcessState(secondMessage, ProcessState.INPROCESS, null) != null;
			assertFalse(movedFirst && movedSecond, "raw message not have been moved by both threads");
		} else {
			assertNotNull(secondMessage, "raw message must still be available when no inProcessFolder is configured");
		}
	}

	@Test
	public void fileListenerTestGetRawMessage() throws Exception {
		fileListenerTestGetRawMessage(null, null);
	}

	@Test
	public void fileListenerTestGetRawMessageWithInProcess() throws Exception {
		String folderName = "inProcessFolder";
		fileListenerTestGetRawMessage(null, folderName);
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageFilename() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message = fileSystemListener.extractMessage(rawMessage, threadContext);
		assertThat(message.asString(), containsString(filename));
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageContents() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType(FileSystemListener.MessageType.CONTENTS);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message = fileSystemListener.extractMessage(rawMessage, threadContext);
		assertEquals(contents, message.asString());
	}

	/**
	 * Test for proper id
	 * Test for additionalProperties in session variables
	 */
	@Test
	public void fileListenerTestGetIdFromRawMessage() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id = rawMessage.getId();
		assertThat(id, endsWith(filename));

		String filenameAttribute = (String) threadContext.get("filename");
		assertThat(filenameAttribute, containsString(filename));

	}

	@Test
	public void fileListenerTestGetIdFromRawMessageMessageTypeName() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType(FileSystemListener.MessageType.NAME);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id = rawMessage.getId();
		assertThat(id, endsWith(filename));

		String filepathAttribute = (String) threadContext.get("filepath");
		assertThat(filepathAttribute, containsString(filename));
	}

	@Test
	public void fileListenerTestGetIdFromRawMessageWithMetadata() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setStoreMetadataInSessionKey("metadata");
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id = rawMessage.getId();
		assertThat(id, endsWith(filename));

		String metadataAttribute = (String) threadContext.get("metadata");
		System.out.println(metadataAttribute);
		assertThat(metadataAttribute, startsWith("<metadata"));
	}

	/**
	 * Test for proper id
	 * Test for additionalProperties in session variables
	 */
	@Test
	public void fileListenerTestGetIdFromRawMessageFileTimeSensitive() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String id = rawMessage.getId();
		assertThat(id, containsString(filename));

		long currentDate = System.currentTimeMillis();
		String currentDateFormatted = DateFormatUtils.format(currentDate, DateFormatUtils.FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER);
		String timestamp = id.substring(id.length() - currentDateFormatted.length());
		long timestampDate = DateFormatUtils.parseToInstant(timestamp, DateFormatUtils.FULL_ISO_TIMESTAMP_NO_TZ_FORMATTER).toEpochMilli();

		log.debug("Current date formatted: {}, in Millis: {}, timestamp from file: {}, parsed to millis: {}, difference: {}", currentDateFormatted, currentDate, timestamp, timestampDate, timestampDate - currentDate);
		assertTrue(Math.abs(timestampDate - currentDate) < 7300000); // less than two hours in milliseconds.
	}

	@Test
	public void fileListenerTestAfterMessageProcessedDeleteAndCopy() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;
		String logFolder = "logFolder";
		String contents = "contents of file";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.setLogFolder(fileAndFolderPrefix + logFolder);
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(filename);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse(_fileExists(filename), "Expected file [" + filename + "] not to be present");
		assertFileExistsWithContents(logFolder, filename, contents);
	}

	@Test
	public void fileListenerTestAfterMessageProcessedMoveFile() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Destination folder must exist");
		assertTrue(_fileExists(processedFolder, fileName), "Destination must exist");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedMoveFileOverwrite() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		createFile(processedFolder, fileName, "");
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.setOverwrite(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Destination folder must exist");
		assertTrue(_fileExists(processedFolder, fileName), "Destination must exist");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorDelete() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		// test
		existsCheck(filename);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse(_fileExists(filename), "Expected file [" + filename + "] not to be present");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToErrorFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		String errorFolder = "errorFolder";

		createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		_createFolder(errorFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.setErrorFolder(fileAndFolderPrefix + errorFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.ERROR, "test");  // this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Error folder must exist");
		assertTrue(_fileExists(errorFolder, fileName), "Destination must exist in error folder");
		assertFalse(_fileExists(processedFolder, fileName), "Destination must not exist in processed folder");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToProcessedFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test"); // this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Error folder must exist");
		assertTrue(_fileExists(processedFolder, fileName), "Destination must exist in processed folder");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");
	}
}
