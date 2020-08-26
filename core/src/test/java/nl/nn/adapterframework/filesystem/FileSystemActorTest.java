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

import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringContains;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import nl.nn.adapterframework.core.INamedObject;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.util.Misc;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class FileSystemActorTest<F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FileSystemActor<F, FS> actor;

	protected FS fileSystem;
	protected INamedObject owner;
	private IPipeLineSession session;


	protected abstract FS createFileSystem();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		owner= new INamedObject() {

			@Override
			public String getName() {
				return "fake owner of FileSystemActor";
			}
			@Override
			public void setName(String newName) {
				throw new IllegalStateException("setName() should not be called");
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
		thrown.expectMessage("action must be specified");
		thrown.expectMessage("fake owner of FileSystemActor");
		actor.configure(fileSystem,null,owner);
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
	
	public void fileSystemActorListActionTest(String inputFolder, int numberOfFiles) throws Exception {

		
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
		IPipeLineSession session = new PipeLineSessionBase();
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
		assertEquals("count mismatch",numberOfFiles, resultCount);
		assertEquals("mismatch in number of files",numberOfFiles, resultCount);
	}

	@Test
	public void fileSystemActorListActionTestInRootNoFiles() throws Exception {
		fileSystemActorListActionTest(null,0);
	}
	@Test
	public void fileSystemActorListActionTestInRoot() throws Exception {
		fileSystemActorListActionTest(null,2);
	}

	@Test
	public void fileSystemActorListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder",0);
	}

	@Test
	public void fileSystemActorListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemActorListActionTest("folder",2);
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
		Object result = actor.doAction(message, pvl, session);
		assertThat(result, IsInstanceOf.instanceOf(InputStream.class));
		String actualContents = Misc.streamToString((InputStream)result);
		assertEquals(contents, actualContents);
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
	public void fileSystemActorWriteActionTestWithStringAndUploadAsAction() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
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
	public void fileSystemActorWriteActionTestWithByteArrayAndContentsViaAlternativeParameter() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
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
		PipeLineSessionBase session = new PipeLineSessionBase();
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
		PipeLineSessionBase session = new PipeLineSessionBase();

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
		String filename = "uploadedwithString" + FILE1;
		String contents = "text content:";
		int numOfBackups=3;
		int numOfWrites=5;
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();

		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("contents");
		p.setSessionKey("uploadActionTargetwString");

		params.add(p);
		actor.setAction("write");
		actor.setNumberOfBackups(numOfBackups);
		params.configure();
		actor.configure(fileSystem,params,owner);
		actor.open();

		Message message= new Message(filename);
		for (int i=0;i<numOfWrites;i++) {
			session.put("uploadActionTargetwString", contents+i);
			ParameterValueList pvl= params.getValues(message, session);
			Object result = actor.doAction(message, pvl, null);

			String stringResult=(String)result;
			TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");
		}
		waitForActionToFinish();
		
		
		String actualContents = readFile(null, filename);
		assertEquals(contents.trim()+(numOfWrites-1), actualContents.trim());
		
		for (int i=1;i<=numOfBackups;i++) {
			String actualContentsi = readFile(null, filename+"."+i);
			assertEquals(contents.trim()+(numOfWrites-1-i), actualContentsi.trim());
		}
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
		createFile(null, filename, "thanos car ");
		
		PipeLineSessionBase session = new PipeLineSessionBase();
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

		assertTrue(fileSystem.exists(fileSystem.toFile(filename+"."+(numOfWrites<numOfBackups?numOfWrites:numOfBackups))));
		for (int i=1;i<=numOfBackups;i++) {
			String actualContentsi = readFile(null, filename+"."+i);
			assertEquals((contents+(numOfWrites-1-i)).trim(), actualContentsi.trim());
		}
	}
	
	
	@Test()
	public void fileSystemActorMoveActionTestForDestinationParameter() throws Exception {
		actor.setAction("move");
		thrown.expectMessage("the move action requires the parameter [destination] or the attribute [destination] to be present");
		actor.configure(fileSystem,null,owner);
	}
	
	public void fileSystemActorMoveActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
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

		actor.setAction("move");
		ParameterList params = new ParameterList();
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(folder2);
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
		assertFalse("file should not exist anymore in original folder ["+folder1+"]", _fileExists(folder1, filename));
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

	public void fileSystemActorRenameActionTest(boolean destinationExists) throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;
		
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
	
	
	
	protected ParameterValueList createParameterValueList(ParameterList paramList, Message input, IPipeLineSession session) throws ParameterException {
		if (paramList==null) {
			return null;
		}
		return paramList.getValues(input, session);
	}
	
	
}