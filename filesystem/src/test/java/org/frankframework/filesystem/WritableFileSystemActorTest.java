package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.frankframework.core.PipeLineSession;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;

@TestMethodOrder(MethodName.class)
public abstract class WritableFileSystemActorTest<F, FS extends IBasicFileSystem<F>> extends FileSystemActorTest<F, FS> {

	private final String lineSeparator = System.getProperty("line.separator");

	@Test
	public void fileSystemActorParameterActionAndAttributeActionConfigured() throws Exception {
		String filename = "actionParamAndAttr" + FILE1;
		String contents = "Text to read";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		params.add(new Parameter("action", "read"));

		params.add(new Parameter("filename", filename));
		params.configure();

		actor.setAction(FileSystemAction.WRITE);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(null, session);

		result = actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorWriteWithNoCharsetUsed() throws Exception {
		String filename = "senderwriteWithCharsetUseDefault" + FILE1;
		String contents = "€ $ & ^ % @ < é ë ó ú à è";

		PipeLineSession session = new PipeLineSession();
		session.put("senderwriteWithCharsetUseDefault", contents);

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("senderwriteWithCharsetUseDefault"));
		params.configure();

		waitForActionToFinish();

		actor.setAction(FileSystemAction.WRITE);
		actor.setFilename(filename);
		actor.configure(fileSystem, null, adapter);
		actor.open();

		Message message = new Message(contents);
		ParameterValueList pvl = params.getValues(message, session);

		actor.doAction(message, pvl, session);

		String actualContents = readFile(null, filename);
		assertEquals(contents, actualContents);
	}

