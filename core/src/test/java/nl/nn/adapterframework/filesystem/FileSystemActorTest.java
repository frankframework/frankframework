package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Date;

import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurable;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.testutil.TestAssertions;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class FileSystemActorTest<F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FileSystemActor<F, FS> actor;

	protected FS fileSystem;
	protected IConfigurable owner;
	private PipeLineSession session;

	private final String lineSeparator = System.getProperty("line.separator");

	protected abstract FS createFileSystem();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		owner= new IConfigurable() {

			@Override
			public String getName() {
				return "fake owner of FileSystemActor";
			}
			@Override
			public void setName(String newName) {
				throw new IllegalStateException("setName() should not be called");
			}
			@Override
			public ClassLoader getConfigurationClassLoader() {
				return Thread.currentThread().getContextClassLoader();
			}
			@Override
			public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
				// Ignore
			}
			@Override
			public void configure() throws ConfigurationException {
				// Ignore
			}
			@Override
			public ApplicationContext getApplicationContext() {
				return null;
			}
		};
		fileSystem = createFileSystem();
		fileSystem.configure();
		fileSystem.open();
		actor=new FileSystemActor<F, FS>();
	}

//	@Override
//	@After
//	public void tearDown() throws Exception {
//		if (actor!=null) {
//			actor.close();
//		};
//		super.tearDown();
//	}

	@Test
	public void fileSystemActorTestConfigureBasic() throws Exception {
		actor.setAction("list");
		actor.configure(fileSystem,null,owner);
	}

	@Test
	public void fileSystemActorTestConfigureNoAction() throws Exception {
		thrown.expectMessage("either attribute [action] or parameter [action] must be specified");
		thrown.expectMessage("fake owner of FileSystemActor");
		actor.configure(fileSystem,null,owner);
	}

	@Test
	public void fileSystemActorEmptyParameterAction() throws Exception {
		thrown.expectMessage("unable to resolve the value of parameter");
		String filename = "emptyParameterAction" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("action");
		p.setValue("");
		params.add(p);
		params.configure();

		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message= new Message(filename);
		ParameterValueList pvl = params.getValues(new Message(""), session);

		actor.doAction(message, pvl, session);
	}

	@Test
	public void fileSystemActorEmptyParameterActionWillBeOverridenByConfiguredAction() throws Exception {
		String filename = "overwriteEmptyParameter" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("action");
		p.setValue(null);
		params.add(p);
		params.configure();
		actor.setAction("read");
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message= new Message(filename);
		ParameterValueList pvl = params.getValues(null, session);

		Message result = (Message)actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorParameterActionAndAttributeActionConfigured() throws Exception {
		String filename = "actionParamAndAttr" + FILE1;
		String contents = "Text to read";

		createFile(null, filename, contents);
		waitForActionToFinish();

		ParameterList params = new ParameterList();
		Parameter pAction = new Parameter();
		pAction.setName("action");
		pAction.setValue("read");
		params.add(pAction);
		Parameter pFilename = new Parameter();
		pFilename.setName("filename");
		pFilename.setValue(filename);
		params.add(pFilename);
		params.configure();

		actor.setAction("write");
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message= new Message(filename);
		ParameterValueList pvl = params.getValues(null, session);

		Message result = (Message)actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
	}

	@Test
	public void fileSystemActorTestConfigureInvalidAction() throws Exception {
		thrown.expectMessage("unknown or invalid action [xxx]");
		thrown.expectMessage("fake owner of FileSystemActor");
		actor.setAction("xxx");
		actor.configure(fileSystem,null,owner);
	}

	@Test
	public void fileSystemActorTestBasicOpen() throws Exception {
		actor.setAction("list");
		actor.configure(fileSystem,null,owner);
		actor.open();
	}

	@Test
	public void fileSystemActorTestConfigureInputDirectoryForListActionDoesNotExist() throws Exception {
		thrown.expectMessage("inputFolder [xxx], canonical name [");
		thrown.expectMessage("does not exist");
		actor.setAction("list");
		actor.setInputFolder("xxx");
		actor.configure(fileSystem,null,owner);
		actor.open();
	}

	@Test
	public void fileSystemActorTestConfigureInputDirectoryForListActionDoesNotExistButAllowCreate() throws Exception {
		actor.setAction("list");
		actor.setCreateFolder(true);
		actor.setInputFolder("xxx");
		actor.configure(fileSystem,null,owner);
		actor.open();
	}


	@Test
	public void fileSystemActorListActionTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		actor.setAction("list");
		actor.setInputFolder("folder");
		actor.configure(fileSystem,null,owner);
		actor.open();
	}

	@Test()
	public void fileSystemActorListActionTestForFolderExistenceWithRoot() throws Exception {
		actor.setAction("list");
		actor.configure(fileSystem,null,owner);
		actor.open();
	}

	@Test()
	public void fileSystemActorListActionWhenDuplicateConfigurationAttributeHasPreference() throws Exception {
		actor.setAction("list");
		actor.setInputFolder("folder1");
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("inputFolder");
		p.setValue("folder2");
		params.add(p);
		thrown.expectMessage("inputFolder [folder1], canonical name [");
		thrown.expectMessage("does not exist");
		actor.configure(fileSystem,params,owner);
		actor.open();
	}
	
	public void fileSystemActorListActionTest(String inputFolder, int numberOfFiles, int expectedNumberOfFiles) throws Exception {

		
		for (int i=0; i<numberOfFiles; i++) {
			String filename = "tobelisted"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}
		
		actor.setAction("list");
		if (inputFolder!=null) {
			actor.setInputFolder(inputFolder);
		}
		actor.configure(fileSystem,null,owner);
		actor.open();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);
		String stringResult=(String)result;

		log.debug(result);
		
		// TODO test that the fileSystemSender has returned the an XML with the details of the file
