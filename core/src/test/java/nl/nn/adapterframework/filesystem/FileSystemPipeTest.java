package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.testutil.TestAssertions;

public abstract class FileSystemPipeTest<FSP extends FileSystemPipe<F, FS>, F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FSP fileSystemPipe;

	public abstract FSP createFileSystemPipe();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fileSystemPipe = createFileSystemPipe();
		fileSystemPipe.registerForward(new PipeForward("success",null));
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (fileSystemPipe!=null) {
			fileSystemPipe.stop();
		};
		super.tearDown();
	}

	@Test
	public void fileSystemPipeTestConfigure() throws Exception {
		fileSystemPipe.setAction("list");
		fileSystemPipe.configure();
	}

	@Test
	public void fileSystemPipeTestOpen() throws Exception {
		fileSystemPipe.setAction("list");
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}

	@Test
	public void fileSystemPipeUploadActionTestWithString() throws Exception {
		String filename = "uploadedwithString" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwString", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwString");

		fileSystemPipe.addParameter(p);
		fileSystemPipe.setAction("upload");
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();
		waitForActionToFinish();
		
		TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemPipeUploadActionTestWithByteArray() throws Exception {
		String filename = "uploadedwithByteArray" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwByteArray");

		fileSystemPipe.addParameter(p);
		fileSystemPipe.setAction("upload");
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();
		TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		waitForActionToFinish();


		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemPipeUploadActionTestWithInputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTarget", stream);

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTarget");

		fileSystemPipe.addParameter(p);
		fileSystemPipe.setAction("upload");
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, session);
		String result=prr.getResult().asString();
		TestAssertions.assertXpathValueEquals(filename, result, "file/@name");
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemPipeUploadActionTestWithOutputStream() throws Exception {
		String filename = "uploadedwithOutputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		//InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		PipeLineSessionBase session = new PipeLineSessionBase();
		//session.put("uploadActionTarget", stream);

		Parameter param = new Parameter();
		param.setName("filename");
		param.setValue(filename);
		fileSystemPipe.addParameter(param);
		fileSystemPipe.setAction("upload");
		fileSystemPipe.configure();
		fileSystemPipe.start();

		assertTrue(fileSystemPipe.canProvideOutputStream());

		MessageOutputStream target = fileSystemPipe.provideOutputStream(session, null);

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
		
		PipeForward forward = target.getForward();
		assertNotNull(forward);
		assertEquals("success", forward.getName());
	}
	
	
	@Test
	public void fileSystemPipeDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemPipe.setAction("download");
		fileSystemPipe.configure();
		fileSystemPipe.start();
		
		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();
		
		// test
		assertEquals("result should be base64 of file content", contents.trim(), result.trim());
	}

	public void fileSystemPipeMoveActionTest(String folder1, String folder2, boolean folderExists, boolean setCreateFolderAttribute) throws Exception {
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

		fileSystemPipe.setAction("move");
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(folder2);
		fileSystemPipe.addParameter(p);
		if (setCreateFolderAttribute) {
			fileSystemPipe.setCreateFolder(true);
		}
		fileSystemPipe.configure();
		fileSystemPipe.start();
		
		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();
		
		// test
		// result should be name of the moved file
		assertNotNull(result);
		
		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file
		
		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse("file should not exist anymore in original folder ["+folder1+"]", _fileExists(folder1, filename));
	}

	@Test
	public void fileSystemPipeMoveActionTestRootToFolder() throws Exception {
		fileSystemPipeMoveActionTest(null,"folder",true,false);
	}
	@Test
	public void fileSystemPipeMoveActionTestRootToFolderCreateFolder() throws Exception {
		fileSystemPipeMoveActionTest(null,"folder",false,true);
	}
	@Test
	public void fileSystemPipeMoveActionTestRootToFolderFailIfolderDoesNotExist() throws Exception {
		thrown.expectMessage("unable to process [move] action for File [sendermovefile1.txt]: destination folder [folder] does not exist");
		fileSystemPipeMoveActionTest(null,"folder",false,false);
	}
