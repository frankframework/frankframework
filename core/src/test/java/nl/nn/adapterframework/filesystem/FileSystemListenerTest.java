package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class FileSystemListenerTest<F, FS extends IBasicFileSystem<F>> extends BasicFileSystemTest<F, FS> {

	private FileSystemListener<F, FS> fileSystemListener;
	private Map<String,Object> threadContext;

	public FileSystemListener<F, FS> createFileSystemListener() {
		FileSystemListener<F, FS> fileSystemListener = new FileSystemListener<F, FS>();
		fileSystemListener.setFileSystem(fileSystem);
		return fileSystemListener;
	}

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
		super.tearDown();
		if (fileSystemListener!=null) {
			fileSystemListener.close();
		};
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
		fileSystemListener.setMinStableTime(100);
		String filename="rawMessageFile";
		String contents="Test Message Contents";
		
		fileSystemListener.configure();
		fileSystemListener.open();
		
		long beforeCreateFile=System.currentTimeMillis();
		createFile(null, filename, contents);
		long afterCreateFile=System.currentTimeMillis();
		log.debug("beforeCreateFile ["+beforeCreateFile+"] afterCreateFile ["+afterCreateFile+"]");
		Thread.sleep(50);
		long afterSleep=System.currentTimeMillis();
		log.debug("afterSleep ["+afterSleep+"]");
		
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		if (rawMessage!=null) {
			long fileModTime=fileSystem.getModificationTime(rawMessage).getTime();
			log.debug("fileModTime ["+fileModTime+"]");
		}
		assertNull("raw message must be null when not yet stable for 100ms",rawMessage);
		
		Thread.sleep(100);
		rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull("raw message must be not null when stable for 150ms",rawMessage);
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageFilenam() throws Exception {
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
		fileSystemListener.setMessageType("contents");
		fileSystemListener.configure();
		fileSystemListener.open();
		
		createFile(null, filename, contents);
	
		F rawMessage=fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);
		
		String id=fileSystemListener.getIdFromRawMessage(rawMessage, threadContext);
		assertEquals(filename,id);
	}
	
	@Test
	public void fileListenerTestAfterMessageProcessedDelete() throws Exception {
		String filename = "AfterMessageProcessedDelete" + FILE1;
		
		fileSystemListener.setDelete(true);
		fileSystemListener.configure();
		fileSystemListener.open();

		createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F file = fileSystem.toFile(filename);
		fileSystemListener.afterMessageProcessed(null, file, null);
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

		fileSystemListener.setProcessedFolder(processedFolder);
		fileSystemListener.configure();
		fileSystemListener.open();


		assertTrue(_fileExists(fileName));
		assertTrue(_folderExists(processedFolder));

		F file= fileSystem.toFile(fileName);
		fileSystemListener.afterMessageProcessed(null, file, null);
		waitForActionToFinish();
		
		
		assertTrue("Destination folder must exist",_folderExists(processedFolder));
		assertTrue("Destination must exist",_fileExists(processedFolder, fileName));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}

}