//		Iterator<F> it = result;
//		int count = 0;
//		while (it.hasNext()) {
//			it.next();
//			count++;
//		}
		
		String anchor=" count=\"";
		int posCount=stringResult.indexOf(anchor);
		if (posCount<0) {
			fail("result does not contain anchor ["+anchor+"]");
		}
		int posQuote=stringResult.indexOf('"',posCount+anchor.length());
		
		int resultCount = Integer.valueOf(stringResult.substring(posCount+anchor.length(), posQuote));
		// test
		assertEquals("count mismatch",expectedNumberOfFiles, resultCount);
		assertEquals("mismatch in number of files",expectedNumberOfFiles, resultCount);
	}

	@Test
	public void fileSystemActorListActionTestInRootNoFiles() throws Exception {
		fileSystemActorListActionTest(null,0,0);
	}
	@Test
	public void fileSystemActorListActionTestInRoot() throws Exception {
		fileSystemActorListActionTest(null,2,2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder",0,0);
	}

	@Test
	public void fileSystemActorListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder",2,2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithWildCard() throws Exception {
		actor.setWildCard("*d0*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder",5,1);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithExcludeWildCard() throws Exception {
		actor.setExcludeWildcard("*d0*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder",5,4);
	}

	@Test
	public void fileSystemActorListActionTestInFolderWithBothWildCardAndExcludeWildCard() throws Exception {
		actor.setWildCard("*.txt");
		actor.setExcludeWildcard("*ted1*");
		_createFolder("folder");
		fileSystemActorListActionTest("folder",5,4);
	}
	@Test
	public void migrated_localFileSystemTestListWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename+".bak";
		String filename2 = filename+".xml";
		String contents = "regeltje tekst";

		actor.setWildCard("*.xml");
		actor.setAction("list");
		actor.configure(fileSystem,null,owner);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);
		String stringResult=(String)result;
		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}
	
	@Test
	public void migrated_localFileSystemTestListExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename+".bak";
		String filename2 = filename+".xml";
		String contents = "regeltje tekst";
		
		actor.setExcludeWildcard("*.bak");
		actor.setAction("list");
		actor.configure(fileSystem,null,owner);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();
		
		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);
		String stringResult=(String)result;
		
		assertTrue(stringResult.contains(filename2));
		assertFalse(stringResult.contains(filename1));
	}



	@Test
	public void migrated_localFileSystemTestListIncludeExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename+".oud.xml";
		String filename2 = filename+".xml";
		String contents = "regeltje tekst";
		
		actor.setWildCard("*.xml");
		actor.setExcludeWildcard("*.oud.xml");
		actor.setAction("list");
		actor.configure(fileSystem,null,owner);
		actor.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();

		Message message = new Message("");
		PipeLineSession session = new PipeLineSession();
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);
		String stringResult=(String)result;

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
		Parameter p = new Parameter();
		p.setName("inputFolder");
		p.setValue(inputFolder);

		params.add(p);
		actor.setAction("list");
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		_createFolder(inputFolder);
		OutputStream out = _createFile(inputFolder, filename);
		out.write("some content".getBytes());
		out.close();
		waitForActionToFinish();
		assertTrue("File ["+filename+"] expected to be present", _fileExists(inputFolder, filename));
		
		OutputStream out2 = _createFile(inputFolder, filename2);
		out2.write("some content of second file".getBytes());
		out2.close();
		waitForActionToFinish();
		assertTrue("File ["+filename2+"] expected to be present", _fileExists(inputFolder, filename2));
		
		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);
		System.err.println(result);
		String stringResult=(String)result;
		waitForActionToFinish();
		
		String anchor=" count=\"";
		int posCount=stringResult.indexOf(anchor);
		if (posCount<0) {
			fail("result does not contain anchor ["+anchor+"]");
		}
		int posQuote=stringResult.indexOf('"',posCount+anchor.length());
		
		int resultCount = Integer.valueOf(stringResult.substring(posCount+anchor.length(), posQuote));
		// test
		assertEquals("count mismatch", 2, resultCount);
		assertEquals("mismatch in number of files", 2, resultCount);
	}

	public void fileSystemActorInfoActionTest(boolean fileViaAttribute) throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction("info");
		if (fileViaAttribute) {
			actor.setFilename(filename);
		}
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message= new Message(fileViaAttribute?null:filename);
		ParameterValueList pvl = null;
		String result = (String)actor.doAction(message, pvl, session);
		assertThat(result,StringContains.containsString("<file name=\"senderfile1.txt\""));
		assertThat(result,StringContains.containsString("size=\"17\""));
		assertThat(result,StringContains.containsString("canonicalName="));
		assertThat(result,StringContains.containsString("modificationDate="));
		assertThat(result,StringContains.containsString("modificationTime="));
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
		Parameter p = new Parameter();
		p.setName("action");
		p.setValue("read");
		params.add(p);
		params.configure();
		
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message= new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);

		Message result = (Message)actor.doAction(message, pvl, session);
		assertEquals(contents, result.asString());
		assertTrue(_fileExists(filename));
	}

	public void fileSystemActorReadActionTest(String action, boolean fileViaAttribute, boolean fileShouldStillExistAfterwards) throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction(action);
		if (fileViaAttribute) {
			actor.setFilename(filename);
		}
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message= new Message(fileViaAttribute?null:filename);
		ParameterValueList pvl = null;

		Message result = Message.asMessage(actor.doAction(message, pvl, session));
		assertEquals(contents, result.asString());
		assertEquals(fileShouldStillExistAfterwards, _fileExists(filename));
	}

	@Test
	public void fileSystemActorReadActionTest() throws Exception {
		fileSystemActorReadActionTest("read",false, true);
	}

	@Test
	public void fileSystemActorReadActionTestFilenameViaAttribute() throws Exception {
		fileSystemActorReadActionTest("read",true, true);
	}

	@Test
	public void fileSystemActorReadActionTestCompatiblity() throws Exception {
		fileSystemActorReadActionTest("download",false, true);
	}

	@Test
	public void fileSystemActorReadDeleteActionTest() throws Exception {
		fileSystemActorReadActionTest("readDelete",false, false);
	}

	@Test
	public void fileSystemActorReadWithCharsetUseDefault() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "€ $ & ^ % @ < é ë ó ú à è";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction("read");
		actor.setFilename(filename);
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message= new Message(filename);
		ParameterValueList pvl = null;

		Message result = Message.asMessage(actor.doAction(message, pvl, session));
		assertEquals(contents, result.asString());
	}
	
	@Test
	public void fileSystemActorReadWithCharsetUseIncompatible() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "€ è";
		String expected = "â¬ Ã¨";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		actor.setAction("read");
		actor.setFilename(filename);
		actor.setCharset("ISO-8859-1");
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message= new Message(filename);
		ParameterValueList pvl = null;

		Message result = Message.asMessage(actor.doAction(message, pvl, session));
		assertEquals(expected, result.asString());
	}
	
	@Test
	public void fileSystemActorWriteWithNoCharsetUsed() throws Exception {
		String filename = "senderwriteWithCharsetUseDefault" + FILE1;
		String contents = "€ $ & ^ % @ < é ë ó ú à è";

		PipeLineSession session = new PipeLineSession();
		session.put("senderwriteWithCharsetUseDefault", contents);

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("senderwriteWithCharsetUseDefault");
		params.add(p);
		params.configure();

		waitForActionToFinish();

		actor.setAction("write");
		actor.setFilename(filename);
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message= new Message(contents);
		ParameterValueList pvl = params.getValues(message, session);
		
		actor.doAction(message, pvl, session);

		String actualContents = readFile(null, filename);
		assertEquals(contents, actualContents);
	}

	@Test
	public void fileSystemActorWriteActionTestWithStringAndUploadAsAction() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTargetwString", contents);

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("uploadActionTargetwString");

		params.add(p);
		actor.setAction("upload");
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);
		waitForActionToFinish();
		
		String stringResult=(String)result;
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		
		String actualContents = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actualContents.trim());
	}

	@Test
	public void fileSystemActorWriteActionWriteLineSeparator() throws Exception {
		String filename = "writeLineSeparator" + FILE1;
		String contents = "Some text content to test write action writeLineSeparator enabled";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();
		session.put("writeLineSeparator", contents);

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("writeLineSeparator");

		params.add(p);
		actor.setWriteLineSeparator(true);
		actor.setAction("write");
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		String stringResult=(String)result;
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		
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
		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwByteArray");

		params.add(p);
		actor.setAction("write");
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl= params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);

		String stringResult=(String)result;
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		waitForActionToFinish();


		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemActorWriteActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		PipeLineSession session = new PipeLineSession();
		session.put("uploadActionTarget", stream);

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTarget");

		params.add(p);
		actor.setAction("write");
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);

		String stringResult=(String)result;
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemActorWriteActionTestWithOutputStream() throws Exception {
		String filename = "uploadedwithOutputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

//		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		PipeLineSession session = new PipeLineSession();

		ParameterList paramlist = new ParameterList();
		Parameter param = new Parameter();
		param.setName("filename");
		param.setValue(filename);
		paramlist.add(param);
		paramlist.configure();
		
		actor.setAction("write");
		actor.configure(fileSystem,paramlist,owner);
		actor.open();
		
		assertTrue(actor.canProvideOutputStream());
		
		MessageOutputStream target = actor.provideOutputStream(session, null);

		// stream the contents
		try (Writer writer = target.asWriter()) {
			writer.write(contents);
		}

		// verify the filename is properly returned
		String stringResult=target.getPipeRunResult().getResult().asString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
	
		// verify the file contents
		waitForActionToFinish();
		String actualContents = readFile(null, filename);
		assertEquals(contents,actualContents);
	}

	@Test
	public void fileSystemActorWriteActionWithBackup() throws Exception {
		String filename = "writeAndBackupTest.txt";
		String contents = "text content:";
		int numOfBackups=3;
		int numOfWrites=5;
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSession session = new PipeLineSession();

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("fileSystemActorWriteActionWithBackupKey");

		params.add(p);
		actor.setAction("write");
		actor.setNumberOfBackups(numOfBackups);
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message= new Message(filename);
		for (int i=0;i<numOfWrites;i++) {
			session.put("fileSystemActorWriteActionWithBackupKey", contents+i);
			ParameterValueList pvl= params.getValues(message, session);
			Object result = actor.doAction(message, pvl, null);

			String stringResult=(String)result;
			TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		}
		waitForActionToFinish();
		
		assertFileExistsWithContents(null, filename, contents.trim()+(numOfWrites-1));
		
		for (int i=1;i<=numOfBackups;i++) {
			assertFileExistsWithContents(null, filename+"."+i, contents.trim()+(numOfWrites-1-i));
//			String actualContentsi = readFile(null, filename+"."+i);
//			assertEquals(contents.trim()+(numOfWrites-1-i), actualContentsi.trim());
		}
	}

	@Test
	public void fileSystemActorAppendActionWriteLineSeparatorEnabled() throws Exception {
		int numOfWrites = 5;
		String filename = "AppendActionWriteLineSeparatorEnabled" + FILE1;
		String contents = "AppendActionWriteLineSeparatorEnabled";
		StringBuilder expectedMessageBuilder = new StringBuilder(contents);
		
		for(int i=0; i<numOfWrites; i++) {
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
		
		for(int i=0; i<numOfWrites; i++) {
			expectedMessageBuilder.append(contents).append(i);
		}
		
		fileSystemActorAppendActionWriteLineSeparatorTest(filename, contents, false, expectedMessageBuilder.toString(), numOfWrites);
	}

	public void fileSystemActorAppendActionWriteLineSeparatorTest(String filename, String contents, boolean isWriteLineSeparator, String expected, int numOfWrites) throws Exception {
		if(_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		createFile(null, filename, contents);

		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();

		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("appendWriteLineSeparatorTest");
		params.add(p);
		params.configure();
		
		actor.setWriteLineSeparator(isWriteLineSeparator);
		actor.setAction("append");
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message message = new Message(filename);
		for(int i=0; i<numOfWrites; i++) {
			session.put("appendWriteLineSeparatorTest", contents+i);
			ParameterValueList pvl = params.getValues(message, session);
			String result = (String)actor.doAction(message, pvl, null);

			TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
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
		
		if(_fileExists(filename)) {
			_deleteFile(null, filename);
		}
		createFile(null, filename, contents);
		
		PipeLineSession session = new PipeLineSession();
		ParameterList params = new ParameterList();
		
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("appendActionwString");
		params.add(p);
		params.configure();
		
		actor.setAction("append");
		actor.setRotateSize(rotateSize);
		actor.setNumberOfBackups(numOfBackups);
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message message = new Message(filename);
		for(int i=0; i<numOfWrites; i++) {
			session.put("appendActionwString", contents+i);
			ParameterValueList pvl = params.getValues(message, session);
			String result = (String)actor.doAction(message, pvl, null);

			TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		}

		int lastSavedBackup=numOfWrites<numOfBackups ? numOfWrites : numOfBackups;
		assertTrue("last backup with no "+lastSavedBackup+" does not exist",fileSystem.exists(fileSystem.toFile(filename+"."+lastSavedBackup)));
		for (int i=1;i<=numOfBackups;i++) {
			String actualContentsi = readFile(null, filename+"."+i);
			assertEquals("contents of backup no "+i+" is not correct",(contents+(numOfWrites-1-i)).trim(), actualContentsi.trim());
		}
	}

	@Test
	public void fileSystemActorMoveActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		
		for (int i=0; i < 3; i++) {
			String filename = "tostay"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();
		
		actor.setAction("move");
		actor.setWildCard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(destFolderName);
		params.add(p);
		actor.setCreateFolder(true);
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		Object result = actor.doAction(m, pvl, session);
		
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertFalse(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorMoveActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		
		for (int i=0; i < 3; i++) {
			String filename = "tostay"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();
		
		actor.setAction("move");
		actor.setExcludeWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(destFolderName);
		params.add(p);
		actor.setCreateFolder(true);
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		Object result = actor.doAction(m, pvl, session);
		
		for (int i=0; i < 3; i++) {
			String filename = "tostay"+i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertFalse(_fileExists(srcFolderName, filename));
		}
	}
	
	@Test()
	public void fileSystemActorMoveActionTestForDestinationParameter() throws Exception {
		actor.setAction("move");
		thrown.expectMessage("the move action requires the parameter [destination] or the attribute [destination] to be present");
		actor.configure(fileSystem,null,owner);
	}
	
	public void fileSystemActorMoveActionTest(String srcFolder, String destFolder, boolean createDestFolder, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		
		if (srcFolder!=null) {
			_createFolder(srcFolder);
		}
		if (createDestFolder && destFolder!=null) {
			_createFolder(destFolder);
		}
		createFile(srcFolder, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction("move");
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(destFolder);
		params.add(p);
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);
		
		// test
		// result should be name of the moved file
		assertNotNull(result);
		
		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file
		
		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse("file should not exist anymore in original folder ["+srcFolder+"]", _fileExists(srcFolder, filename));
	}


	@Test
	public void fileSystemActorMoveActionTestRootToFolder() throws Exception {
		fileSystemActorMoveActionTest(null,"folder",true,false);
	}
	@Test
	public void fileSystemActorMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemActorMoveActionTest(null,"folder",false,true);
	}
	@Test
	public void fileSystemActorMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		thrown.expectMessage("unable to process [move] action for File [sendermovefile1.txt]: destination folder [folder] does not exist");
		fileSystemActorMoveActionTest(null,"folder",false,false);
	}
	@Test
	public void fileSystemActorMoveActionTestRootToFolderExistsAndAllowToCreate() throws Exception {
		fileSystemActorMoveActionTest(null,"folder",true,true);
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
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		for (int i=0; i < 3; i++) {
			String filename = "tostay"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();
		
		actor.setAction("copy");
		actor.setWildCard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(destFolderName);
		params.add(p);
		actor.setCreateFolder(true);
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		Object result = actor.doAction(m, pvl, session);
		
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorCopyActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);
		String destFolderName = "dest" + new Date().getTime();
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		
		for (int i=0; i < 3; i++) {
			String filename = "tostay"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		waitForActionToFinish();
		
		actor.setAction("copy");
		actor.setExcludeWildcard("tobemoved*");
		actor.setInputFolder(srcFolderName);
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(destFolderName);
		params.add(p);
		actor.setCreateFolder(true);
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message m = new Message("");
		ParameterValueList pvl = params.getValues(m, session);
		Object result = actor.doAction(m, pvl, session);
		
		for (int i=0; i < 3; i++) {
			String filename = "tostay"+i + FILE1;
			assertTrue(_fileExists(destFolderName, filename));
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	public void fileSystemActorCopyActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		
		if (folder1!=null) {
			_createFolder(folder1);
		}
		if (folderExists && folder2!=null) {
			_createFolder(folder2);
		}
		createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		actor.setAction("copy");
		actor.setDestination(folder2);
		ParameterList params = new ParameterList();
		if (setCreateFolderAttribute) {
			actor.setCreateFolder(true);
		}
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();
		
		Message message = new Message(filename);
		ParameterValueList pvl = params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);
		
		// test
		// result should be name of the moved file
		assertNotNull(result);
		
		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file
		
		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertTrue("file should still exist anymore in original folder ["+folder1+"]", _fileExists(folder1, filename));
	}

	@Test
	public void fileSystemActorCopyActionTestRootToFolder() throws Exception {
		fileSystemActorCopyActionTest(null,"folder",true,false);
	}

	
	@Test
	public void fileSystemActorMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;
		
		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		actor.setAction("mkdir");
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message = new Message(folder);
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);
		waitForActionToFinish();

		// test
		
		boolean actual = _folderExists(folder);
		// test
		assertEquals("result of actor should be name of created folder",folder,result);
		assertTrue("Expected folder [" + folder + "] to be present", actual);
	}

	@Test
	public void fileSystemActorRmdirActionTest() throws Exception {
		String folder = DIR1;
		
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		actor.setAction("rmdir");
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message = new Message(folder);
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);

		// test
		assertEquals("result of actor should be name of removed folder",folder,result);
		waitForActionToFinish();
		
		boolean actual = _folderExists(folder);
		// test
		assertFalse("Expected folder [" + folder + "] " + "not to be present", actual);
	}
	@Test
	public void fileSystemActorRmNonEmptyDirActionTest() throws Exception {
		String folder = DIR1;
		String innerFolder = DIR1+"/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}
		for (int i=0; i < 3; i++) {
			String filename = "file"+i + FILE1;
			createFile(folder, filename, "is not empty");
			createFile(innerFolder, filename, "is not empty");
		}

		actor.setAction("rmdir");
		actor.setRemoveNonEmptyFolder(true);
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message = new Message(folder);
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);

		// test
		assertEquals("result of actor should be name of removed folder",folder,result);
		waitForActionToFinish();
		
		boolean actual = _folderExists(folder);
		// test
		assertFalse("Expected folder [" + folder + "] " + "not to be present", actual);
	}
	
	@Test
	public void fileSystemActorAttemptToRmNonEmptyDir() throws Exception {
		thrown.expectMessage("Cannot remove folder");
		String folder = DIR1;
		String innerFolder = DIR1+"/innerFolder";
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
		}
		for (int i=0; i < 3; i++) {
			String filename = "file"+i + FILE1;
			createFile(folder, filename, "is not empty");
			createFile(innerFolder, filename, "is not empty");
		}

		actor.setAction("rmdir");
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message = new Message(folder);
		ParameterValueList pvl = null;
		actor.doAction(message, pvl, session);

	}
	
	@Test
	public void fileSystemActorDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		actor.setAction("delete");
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message message = new Message(filename);
		ParameterValueList pvl = null;
		Object result = actor.doAction(message, pvl, session);

		waitForActionToFinish();
		
		boolean actual = _fileExists(filename);
		// test
		assertEquals("result of sender should be name of deleted file",filename,result);
		assertFalse("Expected file [" + filename + "] " + "not to be present", actual);
	}

	@Test
	public void fileSystemActorDeleteActionTestWithWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);

		for (int i=0; i < 3; i++) {
			String filename = "tobedeleted"+i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}

			filename = "tostay"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}

		waitForActionToFinish();
		
		actor.setAction("delete");
		actor.setWildCard("tobedeleted*");
		actor.setInputFolder(srcFolderName);
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message m = new Message("");
		Object result = actor.doAction(m, null, session);
		
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			assertFalse(_fileExists(srcFolderName, filename));
			filename = "tostay"+i + FILE1;
			assertTrue(_fileExists(srcFolderName, filename));
		}
	}

	@Test
	public void fileSystemActorDeleteActionTestWithExcludeWildCard() throws Exception {
		String srcFolderName = "src" + new Date().getTime();
		_createFolder(srcFolderName);

		for (int i=0; i < 3; i++) {
			String filename = "tobedeleted"+i + FILE1;

			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}

			filename = "tostay"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(srcFolderName, filename, "is not empty");
			}
		}
		
		waitForActionToFinish();
		
		actor.setAction("delete");
		actor.setExcludeWildcard("tostay*");
		actor.setInputFolder(srcFolderName);
		actor.configure(fileSystem,null,owner);
		actor.open();
		
		Message m = new Message("");
		Object result = actor.doAction(m, null, session);
		
		for (int i=0; i < 3; i++) {
			String filename = "tobemoved"+i + FILE1;
			assertFalse(_fileExists(srcFolderName, filename));
			filename = "tostay"+i + FILE1;
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
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(dest);

		params.add(p);
		actor.setAction("rename");
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		deleteFile(null, dest);

		Message message = new Message(filename);
		ParameterValueList pvl= params.getValues(message, session);
		Object result = actor.doAction(message, pvl, session);

		// test
		assertEquals("result of actor should be name of new file",dest,result);

		boolean actual = _fileExists(filename);
		// test
		assertFalse("Expected file [" + filename + "] " + "not to be present", actual);

		actual = _fileExists(dest);
		// test
		assertTrue("Expected file [" + dest + "] " + "to be present", actual);
	}

	@Test
	public void fileSystemActorRenameActionTest() throws Exception {
		fileSystemActorRenameActionTest(false);
	}

//	@Test
////	@Ignore("MockFileSystem.exists appears not to work properly")
//	public void fileSystemActorRenameActionTestDestinationExists() throws Exception {
//		thrown.expectMessage("destination exists");
//		fileSystemActorRenameActionTest(true);
//	}
	
	
	
	protected ParameterValueList createParameterValueList(ParameterList paramList, Message input, PipeLineSession session) throws ParameterException {
		if (paramList==null) {
			return null;
		}
		return paramList.getValues(input, session);
	}
	
	
}