//	@Test
//	public void fileSystemPipeMoveActionTestFolderToRoot() throws Exception {
//		fileSystemPipeMoveActionTest("folder",null);
//	}
//	@Test
//	public void fileSystemPipeMoveActionTestFolderToFolder() throws Exception {
//		fileSystemPipeMoveActionTest("folder1","folder2");
//	}

	@Test
	public void fileSystemPipeMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;
		
		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		fileSystemPipe.setAction("mkdir");
		fileSystemPipe.configure();
		fileSystemPipe.start();
		
		Message message= new Message(folder);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();
		waitForActionToFinish();

		// test
		
		boolean actual = _folderExists(folder);
		// test
		assertEquals("result of pipe should be name of created folder",folder,result);
		assertTrue("Expected folder [" + folder + "] to be present", actual);
	}

	@Test
	public void fileSystemPipeRmdirActionTest() throws Exception {
		String folder = DIR1;
		
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		fileSystemPipe.setAction("rmdir");
		fileSystemPipe.configure();
		fileSystemPipe.start();
		
		Message message= new Message(folder);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();

		// test
		assertEquals("result of pipe should be name of removed folder",folder,result);
		waitForActionToFinish();
		
		boolean actual = _fileExists(folder);
		// test
		assertFalse("Expected file [" + folder + "] " + "not to be present", actual);
	}

	@Test
	public void fileSystemPipeDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemPipe.setAction("delete");
		fileSystemPipe.configure();
		fileSystemPipe.start();
		
		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();

		waitForActionToFinish();
		
		boolean actual = _fileExists(filename);
		// test
		assertEquals("result of pipe should be name of deleted file",filename,result);
		assertFalse("Expected file [" + filename + "] " + "not to be present", actual);
	}

	@Test
	public void fileSystemPipeRenameActionTest() throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(dest);

		fileSystemPipe.addParameter(p);
		fileSystemPipe.setAction("rename");
		fileSystemPipe.configure();
		fileSystemPipe.start();

		deleteFile(null, dest);

		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();

		// test
		assertEquals("result of pipe should be name of new file",dest,result);

		boolean actual = _fileExists(filename);
		// test
		assertFalse("Expected file [" + filename + "] " + "not to be present", actual);

		actual = _fileExists(dest);
		// test
		assertTrue("Expected file [" + dest + "] " + "to be present", actual);
	}

	public void fileSystemPipeListActionTest(String inputFolder, int numberOfFiles) throws Exception {

		
		for (int i=0; i<numberOfFiles; i++) {
			String filename = "tobelisted"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}
		
		fileSystemPipe.setAction("list");
		if (inputFolder!=null) {
			fileSystemPipe.setInputFolder(inputFolder);
		}
		fileSystemPipe.configure();
		fileSystemPipe.start();

		Message message= new Message("");
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();

		log.debug(result);
		
		// TODO test that the fileSystemPipe has returned the an XML with the details of the file
//		Iterator<F> it = result;
//		int count = 0;
//		while (it.hasNext()) {
//			it.next();
//			count++;
//		}
		
		String anchor=" count=\"";
		int posCount=result.indexOf(anchor);
		if (posCount<0) {
			fail("result does not contain anchor ["+anchor+"]");
		}
		int posQuote=result.indexOf('"',posCount+anchor.length());
		
		int resultCount = Integer.valueOf(result.substring(posCount+anchor.length(), posQuote));
		// test
		assertEquals("count mismatch",numberOfFiles, resultCount);
		assertEquals("mismatch in number of files",numberOfFiles, resultCount);
	}

	@Test
	public void fileSystemPipeListActionTestInRootNoFiles() throws Exception {
		fileSystemPipeListActionTest(null,0);
	}
	@Test
	public void fileSystemPipeListActionTestInRoot() throws Exception {
		fileSystemPipeListActionTest(null,2);
	}

	@Test
	public void fileSystemPipeListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemPipeListActionTest("folder",0);
	}

	@Test
	public void fileSystemPipeListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemPipeListActionTest("folder",2);
	}
	
	@Test(expected = PipeStartException.class)
	public void fileSystemPipeTestForFolderExistenceWithNonExistingFolder() throws Exception {
		fileSystemPipe.setAction("list");
		fileSystemPipe.setInputFolder("NonExistentFolder");
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}

	@Test
	public void fileSystemPipeTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		fileSystemPipe.setAction("list");
		fileSystemPipe.setInputFolder("folder");
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}

	@Test()
	public void fileSystemPipeTestForFolderExistenceWithRoot() throws Exception {
		fileSystemPipe.setAction("list");
		fileSystemPipe.configure();
		fileSystemPipe.start();
	}
	
	@Test
	public void fileSystemPipeListActionTestWithInputFolderAsParameter() throws Exception {
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";
		
		if (_fileExists(inputFolder, filename)) {
			_deleteFile(inputFolder, filename);
		}
		
		if (_fileExists(inputFolder, filename2)) {
			_deleteFile(inputFolder, filename2);
		}
		_createFolder(inputFolder);

		Parameter p = new Parameter();
		p.setName("inputFolder");
		p.setValue(inputFolder);

		fileSystemPipe.addParameter(p);
		fileSystemPipe.setAction("list");
		fileSystemPipe.configure();
		fileSystemPipe.start();
		
		OutputStream out = _createFile(inputFolder, filename);
		out.write("some content".getBytes());
		out.close();
		waitForActionToFinish();
		assertTrue("File ["+filename+"]expected to be present", _fileExists(inputFolder, filename));
		
		OutputStream out2 = _createFile(inputFolder, filename2);
		out2.write("some content of second file".getBytes());
		out2.close();
		waitForActionToFinish();
		assertTrue("File ["+filename2+"]expected to be present", _fileExists(inputFolder, filename2));
		
		Message message= new Message(filename);
		PipeRunResult prr = fileSystemPipe.doPipe(message, null);
		String result=prr.getResult().asString();
		System.err.println(result);
		waitForActionToFinish();
		
		String anchor=" count=\"";
		int posCount=result.indexOf(anchor);
		if (posCount<0) {
			fail("result does not contain anchor ["+anchor+"]");
		}
		int posQuote=result.indexOf('"',posCount+anchor.length());
		
		int resultCount = Integer.valueOf(result.substring(posCount+anchor.length(), posQuote));
		// test
		assertEquals("count mismatch", 2, resultCount);
		assertEquals("mismatch in number of files", 2, resultCount);
	}
}