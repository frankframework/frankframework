package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.MethodName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.IConfigurable;
import org.frankframework.core.PipeLineSession;
import org.frankframework.filesystem.FileSystemActor.FileSystemAction;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestAssertions;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;
import org.frankframework.util.CloseUtils;

@TestMethodOrder(MethodName.class)
public abstract class FileSystemActorTest<F, FS extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {

	private final String lineSeparator = System.getProperty("line.separator");
	protected FileSystemActor<F, FS> actor;
	protected FS fileSystem;
	protected IConfigurable owner;
	protected Message result;

	protected abstract FS createFileSystem();

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		owner = adapter;
		adapter.setName("fake owner of FileSystemActor");
		fileSystem = createFileSystem();
		autowireByName(fileSystem);
		fileSystem.configure();
		fileSystem.open();
		actor = new FileSystemActor<>();
		result = null;
	}

	@Override
	@AfterEach
	public void tearDown() {
		CloseUtils.closeSilently(result, fileSystem);

		super.tearDown();
	}

	@Test
	public void fileSystemActorTestConfigureBasic() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, null, owner);
	}

	@Test
	public void fileSystemActorTestConfigureNoAction() throws Exception {
		ConfigurationException e = assertThrows(ConfigurationException.class, () -> actor.configure(fileSystem, null, owner));
		assertThat(e.getMessage(), containsString("either attribute [action] or parameter [action] must be specified"));
		assertThat(e.getMessage(), containsString("fake owner of FileSystemActor"));
	}

	@Test
	public void fileSystemActorEmptyParameterAction() throws Exception {
		String filename = "emptyParameterAction" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		params.add(new Parameter("action", ""));
		params.configure();

		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(new Message(""), session);

		FileSystemException e = assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));
		assertThat(e.getMessage(), containsString("unable to resolve the value of parameter"));
	}

	@Test
	public void fileSystemActorEmptyParameterActionWillBeOverridenByConfiguredAction() throws Exception {
		String filename = "overwriteEmptyParameter" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		params.add(ParameterBuilder.create().withName("action"));
		params.configure();
		actor.setAction(FileSystemAction.READ);
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(null, session);

		result = actor.doAction(message, pvl, session);

		assertEquals(contents, result.asString());
	}

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
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(null, session);

		result = actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorTestBasicOpen() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, null, owner);
		actor.open();
	}

	@Test
	public void fileSystemActorTestConfigureInputDirectoryForListActionDoesNotExist() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder("xxx");
		actor.configure(fileSystem, null, owner);

		FolderNotFoundException e = assertThrows(FolderNotFoundException.class, actor::open);
		assertThat(e.getMessage(), containsString("inputFolder [xxx], canonical name ["));
		assertThat(e.getMessage(), containsString("does not exist"));
	}

	@Test
	public void fileSystemActorTestConfigureInputDirectoryForListActionDoesNotExistButAllowCreate() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.setCreateFolder(true);
		actor.setInputFolder("xxx");
		actor.configure(fileSystem, null, owner);
		actor.open();
	}


	@Test
	public void fileSystemActorListActionTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder("folder");
		actor.configure(fileSystem, null, owner);
		actor.open();
	}

	@Test()
	public void fileSystemActorListActionTestForFolderExistenceWithRoot() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, null, owner);
		actor.open();
	}

	@Test()
	public void fileSystemActorListActionWhenDuplicateConfigurationAttributeHasPreference() throws Exception {
		actor.setAction(FileSystemAction.LIST);
		actor.setInputFolder("folder1");
		ParameterList params = new ParameterList();
		params.add(new Parameter("inputFolder", "folder2"));
		actor.configure(fileSystem, params, owner);

		FolderNotFoundException e = assertThrows(FolderNotFoundException.class, actor::open);
		assertThat(e.getMessage(), containsString("inputFolder [folder1], canonical name ["));
		assertThat(e.getMessage(), containsString("does not exist"));
	}

	public void fileSystemActorListActionTest(String inputFolder, int numberOfFiles, int expectedNumberOfFiles) throws Exception {
		for (int i = 0; i < numberOfFiles; i++) {
			String filename = "tobelisted" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}

		actor.setAction(FileSystemAction.LIST);
		if (inputFolder != null) {
			actor.setInputFolder(inputFolder);
		}
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		log.debug(result);

		// TODO test that the fileSystemSender has returned the an XML with the details of the file
//		Iterator<F> it = result;
//		int count = 0;
//		while (it.hasNext()) {
//			it.next();
//			count++;
//		}

		assertFileCountEquals(result, expectedNumberOfFiles);
		message.close();
	}

	@Test
	public void fileSystemActorListActionTestInRootNoFiles() throws Exception {
		fileSystemActorListActionTest(null, 0, 0);
	}

	@Test
	public void fileSystemActorListActionTestInRoot() throws Exception {
		fileSystemActorListActionTest(null, 2, 2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 0, 0);
	}

	@Test
	public void fileSystemActorListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 2, 2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithWildCard() throws Exception {
		actor.setWildcard("*d0*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 5, 1);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithExcludeWildCard() throws Exception {
		actor.setExcludeWildcard("*d0*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 5, 4);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithBothWildCardAndExcludeWildCard() throws Exception {
		actor.setWildcard("*.txt");
		actor.setExcludeWildcard("*ted1*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder", 5, 4);
	}

	@Test
	public void migrated_localFileSystemTestListWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename + ".bak";
		String filename2 = filename + ".xml";
		String contents = "regeltje tekst";

		actor.setWildcard("*.xml");
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, null, owner);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();
		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}

	@Test
	public void migrated_localFileSystemTestListExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename + ".bak";
		String filename2 = filename + ".xml";
		String contents = "regeltje tekst";

		actor.setExcludeWildcard("*.bak");
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, null, owner);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();

		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}


	@Test
	public void migrated_localFileSystemTestListIncludeExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename + ".oud.xml";
		String filename2 = filename + ".xml";
		String contents = "regeltje tekst";

		actor.setWildcard("*.xml");
		actor.setExcludeWildcard("*.oud.xml");
		actor.setAction(FileSystemAction.LIST);
		actor.configure(fileSystem, null, owner);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		String stringResult = result.asString();

		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}


	@Test
	public void fileSystemActorListActionTestWithInputFolderAsParameter() throws Exception {
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";

		if (_fileExists(inputFolder, filename)) {
			_deleteFile(inputFolder, filename);
		}

		if (_fileExists(inputFolder, filename2)) {
			_deleteFile(inputFolder, filename2);
		}


		ParameterList params = new ParameterList();

		params.add(new Parameter("inputFolder", inputFolder));
		actor.setAction(FileSystemAction.LIST);
		params.configure();
		actor.configure(fileSystem, params, owner);
		actor.open();

		_createFolder(inputFolder);
		createFile(inputFolder, filename, "some content");
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, filename), "File [" + filename + "] expected to be present");

		createFile(inputFolder, filename2, "some content of second file");
		waitForActionToFinish();
		assertTrue(_fileExists(inputFolder, filename2), "File [" + filename2 + "] expected to be present");

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);

		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		assertFileCountEquals(result, 2);
	}

	public void fileSystemActorInfoActionTest(boolean fileViaAttribute) throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.INFO);
		if (fileViaAttribute) {
			actor.setFilename(filename);
		}
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(fileViaAttribute ? null : filename);
		ParameterValueList pvl = null;

		// Act
		result = actor.doAction(message, pvl, session);
		String resultStr = result.asString();

		assertThat(resultStr, containsString("<file name=\"senderfile1.txt\""));
		assertThat(resultStr, containsString("size=\"17\""));
		assertThat(resultStr, containsString("canonicalName="));
		assertThat(resultStr, containsString("modificationDate="));
		assertThat(resultStr, containsString("modificationTime="));
	}

	@Test
	public void fileSystemActorInfoActionTest() throws Exception {
		fileSystemActorInfoActionTest(false);
	}

	@Test
	public void fileSystemActorInfoActionTestFilenameViaAttribute() throws Exception {
		fileSystemActorInfoActionTest(true);
	}

	@Test
	public void fileSystemActorReadActionFromParameterTest() throws Exception {
		String filename = "parameterAction" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		params.add(new Parameter("action", "read"));
		params.configure();

		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);

		result = actor.doAction(message, pvl, session);

		assertEquals(contents, result.asString());
		assertTrue(_fileExists(filename));
	}

	public void fileSystemActorReadActionTest(FileSystemAction action, boolean fileViaAttribute, boolean fileShouldStillExistAfterwards) throws Exception {
		fileSystemActorReadActionTest(action, fileViaAttribute, fileShouldStillExistAfterwards, "Tekst om te lezen");
	}

	public void fileSystemActorReadActionTest(FileSystemAction action, boolean fileViaAttribute, boolean fileShouldStillExistAfterwards, String contents) throws Exception {
		String filename = "sender" + FILE1;

		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(action);
		if (fileViaAttribute) {
			actor.setFilename(filename);
		}
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(fileViaAttribute ? null : filename);
		ParameterValueList pvl = null;

		result = Message.asMessage(actor.doAction(message, pvl, session));

		if(contents == null) {
			assertTrue(Message.isEmpty(result));
		} else {
			assertEquals(contents, result.asString());
		}
		assertEquals(fileShouldStillExistAfterwards, _fileExists(filename));
	}

	@Test
	public void testReadEmptyFile() throws Exception {
		// Test to prove that we work around the bug in AWS v2 api that handles empty files incorrectly
		// in org.frankframework.filesystem.AmazonS3FileSystem.readFile
		fileSystemActorReadActionTest(FileSystemAction.READ, false, true, null);
	}

	@Test
	public void fileSystemActorReadActionTest() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.READ, false, true);
	}

	@Test
	public void fileSystemActorReadActionTestFilenameViaAttribute() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.READ, true, true);
	}

	@Test
	public void fileSystemActorReadActionTestCompatiblity() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.DOWNLOAD, false, true);
	}

	@Test
	public void fileSystemActorReadDeleteActionTest() throws Exception {
		fileSystemActorReadActionTest(FileSystemAction.READDELETE, false, false);
	}

	@Test
	public void fileSystemActorReadDeleteActionWithDeleteEmptyFolderTest() throws Exception {
		String filename = "presender" + FILE1;
		String contents = "Tekst om te lezen";
		String folder = "inner";

		_createFolder(folder);
		createFile(folder, filename, contents);
		waitForActionToFinish();

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.READDELETE);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder + "/" + filename);
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);

		assertEquals(contents, result.asString());
		assertFalse(_fileExists(filename), "Expected file [" + filename + "] not to be present");
		assertFalse(_fileExists(filename), "Expected file [" + filename + "] not to be present");

		assertFalse(_folderExists(folder), "Expected parent folder not to be present");
	}

	@Test
	public void fileSystemActorReadWithCharsetUseDefault() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "€ $ & ^ % @ < é ë ó ú à è";

		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.READ);
		actor.setFilename(filename);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = null;

		result = actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorReadWithCharsetUseIncompatible() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "€ è";
		String expected = "â¬ Ã¨";

		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.READ);
		actor.setFilename(filename);
		actor.setCharset("ISO-8859-1");
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = null;

		Message result = actor.doAction(message, pvl, session);
		assertEquals(expected, result.asString());
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
		actor.configure(fileSystem, null, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, null, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, params, owner);
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

	@Test
	public void fileSystemActorMoveActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		actor.setWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", destFolderName));
		params.configure();
		actor.setCreateFolder(true);
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertFalse(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorMoveActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		actor.setExcludeWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", destFolderName));
		params.configure();
		actor.setCreateFolder(true);
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertFalse(_fileExists(srcFolderName, filename));
		}
	}

	@Test()
	public void fileSystemActorMoveActionTestForDestinationParameter() throws Exception {
		actor.setAction(FileSystemAction.MOVE);

		ConfigurationException e = assertThrows(ConfigurationException.class, () -> actor.configure(fileSystem, null, owner));
		assertThat(e.getMessage(), endsWith("the [MOVE] action requires the parameter [destination] or the attribute [destination] to be present"));
	}

	public void fileSystemActorMoveActionTest(String srcFolder, String destFolder, boolean createDestFolder, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (srcFolder != null) {
			_createFolder(srcFolder);
		}
		if (createDestFolder && destFolder != null) {
			_createFolder(destFolder);
		}
		createFile(srcFolder, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.MOVE);
		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", destFolder));
		params.configure();

		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		// test
		// result should be name of the moved file
		assertNotNull(result);

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse(_fileExists(srcFolder, filename), "file should not exist anymore in original folder [" + srcFolder + "]");
	}


	@Test
	public void fileSystemActorMoveActionTestRootToFolder() throws Exception {
		fileSystemActorMoveActionTest(null, "folder", true, false);
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemActorMoveActionTest(null, "folder", false, true);
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		FileSystemException e = assertThrows(FileSystemException.class, () -> fileSystemActorMoveActionTest(null, "folder", false, false));
		assertThat(e.getMessage(), containsString("unable to process [MOVE] action for File [sendermovefile1.txt]: destination folder [folder] does not exist"));
	}

	@Test
	public void fileSystemActorMoveActionTestRootToFolderExistsAndAllowToCreate() throws Exception {
		fileSystemActorMoveActionTest(null, "folder", true, true);
	}

	@Test
	public void fileSystemActorMoveActionWithDeleteEmptyFolderTest() throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		String destinationFolder = "deleteEmptyFolder";

		_createFolder("innerFolder");
		_createFolder(destinationFolder);

		createFile("innerFolder", filename, contents);

		waitForActionToFinish();

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.MOVE);
		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", destinationFolder));
		params.configure();

		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message("innerFolder/" + filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		assertNotNull(result);
		assertFalse(_fileExists(filename), "file should not exist anymore in original folder");
		assertTrue(_fileExists(destinationFolder, filename), "file should not exist anymore in original folder");

		assertFalse(_folderExists("innerFolder"), "Expected parent folder not to be present");
	}
