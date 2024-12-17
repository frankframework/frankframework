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
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.ProcessState;
import org.frankframework.lifecycle.LifecycleException;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;

public abstract class BasicFileSystemListenerTest<F, S extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {

	protected String fileAndFolderPrefix = "";
	protected boolean testFullErrorMessages = true;

	protected AbstractFileSystemListener<F, S> fileSystemListener;
	protected Map<String, Object> threadContext;

	public abstract AbstractFileSystemListener<F, S> createFileSystemListener();

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
		if (fileSystemListener != null) {
			fileSystemListener.stop();
			fileSystemListener = null;
		}

		super.tearDown();
	}

	@Test
	public void fileListenerTestConfigure() {
		assertDoesNotThrow(() -> fileSystemListener.configure());
	}

	@Test
	public void fileListenerTestConfigureThrows1() {
		assumeFalse(fileSystemListener.getFileSystem() instanceof IWritableFileSystem<?>);
		fileSystemListener.setFileTimeSensitive(true);

		assertThrows(ConfigurationException.class, () -> fileSystemListener.configure());
	}

	@Test
	public void fileListenerTestConfigureThrows2() {
		assumeFalse(fileSystemListener.getFileSystem() instanceof IWritableFileSystem<?>);
		fileSystemListener.setNumberOfBackups(2);

		assertThrows(ConfigurationException.class, () -> fileSystemListener.configure());
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

		LifecycleException e = assertThrows(LifecycleException.class, fileSystemListener::start);
		String message = e.getCause().getMessage();

		if (testFullErrorMessages) {
			assertThat(message, startsWith("The value for inputFolder [" + folder + "], canonical name ["));
		}
		assertThat(message, endsWith("It is not a folder."));
	}

	@Test
	public void fileListenerTestInvalidInProcessFolder() {
		String folder = fileAndFolderPrefix + "xxx";
		fileSystemListener.setInProcessFolder(folder);
		assertDoesNotThrow(() -> fileSystemListener.configure());

		LifecycleException e = assertThrows(LifecycleException.class, fileSystemListener::start);
		String message = e.getCause().getMessage();

		if (testFullErrorMessages) {
			assertThat(message, startsWith("The value for inProcessFolder [" + folder + "], canonical name ["));
		}
		assertThat(message, endsWith("It is not a folder."));
	}

	@Test
	public void fileListenerTestInvalidProcessedFolder() {
		String folder = fileAndFolderPrefix + "xxx";
		fileSystemListener.setProcessedFolder(folder);
		assertDoesNotThrow(() -> fileSystemListener.configure());

		LifecycleException e = assertThrows(LifecycleException.class, fileSystemListener::start);
		String message = e.getCause().getMessage();

		if (testFullErrorMessages) {
			assertThat(message, startsWith("The value for processedFolder [" + folder + "], canonical name ["));
		}
		assertThat(message, endsWith("It is not a folder."));
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

		String id = createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message = fileSystemListener.extractMessage(rawMessage, threadContext);
		assertThat(message.asString(), containsString(id));
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageContents() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType(AbstractFileSystemListener.MessageType.CONTENTS);
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

		String id = createFile(null, filename, contents);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		String wrapperId = rawMessage.getId();
		assertThat(wrapperId, endsWith(id));

		String filenameAttribute = (String) threadContext.get("filename");
		assertThat(filenameAttribute, containsString(id));

	}

	@Test
	public void fileListenerTestGetIdFromRawMessageMessageTypeName() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType(AbstractFileSystemListener.MessageType.NAME);
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

		String id = createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(id);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse(_fileExists(id), "Expected file [" + filename + "] not to be present");
		if (filename.equals(id)) {
			assertFileExistsWithContents(logFolder, filename, contents);
		}
	}

	@Test
	public void fileListenerTestAfterMessageProcessedMoveFile() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		String id = createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(id));

		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(id));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Destination folder must exist");
		if (fileName.equals(id)) {
			assertTrue(_fileExists(processedFolder, fileName), "Destination must exist");
		}
		assertFalse(_fileExists(id), "Origin must have disappeared");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedMoveFileOverwrite() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		String id1 = createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(id1));

		_createFolder(processedFolder);
		String id2 = createFile(processedFolder, fileName, "");
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.setOverwrite(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(id1));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.SUCCESS);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Destination folder must exist");
		assertTrue(_fileExists(processedFolder, id2), "Destination must exist");
		assertFalse(_fileExists(id1), "Origin must have disappeared");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorDelete() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.configure();
		fileSystemListener.start();

		String id = createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		// test
		existsCheck(id);

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse(_fileExists(id), "Expected file [" + filename + "] not to be present");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToErrorFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		String errorFolder = "errorFolder";

		String id = createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(id));

		_createFolder(processedFolder);
		_createFolder(errorFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.setErrorFolder(fileAndFolderPrefix + errorFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(id));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.ERROR, "test");  // this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Error folder must exist");
		if (fileName.equals(id)) {
			assertTrue(_fileExists(errorFolder, fileName), "Destination must exist in error folder");
			assertFalse(_fileExists(processedFolder, fileName), "Destination must not exist in processed folder");
		}
		assertFalse(_fileExists(id), "Origin must have disappeared");
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToProcessedFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";

		String id = createFile(null, fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(id));

		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix + processedFolder);
		fileSystemListener.configure();
		fileSystemListener.start();

		assertTrue(_fileExists(id));
		assertTrue(_folderExists(processedFolder));

		RawMessageWrapper<F> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState(ExitState.ERROR);
		fileSystemListener.changeProcessState(rawMessage, ProcessState.DONE, "test"); // this action was formerly executed by afterMessageProcessed(), but has been moved to Receiver.moveInProcessToError()
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();

		assertTrue(_folderExists(processedFolder), "Error folder must exist");
		if (fileName.equals(id)) {
			assertTrue(_fileExists(processedFolder, fileName), "Destination must exist in processed folder");
		}
		assertFalse(_fileExists(id), "Origin must have disappeared");
	}
}
