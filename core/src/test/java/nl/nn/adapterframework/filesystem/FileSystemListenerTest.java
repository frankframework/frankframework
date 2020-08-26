package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineResult;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.DateUtils;

public abstract class FileSystemListenerTest<F, FS extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {
	
	
	protected String fileAndFolderPrefix="";
	protected boolean testFullErrorMessages=true;

	protected FileSystemListener<F,FS> fileSystemListener;
	protected Map<String,Object> threadContext;

	
	public abstract FileSystemListener<F,FS> createFileSystemListener();

	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fileSystemListener = createFileSystemListener();
		threadContext=new HashMap<String,Object>();
	}

	@Override
	@After
	public void tearDown() throws Exception {
		if (fileSystemListener!=null) {
			fileSystemListener.close();
		};
		super.tearDown();
	}
	

	@Test
	public void fileListenerTestConfigure() throws Exception {
		fileSystemListener.configure();
	}

	@Test
	public void fileListenerTestOpen() throws Exception {
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	

	@Test
	public void fileListenerTestInvalidInputFolder() throws Exception {
		String folder=fileAndFolderPrefix+"xxx";
		fileSystemListener.setInputFolder(folder);
		if (testFullErrorMessages) {
			thrown.expectMessage("The value for inputFolder ["+folder+"], canonical name [");
			thrown.expectMessage("It is not a folder.");
		} else {
			thrown.expectMessage("["+folder+"] is invalid.");
		}
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestInvalidInProcessFolder() throws Exception {
		String folder=fileAndFolderPrefix+"xxx";
		fileSystemListener.setInProcessFolder(folder);
		if (testFullErrorMessages) {
			thrown.expectMessage("The value for inProcessFolder ["+folder+"], canonical name [");
			thrown.expectMessage("It is not a folder.");
		} else {
			thrown.expectMessage("["+folder+"] is invalid.");
		}
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestInvalidProcessedFolder() throws Exception {
		String folder=fileAndFolderPrefix+"xxx";
		fileSystemListener.setProcessedFolder(folder);
		if (testFullErrorMessages) {
			thrown.expectMessage("The value for processedFolder ["+folder+"], canonical name [");
			thrown.expectMessage("It is not a folder.");
		} else {
			thrown.expectMessage("["+folder+"] is invalid.");
		}
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestCreateInputFolder() throws Exception {
		fileSystemListener.setInputFolder(fileAndFolderPrefix+"xxx1");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestCreateInProcessFolder() throws Exception {
		fileSystemListener.setInProcessFolder(fileAndFolderPrefix+"xxx2");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestCreateProcessedFolder() throws Exception {
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+"xxx3");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}

	@Test
	public void fileListenerTestCreateLogFolder() throws Exception {
		fileSystemListener.setLogFolder(fileAndFolderPrefix+"xxx4");
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	/*
	 * vary this on: 
	 *   inputFolder
	 *   inProcessFolder
	 */
	public void fileListenerTestGetRawMessage(String inputFolder, String inProcessFolder) throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.setMinStableTime(0);
		if (inputFolder!=null) {
			fileSystemListener.setInputFolder(inputFolder);
		}
		if (inProcessFolder!=null) {
			fileSystemListener.setInProcessFolder(fileAndFolderPrefix+inProcessFolder);
			_createFolder(inProcessFolder);
			waitForActionToFinish();
		}
		fileSystemListener.configure();
		fileSystemListener.open();
		
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not available",rawMessage);
		
		createFile(null, filename, contents);

		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when a file is available",rawMessage);
		
		F secondMessage=fileSystemListener.getRawMessage(threadContext);
		if (inProcessFolder!=null) {
			assertNull("raw message must have been moved to inProcessFolder",secondMessage);			
		} else {
			assertNotNull("raw message must still be available when no inProcessFolder is configured",secondMessage);			
		}

	}

	@Test
	public void fileListenerTestGetRawMessage() throws Exception {
		fileListenerTestGetRawMessage(null,null);
	}
	
	@Test
	public void fileListenerTestGetRawMessageWithInProcess() throws Exception {
		String folderName = "inProcessFolder";
		_createFolder(folderName);
		fileListenerTestGetRawMessage(null,folderName);
	}


	@Test
	public void fileListenerTestGetStringFromRawMessageFilename() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.open();
		
		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		
		Message message=fileSystemListener.extractMessage(rawMessage, threadContext);
		assertThat(message.asString(),CoreMatchers.containsString(filename));
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageContents() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setMessageType("contents");
		fileSystemListener.configure();
		fileSystemListener.open();
		
		createFile(null, filename, contents);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		
		Message message=fileSystemListener.extractMessage(rawMessage, threadContext);
		assertEquals(contents,message.asString());
	}

//	@Test
//	public void fileListenerTestGetStringFromRawMessageOtherAttribute() throws Exception {
//		throw new NotImplementedException();
//	}
	
	
	/*
	 * Test for proper id
	 * Test for additionalProperties in session variables
	 */
	@Test
	public void fileListenerTestGetIdFromRawMessage() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.open();
		
		createFile(null, filename, contents);
	
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		
		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertThat(id, Matchers.endsWith(filename));
	}

	/*
	 * Test for proper id
	 * Test for additionalProperties in session variables
	 */
	@Test
	public void fileListenerTestGetIdFromRawMessageFileTimeSensitive() throws Exception {
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setFileTimeSensitive(true);
		fileSystemListener.configure();
		fileSystemListener.open();
		
		createFile(null, filename, contents);
	
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		
		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertThat(id, Matchers.containsString(filename));
		String currentDateFormatted=DateUtils.format(new Date());
		String timestamp=id.substring(id.length()-currentDateFormatted.length());
		long currentDate=DateUtils.parseAnyDate(currentDateFormatted).getTime();
		long timestampDate=DateUtils.parseAnyDate(timestamp).getTime();
		assertTrue(Math.abs(timestampDate-currentDate)<7300000); // less then two hours in milliseconds.
	}
	
	@Test
	public void fileListenerTestAfterMessageProcessedDeleteAndCopy() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;
		String logFolder = "logFolder";
		String contents = "contents of file";
		
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.setLogFolder(fileAndFolderPrefix+logFolder);
		fileSystemListener.setCreateFolders(true);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState("success");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
		assertFileExistsWithContents(logFolder, filename, contents);
	}


	@Test
	public void fileListenerTestAfterMessageProcessedMoveFile() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		
		createFile(null,fileName, "");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState("success");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		
		
		assertTrue("Destination folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}

	@Test
	public void fileListenerTestAfterMessageProcessedMoveFileOverwrite() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		
		createFile(null,fileName, "");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		_createFolder(processedFolder);
		createFile(processedFolder,fileName, "");
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.setOverwrite(true);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState("success");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		
		
		assertTrue("Destination folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}

	@Test
	public void fileListenerTestAfterMessageProcessedErrorDelete() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;
		
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setDelete(true);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState("error");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
	}


	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToErrorFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		String errorFolder = "errorFolder";
		
		createFile(null,fileName, "");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		_createFolder(processedFolder);
		_createFolder(errorFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.setErrorFolder(fileAndFolderPrefix+errorFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState("error");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		
		
		assertTrue("Error folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist in error folder",_fileExists(errorFolder, fileName));
		assertTrue("Destination must not exist in processed folder",!_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}
	@Test
	public void fileListenerTestAfterMessageProcessedErrorMoveFileToProcessedFolder() throws Exception {
		String fileName = "fileTobeMoved.txt";
		String processedFolder = "destinationFolder";
		
		createFile(null,fileName, "");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		_createFolder(processedFolder);
		waitForActionToFinish();

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.setProcessedFolder(fileAndFolderPrefix+processedFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		PipeLineResult processResult = new PipeLineResult();
		processResult.setState("error");
		fileSystemListener.afterMessageProcessed(processResult, rawMessage, null);
		waitForActionToFinish();
		
		
		assertTrue("Error folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist in processed folder",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}


}