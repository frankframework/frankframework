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

import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.util.TestAssertions;

public abstract class FileSystemSenderTest<FSS extends FileSystemSender<F, FS>, F, FS extends IWritableFileSystem<F>> extends HelperedFileSystemTestBase {

	protected FSS fileSystemSender;

	public abstract FSS createFileSystemSender();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fileSystemSender = createFileSystemSender();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (fileSystemSender!=null) {
			fileSystemSender.close();
		};
		super.tearDown();
	}

	@Test
	public void fileSenderTestConfigure() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
	}

	@Test
	public void fileSenderTestOpen() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test
	public void fileSystemSenderUploadActionTestWithString() throws Exception {
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

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
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

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTargetwByteArray", contents.getBytes());

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTargetwByteArray");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
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

		InputStream stream = new ByteArrayInputStream(contents.getBytes("UTF-8"));
		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("uploadActionTarget", stream);

		Parameter p = new Parameter();
		p.setName("file");
		p.setSessionKey("uploadActionTarget");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("upload");
		fileSystemSender.configure();
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
		waitForActionToFinish();

		String actual = readFile(null, filename);
		// test
		// TODO: evaluate 'result'
		//assertEquals("result of sender should be input message",result,message);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void fileSystemSenderUploadActionTestWithOutputStream() throws Exception {
		String filename = "uploadedwithInputStream" + FILE1;
		String contents = "Some text content to test upload action\n";
		
		if (_fileExists(filename)) {
			_deleteFile(null, filename);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();

		Parameter param = new Parameter();
		param.setName("filename");
		param.setValue(filename);
		fileSystemSender.addParameter(param);

		fileSystemSender.setAction("upload");
		fileSystemSender.configure();
		fileSystemSender.open();

		assertTrue(fileSystemSender.canProvideOutputStream());

		String correlationId="fakecorrelationid";
		MessageOutputStream target = fileSystemSender.provideOutputStream(correlationId, session, null);

		// stream the contents
		try (Writer writer = target.asWriter()) {
			writer.write(contents);
		}

		// verify the filename is properly returned
		String stringResult=target.getResponseAsString();
		TestAssertions.assertXpathValueEquals(filename, stringResult, "file/@name");

		// verify the file contents
		waitForActionToFinish();
		String actualContents = readFile(null, filename);
		assertEquals(contents,actualContents);
	}

	@Test
	public void fileSystemSenderDownloadActionTest() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction("download");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
		
		// test
		assertEquals("result should be base64 of file content", contents.trim(), result.trim());
	}

	@Test
	public void fileSystemSenderDownloadActionBase64Test() throws Exception {
		String filename = "sender" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(null, filename, contents);
		waitForActionToFinish();

		fileSystemSender.setAction("download");
		fileSystemSender.configure();
		fileSystemSender.setBase64("encode");
		fileSystemSender.open();
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
		
		String contentsBase64 = Base64.encodeBase64String(contents.getBytes());
		// test
		assertEquals("result should be base64 of file content", contentsBase64.trim(), result.trim());
	}

	public void fileSystemSenderMoveActionTest(String folder1, String folder2) throws Exception {
		String filename = "sendermove" + FILE1;
		String contents = "Tekst om te lezen";
		
		if (folder1!=null) {
			_createFolder(folder1);
		}
		if (folder2!=null) {
			_createFolder(folder2);
		}
		createFile(folder1, filename, contents);
//		deleteFile(folder2, filename);
		waitForActionToFinish();

		fileSystemSender.setAction("move");
		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(folder2);
		fileSystemSender.addParameter(p);
		fileSystemSender.configure();
		fileSystemSender.open();
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
		
		// test
		// result should be name of the moved file
		assertNotNull(result);
		
		// TODO: result should point to new location of file
		// TODO: contents of result should be contents of original file
		
		// assertTrue("file should exist in destination folder ["+folder2+"]", _fileExists(folder2, filename)); // does not have to be this way. filename may have changed.
		assertFalse("file should not exist anymore in original folder ["+folder1+"]", _fileExists(folder1, filename));
	}

	@Test
	public void fileSystemSenderMoveActionTestRootToFolder() throws Exception {
		fileSystemSenderMoveActionTest(null,"folder");
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
	public void fileSystemSenderMkdirActionTest() throws Exception {
		String folder = "mkdir" + DIR1;
		
		if (_folderExists(folder)) {
			_deleteFolder(folder);
		}

		fileSystemSender.setAction("mkdir");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=folder;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
		waitForActionToFinish();

		// test
		
		boolean actual = _folderExists(folder);
		// test
		assertEquals("result of sender should be name of created folder",folder,result);
		assertTrue("Expected folder [" + folder + "] to be present", actual);
	}

	@Test
	public void fileSystemSenderRmdirActionTest() throws Exception {
		String folder = DIR1;
		
		if (!_folderExists(DIR1)) {
			_createFolder(folder);
		}

		fileSystemSender.setAction("rmdir");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=folder;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);

		// test
		assertEquals("result of sender should be name of deleted folder",folder,result);
		waitForActionToFinish();
		
		boolean actual = _folderExists(folder);
		// test
		assertFalse("Expected folder [" + folder + "] " + "not to be present", actual);
	}

	@Test
	public void fileSystemSenderDeleteActionTest() throws Exception {
		String filename = "tobedeleted" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		fileSystemSender.setAction("delete");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);

		waitForActionToFinish();
		
		boolean actual = _fileExists(filename);
		// test
		assertEquals("result of sender should be name of deleted file",filename,result);
		assertFalse("Expected file [" + filename + "] " + "not to be present", actual);
	}

	@Test
	public void fileSystemSenderRenameActionTest() throws Exception {
		String filename = "toberenamed" + FILE1;
		String dest = "renamed" + FILE1;
		
		if (!_fileExists(filename)) {
			createFile(null, filename, "is not empty");
		}

		Parameter p = new Parameter();
		p.setName("destination");
		p.setValue(dest);

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("rename");
		fileSystemSender.configure();
		fileSystemSender.open();

		deleteFile(null, dest);

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);

		// test
		assertEquals("result of sender should be new name of file",dest,result);

		boolean actual = _fileExists(filename);
		// test
		assertFalse("Expected file [" + filename + "] " + "not to be present", actual);

		actual = _fileExists(dest);
		// test
		assertTrue("Expected file [" + dest + "] " + "to be present", actual);
	}

	public void fileSystemSenderListActionTest(String inputFolder, int numberOfFiles) throws Exception {

		
		for (int i=0; i<numberOfFiles; i++) {
			String filename = "tobelisted"+i + FILE1;
			
			if (!_fileExists(filename)) {
				createFile(inputFolder, filename, "is not empty");
			}
		}
		
		fileSystemSender.setAction("list");
		if (inputFolder!=null) {
			fileSystemSender.setInputFolder(inputFolder);
		}
		fileSystemSender.configure();
		fileSystemSender.open();

		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(new PipeLineSessionBase());
		String correlationId="fakecorrelationid";
		String message="";
		String result = fileSystemSender.sendMessage(correlationId, message, prc);

		log.debug(result);
		
		// TODO test that the fileSystemSender has returned the an XML with the details of the file
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
	public void fileSystemSenderListActionTestInRootNoFiles() throws Exception {
		fileSystemSenderListActionTest(null,0);
	}
	@Test
	public void fileSystemSenderListActionTestInRoot() throws Exception {
		fileSystemSenderListActionTest(null,2);
	}

	@Test
	public void fileSystemSenderListActionTestInFolderNoFiles() throws Exception {
		_createFolder("folder");
		fileSystemSenderListActionTest("folder",0);
	}

	@Test
	public void fileSystemSenderListActionTestInFolder() throws Exception {
		_createFolder("folder");
		fileSystemSenderListActionTest("folder",2);
	}
	
	@Test(expected = SenderException.class)
	public void fileSystemSenderTestForFolderExistenceWithNonExistingFolder() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.setInputFolder("NonExistentFolder");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test
	public void fileSystemSenderTestForFolderExistenceWithExistingFolder() throws Exception {
		_createFolder("folder");
		fileSystemSender.setAction("list");
		fileSystemSender.setInputFolder("folder");
		fileSystemSender.configure();
		fileSystemSender.open();
	}

	@Test()
	public void fileSystemSenderTestForFolderExistenceWithRoot() throws Exception {
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.open();
	}
	
	@Test
	public void fileSystemSenderListActionTestWithInputFolderAsParameter() throws Exception {
		String filename = FILE1;
		String filename2 = FILE2;
		String inputFolder = "directory";
		
		if (_fileExists(inputFolder, filename)) {
			_deleteFile(inputFolder, filename);
		}
		
		if (_fileExists(inputFolder, filename2)) {
			_deleteFile(inputFolder, filename2);
		}

		PipeLineSessionBase session = new PipeLineSessionBase();
		session.put("listWithInputFolderAsParameter", inputFolder);

		Parameter p = new Parameter();
		p.setName("inputFolder");
		p.setSessionKey("listWithInputFolderAsParameter");

		fileSystemSender.addParameter(p);
		fileSystemSender.setAction("list");
		fileSystemSender.configure();
		fileSystemSender.open();
		
		_createFolder(inputFolder);
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
		
		ParameterResolutionContext prc = new ParameterResolutionContext();
		prc.setSession(session);
		String correlationId="fakecorrelationid";
		String message=filename;
		String result = fileSystemSender.sendMessage(correlationId, message, prc);
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