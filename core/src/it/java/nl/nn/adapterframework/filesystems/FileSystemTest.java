package nl.nn.adapterframework.filesystems;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Date;
import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import liquibase.util.StreamUtil;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.filesystem.FileSystemException;
import nl.nn.adapterframework.filesystem.IFileSystem;

public abstract class FileSystemTest<F, FS extends IFileSystem<F>> {

	@Rule
	public ExpectedException exception = ExpectedException.none();
	
	public String FILE1 = "file1.txt";
	public String FILE2 = "file2.txt";
	public String DIR1 = "testDirectory/";
	public String DIR2 = "testDirectory2/";

	protected FS fileSystem;
	private long waitMillis = 0;
	/**
	 * Returns the file system 
	 * @return fileSystem
	 * @throws ConfigurationException
	 */
	protected abstract FS getFileSystem() throws ConfigurationException;

	/**
	 * Checks if a file with the specified name exists.
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean _fileExists(String filename) throws Exception;
	
	/**
	 * Checks if a folder with the specified name exists.
	 * @param folderName
	 * @return
	 * @throws Exception
	 */
	protected abstract boolean _folderExists(String folderName) throws Exception;
	
	/**
	 * Deletes the file with the specified name
	 * @param filename
	 * @throws Exception
	 */
	protected abstract void _deleteFile(String filename) throws Exception;
	
	/**
	 * Creates a file with the specified name and returns output stream 
	 * to be able to write that file.
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	protected abstract OutputStream _createFile(String filename) throws Exception;

	/**
	 * Returns an input stream of the file 
	 * @param filename
	 * @return
	 * @throws Exception
	 */
	protected abstract InputStream _readFile(String filename) throws Exception;
	
	/**
	 * Creates a folder 
	 * @param filename
	 * @throws Exception
	 */
	protected abstract void _createFolder(String filename) throws Exception;

	/**
	 * Deletes the folder 
	 * @param filename
	 * @throws Exception
	 */
	protected abstract void _deleteFolder(String folderName) throws Exception;

	@Before
	public void setUp() throws IOException, ConfigurationException, FileSystemException {
		fileSystem = getFileSystem();
		fileSystem.configure();
		fileSystem.open();
	}
	
	@After 
	public void tearDown() throws Exception {
		fileSystem.close();
	}

	public void deleteFile(String filename) throws Exception {
		if (_fileExists(filename)) {
			_deleteFile(filename);
		}
	}

	public void createFile(String filename, String contents) throws Exception {
		OutputStream out = _createFile(filename);
		if (contents != null)
			out.write(contents.getBytes());
		out.close();
	}

	public String readFile(String filename) throws Exception {
		InputStream in = _readFile(filename);
		String content = StreamUtil.getReaderContents(new InputStreamReader(in));
		in.close();
		return content;
	}

	/** 
	 * Pause current thread. Since creating an object takes a bit time 
	 * this method can be used to make sure object is created in the server.
	 * Added for Amazon S3 sender. 
	 * @throws FileSystemException 
	 */
	public void waitForActionToFinish() throws FileSystemException {
		try {
			Thread.sleep(waitMillis);
		} catch (InterruptedException e) {
			throw new FileSystemException("Cannot pause the thread. Be aware that there may be timing issue to check files on the server.", e);
		}
	}

	@Test
	public void fileSystemTestExists() throws Exception {
		String filename = "testExists" + FILE1;
		
		createFile(filename, "tja");
		waitForActionToFinish();
		// test
		assertTrue("Expected file[" + filename + "] to be present", _fileExists(filename));
	}

	@Test
	public void fileSystemTestNotExists() throws Exception {
		String filename = "testNotExists" + FILE1;
		
		deleteFile(filename);
		waitForActionToFinish();
		// test
		assertFalse("Expected file[" + filename + "] not to be present", _fileExists(filename));
	}

	@Test
	public void fileSystemTestCreateNewFile() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";
		
