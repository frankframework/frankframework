package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderResult;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

public abstract class WritableFileSystemSenderTest<FSS extends AbstractFileSystemSender<F, FS>, F, FS extends IBasicFileSystem<F>> extends FileSystemSenderTest<FSS, F, FS> {

	@Test
	public void fileSystemSenderUploadActionTestWithString() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwString", contents.getBytes());

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwString"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.start();

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithByteArray() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwByteArray"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.start();

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();


		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		InputStream stream = new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8));
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		fileSystemSender.addParameter(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTarget"));
		fileSystemSender.setAction(FileSystemAction.UPLOAD);
		fileSystemSender.configure();
		fileSystemSender.start();

		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderRenameActionTest() throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemSender.addParameter(new Parameter("destination", dest));
		fileSystemSender.setAction(FileSystemAction.RENAME);
		fileSystemSender.configure();
		fileSystemSender.start();

		deleteFile(null, dest);

		PipeLineSession session = new PipeLineSession();
		Message message = new Message(filename);
		result = fileSystemSender.sendMessageOrThrow(message, session);

		// test
		assertEquals(dest, result.asString(), "result of sender should be new name of file");

		boolean actual = _fileExists(filename);
		// test
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		actual = _fileExists(dest);
		// test
		assertTrue(actual, "Expected file [" + dest + "] " + "to be present");
	}

	public SenderResult fileSystemSenderWriteFile(String folder, boolean fileAlreadyExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "write" + FILE1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}
		waitForActionToFinish();

		if (fileAlreadyExists && !_fileExists(folder, filename)) {
			createFile(folder, filename, "dummy-contents\n");
		}

		fileSystemSender.setAction(FileSystemAction.WRITE);
		fileSystemSender.setCreateFolder(setCreateFolderAttribute);
		fileSystemSender.addParameter(ParameterBuilder.create("filename", folder + "/" + filename));
		fileSystemSender.configure();
		fileSystemSender.start();

		Message input = new Message("dummyText");
		senderResult = fileSystemSender.sendMessage(input, session);
		CloseUtils.closeSilently(input);
		if (!senderResult.isSuccess()) {
			return senderResult;
		}
		String result = senderResult.getResult().asString();

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		assertTrue(_fileExists(folder, filename), "file should exist in destination folder [" + folder + "]");
		assertEquals("dummyText", StreamUtil.streamToString(_readFile(folder, filename)));
		return senderResult;
	}

	@Test
	public void fileSystemSenderWriteNewFileInFolder() throws Exception {
		senderResult = fileSystemSenderWriteFile("folder1", false, false);
		assertFalse(senderResult.isSuccess());
		assertEquals("folderNotFound", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [WRITE] action for File [folder1/writefile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("folder1] does not exist"));
	}

	@Test
	public void fileSystemSenderWritingFileAndCreateFolderAttributeEnabled() throws Exception {
		fileSystemSenderWriteFile("folder2", false, true);
	}

	@Test
	public void fileSystemSenderWritingFileThatAlreadyExists() throws Exception {
		senderResult = fileSystemSenderWriteFile("folder3", true, false);
		assertFalse(senderResult.isSuccess());
		assertEquals("fileAlreadyExists", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [WRITE] action for File [folder3/writefile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("writefile1.txt] already exists"));
	}

	@Test
	public void fileSystemSenderWritingFileThatAlreadyExistsAndCreateFolderAttributeEnabled() throws Exception {
		senderResult = fileSystemSenderWriteFile("folder4", true, true);
		assertFalse(senderResult.isSuccess());
		assertEquals("fileAlreadyExists", senderResult.getForwardName());
		assertThat(senderResult.getErrorMessage(), containsString("unable to process [WRITE] action for File [folder4/writefile1.txt]"));
		assertThat(senderResult.getErrorMessage(), containsString("writefile1.txt] already exists"));
	}
}
