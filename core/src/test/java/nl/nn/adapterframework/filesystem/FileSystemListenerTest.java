package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public abstract class FileSystemListenerTest<F, FS extends IBasicFileSystem<F>> extends HelperedFileSystemTestBase {

	private IFileSystemListener<F> fileSystemListener;
	private Map<String,Object> threadContext;

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	
	public abstract IFileSystemListener<F> createFileSystemListener();

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
		fileSystemListener.setInputFolder("xxx");
		thrown.expectMessage("The value for inputFolder [xxx] is invalid. It is not a folder.");
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestInvalidInProcessFolder() throws Exception {
		fileSystemListener.setInProcessFolder("xxx");
		thrown.expectMessage("The value for inProcessFolder [xxx] is invalid. It is not a folder.");
		fileSystemListener.configure();
		fileSystemListener.open();
	}
	
	@Test
	public void fileListenerTestInvalidProcessedFolder() throws Exception {
		fileSystemListener.setProcessedFolder("xxx");
		thrown.expectMessage("The value for processedFolder [xxx] is invalid. It is not a folder.");
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
			fileSystemListener.setInProcessFolder(inProcessFolder);
			_createFolder(inProcessFolder);
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
		fileListenerTestGetRawMessage(null,"inProcessFolder");
	}

	@Test
	public void fileListenerTestGetRawMessageDelayed() throws Exception {
		int stabilityTimeUnit=1000; // ms
		fileSystemListener.setMinStableTime(2*stabilityTimeUnit);
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.configure();
		fileSystemListener.open();
		
		long beforeCreateFile=System.currentTimeMillis();
		createFile(null, filename, contents);
		long afterCreateFile=System.currentTimeMillis();
		log.debug("beforeCreateFile ["+beforeCreateFile+"] afterCreateFile ["+afterCreateFile+"]");
		Thread.sleep(1*stabilityTimeUnit);
		long afterSleep=System.currentTimeMillis();
		log.debug("afterSleep ["+afterSleep+"]");
		
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNull("raw message must be null when not yet stable for "+(2*stabilityTimeUnit)+"ms",rawMessage);
		
		Thread.sleep(2*stabilityTimeUnit);
		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when stable for "+(3*stabilityTimeUnit)+"ms",rawMessage);
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
		
		String message=fileSystemListener.getStringFromRawMessage(rawMessage, threadContext);
		assertEquals(filename,message);
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
		
		String message=fileSystemListener.getStringFromRawMessage(rawMessage, threadContext);
		assertEquals(contents,message);
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

	
	@Test
	public void fileListenerTestAfterMessageProcessedDelete() throws Exception {
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
		fileSystemListener.afterMessageProcessed(null, rawMessage, null);
		waitForActionToFinish();
		// test
		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
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
		fileSystemListener.setProcessedFolder(processedFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		fileSystemListener.afterMessageProcessed(null, rawMessage, null);
		waitForActionToFinish();
		
		
		assertTrue("Destination folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}

}