		deleteFile(filename);
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);
		out.write(contents.getBytes());
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);
		
		String actual = readFile(filename);
		// test
		equalsCheck(contents.trim(), actual.trim());

	}

	private void equalsCheck(String content, String actual) {
		assertEquals(content, actual);
	}

	@Test
	public void fileSystemTestCreateOverwriteFile() throws Exception {
		String filename = "overwrited" + FILE1;
		
		createFile(filename, "Eerste versie van de file");
		waitForActionToFinish();
		
		String contents = "Tweede versie van de file";
		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);
		out.write(contents.getBytes());
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(filename);
		// test
		equalsCheck(contents.trim(), actual.trim());
	}

	private void existsCheck(String filename) throws Exception {
		assertTrue("Expected file [" + filename + "] to be present", _fileExists(filename));
	}

	@Test
	public void fileSystemTestTruncateFile() throws Exception {
		String filename = "truncated" + FILE1;
		
		createFile(filename, "Eerste versie van de file");
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);
		
		String actual = readFile(filename);
		// test
		equalsCheck("", actual.trim());
	}

	@Test
	public void fileSystemTestAppendFile() throws Exception {
		String filename = "append" + FILE1;
		String regel1 = "Eerste regel in de file";
		String regel2 = "Tweede regel in de file";
		String expected = regel1 + regel2;
		
		createFile(filename, regel1);
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.appendFile(file);
		out.write(regel2.getBytes());
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(filename);
		// test
		equalsCheck(expected.trim(), actual.trim());
	}

	@Test
	public void fileSystemTestDelete() throws Exception {
		String filename = "tobeDeleted" + FILE1;
		
		createFile(filename, "maakt niet uit");
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F file = fileSystem.toFile(filename);
		fileSystem.deleteFile(file);
		waitForActionToFinish();
		// test
		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
	}

	public void testReadFile(F file, String expectedContents) throws IOException, FileSystemException {
		InputStream in = fileSystem.readFile(file);
		String actual = StreamUtil.getReaderContents(new InputStreamReader(in));
		// test
		equalsCheck(expectedContents.trim(), actual.trim());
	}

	@Test
	public void fileSystemTestRead() throws Exception {
		String filename = "read" + FILE1;
		String contents = "Tekst om te lezen";

		createFile(filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F file = fileSystem.toFile(filename);
		// test
		testReadFile(file, contents);
	}

	@Test
	public void fileSystemTestGetName() throws Exception {
		String filename = "readName" + FILE1;
		String contents = "Tekst om te lezen";
		
		createFile(filename, contents);
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		// test
		assertEquals(filename, fileSystem.getName(file));
	}

	@Test
	public void fileSystemTestModificationTime() throws Exception {
		String filename = "readModificationTime" + FILE1;
		String contents = "Tekst om te lezen";
		Date date = new Date();

		createFile(filename, contents);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		Date actual = fileSystem.getModificationTime(file, false);
		long diff = actual.getTime() - date.getTime();

		fileSystem.deleteFile(file);
		waitForActionToFinish();
		// test
		assertFalse(diff > 10000);
	}
	
	@Test
	public void fileSystemTestRenameTo() throws Exception {
		String fileName = "fileTobeRenamed.txt";
		
		createFile(fileName,"");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		String destination = "fileRenamed.txt";
		deleteFile(destination);
		waitForActionToFinish();
		
		F f = fileSystem.toFile(fileName);
		fileSystem.renameTo(f, destination);
		waitForActionToFinish();
		
		assertTrue(_fileExists(destination));
		assertFalse(_fileExists(fileName));
	}
	
	@Test
	public void fileSystemTestRenameToExisting() throws Exception {
		exception.expectMessage("Cannot rename file. Destination file already exists.");
		String fileName = "fileToBeRenamedExisting.txt";
		
		createFile(fileName, "");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		String destination = "fileRenamedExists.txt";
		createFile(destination, "");
		waitForActionToFinish();
		
		F f = fileSystem.toFile(fileName);
		fileSystem.renameTo(f, destination);
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
	}
	
	@Test
	public void fileSystemTestRemovingNonExistingDirectory() throws Exception {
		exception.expectMessage("Directory does not exist.");
		String filename = "nonExistingFolder";
		if(_folderExists(filename)) {
			_deleteFolder(filename);
		}
		F f = fileSystem.toFile(filename);
		fileSystem.removeFolder(f);
	}
	
	@Test
	public void fileSystemTestCreateExistingFolder() throws Exception {
		exception.expectMessage("Directory already exists.");
		String folderName = "existingFolder/";
		
		_createFolder(folderName);
		waitForActionToFinish();
		F f = fileSystem.toFile(folderName);
		fileSystem.createFolder(f);
	}
	
	@Test
	public void fileSystemTestExistsMethod() throws Exception {
		String fileName = "fileExists.txt";

		createFile(fileName, "");
		waitForActionToFinish();
		F f = fileSystem.toFile(fileName);

		assertTrue(fileSystem.exists(f));
	}

	@Test
	public void fileSystemTestListFile() throws Exception {
		String contents1 = "maakt niet uit";
		String contents2 = "maakt ook niet uit";
		
		createFile(FILE1, contents1);
		createFile(FILE2, contents2);
		waitForActionToFinish();
		
		Iterator<F> it = fileSystem.listFiles();
		int count = 0;
		// Count files
		while (it.hasNext()) {
			it.next();
			count++;
		}

		it = fileSystem.listFiles();
		for (int i = 0; i < count; i++) {
			assertTrue(it.hasNext());
			it.next();
		}
		// test
		assertFalse(it.hasNext());

		deleteFile(FILE1);
		
		fileSystem.close();
		waitForActionToFinish();
		fileSystem.open();
		
		it = fileSystem.listFiles();
		for (int i = 0; i < count - 1; i++) {
			assertTrue(it.hasNext());
			it.next();
		}
		// test
		assertFalse(it.hasNext());

		deleteFile(FILE2);
		fileSystem.close();
		waitForActionToFinish();
		fileSystem.open();

		it = fileSystem.listFiles();
		for (int i = 0; i < count - 2; i++) {
			assertTrue(it.hasNext());
			it.next();
		}
		// test
		assertFalse(it.hasNext());
	}

	public void setWaitMillis(long waitMillis) {
		this.waitMillis = waitMillis;
	}

}