	@Test
	public void fileSystemActorUploadActionTestWithString() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwString", contents);

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("uploadActionTargetwString"));

		actor.setAction(FileSystemAction.UPLOAD);
		params.configure();
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		String actualContents = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actualContents.trim());
	}

	@Test
	public void fileSystemActorWriteActionWriteLineSeparatorSessionKeyContents() throws Exception {
		String filename = "writeLineSeparator" + FILE1;
		String contents = "Some text content to test write action writeLineSeparator enabled";
		String expectedSize = "1";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		Message sessionMessage = new Message(new ThrowingAfterCloseInputStream(new ByteArrayInputStream(contents.getBytes())));
		session.put("writeLineSeparator", sessionMessage);

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("writeLineSeparator"));

		actor.setWriteLineSeparator(true);
		actor.setAction(FileSystemAction.WRITE);
		actor.setFilename(filename);
		params.configure();
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message("fakeInputMessage");
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		TestAssertions.assertXpathValueEquals(expectedSize, stringResult, "round(file/@size div 100)");

		String actualContents = readFile(null, filename);

		String expected = contents + lineSeparator;

		assertEquals(expected, actualContents);
	}

	@Test
	public void fileSystemActorWriteActionSessionKeyFilenameAndContents() throws Exception {
		String filename = "uploadedFilewithString" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("fileContentSessionValue", contents);

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("fileContentSessionValue"));
		params.add(ParameterBuilder.create().withName("filename").withValue(filename));

		actor.setAction(FileSystemAction.WRITE);
		params.configure();
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message("should-not-be-used");
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		String actualContents = readFile(null, filename);
		assertEquals(contents.trim(), actualContents.trim());
	}

	@Test
	public void fileSystemActorWriteActionWriteLineSeparatorMessageContents() throws Exception {
		String filename = "writeLineSeparator" + FILE1;
		String contents = "Some text content to test write action writeLineSeparator enabled";
		String expectedFSize = "1";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();

		actor.setWriteLineSeparator(true);
		actor.setAction(FileSystemAction.WRITE);
		actor.setFilename(filename);
		params.configure();
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(new ThrowingAfterCloseInputStream(new ByteArrayInputStream(contents.getBytes())));
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		TestAssertions.assertXpathValueEquals(expectedFSize, stringResult, "round(file/@size div 100)");

		String actualContents = readFile(null, filename);

		String expected = contents + lineSeparator;

		assertEquals(expected, actualContents);
	}

	@Test
	public void fileSystemActorWriteActionTestWithByteArrayAndContentsViaAlternativeParameter() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTargetwByteArray"));

		actor.setAction(FileSystemAction.WRITE);
		params.configure();
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// TODO: evaluate 'result'
//		assertEquals(result, message, "result of sender should be input message");
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemActorWriteActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		InputStream stream = new ThrowingAfterCloseInputStream(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("file").withSessionKey("uploadActionTarget"));
		params.configure();

		actor.setAction(FileSystemAction.WRITE);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemActorWriteActionWithBackup() throws Exception {
		String filename = "writeAndBackupTest.txt";
		String contents = "text content:";
		int numOfBackups = 3;
		int numOfWrites = 5;

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("fileSystemActorWriteActionWithBackupKey"));
		params.configure();

		actor.setAction(FileSystemAction.WRITE);
		actor.setNumberOfBackups(numOfBackups);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		for (int i = 0; i < numOfWrites; i++) {
			session.put("fileSystemActorWriteActionWithBackupKey", contents + i);
			ParameterValueList pvl = params.getValues(message, session);
			Message result = actor.doAction(message, pvl, session);

			String stringResult = result.asString();
			TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
			result.close();
		}
		waitForActionToFinish();

		assertFileExistsWithContents(null, filename, contents.trim() + (numOfWrites - 1));

		for (int i = 1; i <= numOfBackups; i++) {
			assertFileExistsWithContents(null, filename + "." + i, contents.trim() + (numOfWrites - 1 - i));
//			String actualContentsi = readFile(null, filename+"."+i);
//			assertEquals(contents.trim()+(numOfWrites-1-i), actualContentsi.trim());
		}
	}

	@Test
	public void fileSystemActorWriteActionWithFolder() throws Exception {
		String folderName = "path/to";
		String filename = "file.txt";

		if (_fileExists(folderName, filename)) {
			_deleteFile(folderName, filename);
		}

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withValue("tralala"));
		params.configure();

		actor.setCreateFolder(true);
		actor.setAction(FileSystemAction.WRITE);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(folderName + "/" + filename); //Flat file structure, should create folder
		ParameterValueList pvl = params.getValues(message, session);
		Message result = actor.doAction(message, pvl, session);
		TestAssertions.assertXpathValueEquals(filename, result.asString(), "file/@name");
		result.close();

		assertTrue(_fileExists(folderName, filename), "Expected the file [" + filename + "] to be present");
		assertTrue(fileSystem.folderExists(folderName), "existing folder is not seen"); // we just checked the file, the folder should be there...

		//Test if we can list items in the folder
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder(folderName);
		actor.configure(fileSystem, null, adapter);
		actor.open();
		Message result2 = actor.doAction(new Message(folderName), null, session);
		TestAssertions.assertXpathValueEquals(filename, result2.asString(), "directory/file/@name");
		result2.close();
	}

	@Test
	public void fileSystemActorWriteActionTestWithCreateFolderButNoFolderInFilename() throws Exception {
		String filename = UUID.randomUUID().toString();
		String contents = "Some text content to test upload action\n";

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}


		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("filename").withValue(filename));
		params.configure();

		actor.setCreateFolder(true);
		actor.setAction(FileSystemAction.WRITE);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(contents);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}


	@Test
	public void fileSystemActorAppendActionWriteLineSeparatorEnabled() throws Exception {
		int numOfWrites = 5;
		String filename = "AppendActionWriteLineSeparatorEnabled" + FILE1;
		String contents = "AppendActionWriteLineSeparatorEnabled";
		StringBuilder expectedMessageBuilder = new StringBuilder(contents);

		for (int i = 0; i < numOfWrites; i++) {
			expectedMessageBuilder.append(contents).append(i).append(lineSeparator);
		}

		fileSystemActorAppendActionWriteLineSeparatorTest(filename, contents, true, expectedMessageBuilder.toString(), numOfWrites);
	}

	@Test
	public void fileSystemActorAppendActionWriteLineSeparatorDisabled() throws Exception {
		int numOfWrites = 5;
		String filename = "AppendAction" + FILE1;
		String contents = "AppendAction";
		StringBuilder expectedMessageBuilder = new StringBuilder(contents);

		for (int i = 0; i < numOfWrites; i++) {
			expectedMessageBuilder.append(contents).append(i);
		}

		fileSystemActorAppendActionWriteLineSeparatorTest(filename, contents, false, expectedMessageBuilder.toString(), numOfWrites);
	}

	public void fileSystemActorAppendActionWriteLineSeparatorTest(String filename, String contents, boolean isWriteLineSeparator, String expected, int numOfWrites) throws Exception {
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		createFile(null, filename, contents);

		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("appendWriteLineSeparatorTest"));
		params.configure();

		actor.setWriteLineSeparator(isWriteLineSeparator);
		actor.setAction(FileSystemAction.APPEND);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		for (int i = 0; i < numOfWrites; i++) {
			Message sessionMessage = new Message(new ThrowingAfterCloseInputStream(new ByteArrayInputStream((contents + i).getBytes())));
			session.put("appendWriteLineSeparatorTest", sessionMessage);
			ParameterValueList pvl = params.getValues(message, session);
			Message result = actor.doAction(message, pvl, session);
			String resultStr = result.asString();

			TestAssertions.assertXpathValueEquals(filename, resultStr, "file/@name");
			result.close();
		}
		String actualContents = readFile(null, filename);

		assertEquals(expected, actualContents);
	}

	@Test
	public void fileSystemActorAppendActionWithRolloverBySize() throws Exception {
		String filename = "rolloverBySize" + FILE1;
		String contents = "thanos car ";
		int numOfBackups = 3;
		int numOfWrites = 5;
		int rotateSize = 10;

		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		createFile(null, filename, contents);

		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("contents").withSessionKey("appendActionwString"));
		params.configure();

		actor.setAction(FileSystemAction.APPEND);
		actor.setRotateSize(rotateSize);
		actor.setNumberOfBackups(numOfBackups);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		for (int i = 0; i < numOfWrites; i++) {
			session.put("appendActionwString", contents + i);
			ParameterValueList pvl = params.getValues(message, session);
			Message result = actor.doAction(message, pvl, session);
			String resultStr = result.asString();

			TestAssertions.assertXpathValueEquals(filename, resultStr, "file/@name");
			result.close();
		}

		int lastSavedBackup = numOfWrites < numOfBackups ? numOfWrites : numOfBackups;
		assertTrue(fileSystem.exists(fileSystem.toFile(filename + "." + lastSavedBackup)), "last backup with no " + lastSavedBackup + " does not exist");
		for (int i = 1; i <= numOfBackups; i++) {
			String actualContentsi = readFile(null, filename + "." + i);
			assertEquals((contents + (numOfWrites - 1 - i)).trim(), actualContentsi.trim(), "contents of backup no " + i + " is not correct");
		}
	}

	private void fileSystemActorRenameActionTest(boolean destinationExists) throws Exception {
		String filename = "toberenamed.txt";
		String dest = "renamed.txt";

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		if (destinationExists && !_fileExists(dest)) {
			createFile(null, dest, "original of destination");
		}

		ParameterList params = new ParameterList();

		params.add(new Parameter("destination", dest));
		actor.setAction(FileSystemAction.RENAME);
		params.configure();
		actor.configure(fileSystem, params, adapter);
		actor.open();

		deleteFile(null, dest);

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		// test
		assertEquals(dest, result.asString(), "result of actor should be name of new file");

		boolean actual = _fileExists(filename);
		// test
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		actual = _fileExists(dest);
		// test
		assertTrue(actual, "Expected file [" + dest + "] " + "to be present");
	}

	@Test
	public void fileSystemActorCreateActionTest() throws Exception {
		String filename = "tobecreated.txt";

		actor.setFilename(filename);
		actor.setAction(FileSystemAction.CREATE);
		actor.configure(fileSystem, null, adapter);
		actor.open();

		Message message = new Message(filename);
		actor.doAction(message, null, session);

		boolean actual = _fileExists(filename);

		assertTrue(actual, "Expected file [" + filename + "] " + "to be present");

		InputStream contents = _readFile(null, filename);
		// test
		assertEquals("", new Message(contents).asString(), "Expected file [" + filename + "] " + "to be empty");
	}

	@Test
	public void fileSystemActorCreateActionFilenameFromParameterTest() throws Exception {
		String filename = "tobecreated.txt";

		ParameterList params = new ParameterList();
		params.add(new Parameter("filename", filename));
		params.configure();

		actor.setFilename(filename);
		actor.setAction(FileSystemAction.CREATE);
		actor.configure(fileSystem, params, adapter);
		actor.open();

		Message message = new Message(filename);
		actor.doAction(message, params.getValues(message, session), session);

		boolean actual = _fileExists(filename);

		assertTrue(actual, "Expected file [" + filename + "] " + "to be present");

		InputStream contents = _readFile(null, filename);
		// test
		assertEquals("", new Message(contents).asString(), "Expected file [" + filename + "] " + "to be empty");
	}

	@Test
	public void fileSystemActorRenameActionTest() throws Exception {
		fileSystemActorRenameActionTest(false);
	}
}