//	@Test
//	public void fileSystemSenderMoveActionTestFolderToRoot() throws Exception {
//		fileSystemSenderMoveActionTest("folder",null);
//	}
//	@Test
//	public void fileSystemSenderMoveActionTestFolderToFolder() throws Exception {
//		fileSystemSenderMoveActionTest("folder1","folder2");
//	}

	@Test
	public void fileSystemActorCopyActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.COPY);
		actor.setWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);

		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", destFolderName));
		params.configure();

		actor.setCreateFolder(true);
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorCopyActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();

		actor.setAction(FileSystemAction.COPY);
		actor.setExcludeWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);

		ParameterList params = new ParameterList();
		params.add(new Parameter("destination", destFolderName));
		params.configure();

		actor.setCreateFolder(true);
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		result = actor.doAction(m, pvl, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	public void fileSystemActorCopyActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";

		if (folder1 != null) {
			_createFolder(folder1);
		}
		if (folderExists && folder2 != null) {
			_createFolder(folder2);
		}
		createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction(FileSystemAction.COPY);
		actor.setDestination(folder2);
		ParameterList params = new ParameterList();
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		params.configure();
		actor.configure(fileSystem, params, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		result = actor.doAction(message, pvl, session);

		// test
		// result should be name of the moved file
		// assertNotNull(result); from 7.8, result is allowed to be null

		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file

		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertTrue(_fileExists(folder1, filename), "file should still exist anymore in original folder [" + folder1 + "]");
	}

	@Test
	public void fileSystemActorCopyActionTestRootToFolder() throws Exception {
		fileSystemActorCopyActionTest(null, "folder", true, false);
	}


	@Test
	public void fileSystemActorMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;

		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		actor.setAction(FileSystemAction.MKDIR);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		// test

		boolean actual = _folderExists(folder);
		// test
		assertEquals(folder, result.asString(), "result of actor should be name of created folder");
		assertTrue(actual, "Expected folder [" + folder + "] to be present");
	}

	@Test
	public void fileSystemActorRmdirActionTest() throws Exception {
		String folder = DIR1;

		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		actor.setAction(FileSystemAction.RMDIR);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		// test
		assertEquals(folder, result.asString(), "result of actor should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _folderExists(folder);
		// test
		assertFalse(actual, "Expected folder [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemActorRmNonEmptyDirActionTest() throws Exception {
		String folder = DIR1;
		String innerFolder = DIR1 + "/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}
		for (int i = 0; i < 3; i++) {
			String filename = "file" + i + FILE1;
			createFile(folder, filename, "is not empty");
			createFile(innerFolder, filename, "is not empty");
		}

		actor.setAction(FileSystemAction.RMDIR);
		actor.setRemoveNonEmptyFolder(true);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		// test
		assertEquals(folder, result.asString(), "result of actor should be name of removed folder");
		waitForActionToFinish();

		boolean actual = _folderExists(folder);
		// test
		assertFalse(actual, "Expected folder [" + folder + "] " + "not to be present");
	}

	@Test
	public void fileSystemActorAttemptToRmNonEmptyDir() throws Exception {
		String folder = DIR1;
		String innerFolder = DIR1 + "/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}
		for (int i = 0; i < 3; i++) {
			String filename = "file" + i + FILE1;
			createFile(folder, filename, "is not empty");
			createFile(innerFolder, filename, "is not empty");
		}

		assertTrue(_fileExists(innerFolder, "file0file1.txt"), "Expected file [" + innerFolder + "/file0file1.txt] to be present");

		actor.setAction(FileSystemAction.RMDIR);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder);
		ParameterValueList pvl = null;
		FileSystemException e = assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));
		assertThat(e.getMessage(), containsString("unable to process [RMDIR] action for File [testDirectory]: Cannot remove folder"));

		// Clean up
		message.close();
	}

	@Test
	public void fileSystemActorDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;

		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		actor.setAction(FileSystemAction.DELETE);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		waitForActionToFinish();

		boolean actual = _fileExists(filename);
		// test
		assertEquals(filename, result.asString(), "result of sender should be name of deleted file");
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");

		message.close();
	}

	@Test
	public void fileSystemActorDeleteActionWithDeleteEmptyFolderTest() throws Exception {
		String filename = "filetobedeleted" + FILE1;
		String folder = "inner";

		_deleteFolder(folder);
		_createFolder(folder);
		createFile(folder, filename, "is not empty");

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.DELETE);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder + "/" + filename);
		ParameterValueList pvl = null;
		result = actor.doAction(message, pvl, session);

		waitForActionToFinish();

		boolean actual = _fileExists(filename);
		// test
		assertEquals(filename, result.asString(), "result of sender should be name of deleted file");
		assertFalse(actual, "Expected file [" + filename + "] " + "not to be present");
		assertFalse(_folderExists(folder), "Expected parent folder not to be present");
		message.close();
	}

	@Test
	//Should not be able to clean up directory after removing 'filename' because there are still 2 empty folders on the same root. Tests if list detects DIRECTORIES
	public void fileSystemActorDeleteActionWithDeleteEmptyFolderRootContainsEmptyFoldersTest() throws Exception {
		String filename = "filetobedeleted" + FILE1;
		final String folder = "inner";

		_createFolder(folder);
		_createFolder(folder + "/innerFolder1");
		_createFolder(folder + "/innerFolder2");
		createFile(folder, filename, "is not empty");

		actor.setDeleteEmptyFolder(true);
		actor.setAction(FileSystemAction.DELETE);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message message = new Message(folder + "/" + filename);
		ParameterValueList pvl = null;
		assertThrows(FileSystemException.class, () -> actor.doAction(message, pvl, session));

		waitForActionToFinish();

		// test
		assertFalse(_fileExists(filename), "Expected file [" + filename + "] " + "not to be present in the root");
		assertTrue(_folderExists(folder), "Expected parent folder to be present");
		assertTrue(_folderExists(folder + "/innerFolder1"), "Expected file in parent folder to be present");
		message.close();
	}

	@Test
	public void fileSystemActorDeleteActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);

		for (int i = 0; i < 3; i++) {
			String filename = "tobedeleted" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}

			filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		waitForActionToFinish();

		actor.setAction(FileSystemAction.DELETE);
		actor.setWildcard("tobedeleted*");
		actor.setInputFolder(srcFolderName);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message m = new Message("");
		result = actor.doAction(m, null, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertFalse(_fileExists(srcFolderName, filename));
			filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorDeleteActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);

		for (int i = 0; i < 3; i++) {
			String filename = "tobedeleted" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}

			filename = "tostay" + i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		waitForActionToFinish();

		actor.setAction(FileSystemAction.DELETE);
		actor.setExcludeWildcard("tostay*");
		actor.setInputFolder(srcFolderName);
		actor.configure(fileSystem, null, owner);
		actor.open();

		Message m = new Message("");
		result = actor.doAction(m, null, session);

		for (int i = 0; i < 3; i++) {
			String filename = "tobemoved" + i + FILE1;
			assertFalse(_fileExists(srcFolderName, filename));
			filename = "tostay" + i + FILE1;
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	public void fileSystemActorRenameActionTest(boolean destinationExists) throws Exception {
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
		actor.configure(fileSystem, params, owner);
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
		actor.configure(fileSystem, null, owner);
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
		actor.configure(fileSystem, params, owner);
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
