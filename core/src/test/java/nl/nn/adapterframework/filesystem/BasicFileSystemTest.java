package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.stream.Message;

public abstract class BasicFileSystemTest<F, FS extends IBasicFileSystem<F>> extends FileSystemTestBase {

	protected FS fileSystem;
	/**
	 * Returns the file system 
	 */
	protected abstract FS createFileSystem();

	@Before
	public void setUp() throws Exception {
		fileSystem = createFileSystem();
	}
	
	@After 
	public void tearDown() throws Exception {
		if (fileSystem!=null) fileSystem.close();
	}

	@Test
	public void basicFileSystemTestConfigure() throws Exception {
		fileSystem.configure();
	}

	@Test
	public void basicFileSystemTestOpen() throws Exception {
		fileSystem.configure();
		fileSystem.open();
	}
	
	@Test
	public void basicFileSystemTestExists() throws Exception {
		String filename = "testExists" + FILE1;
		
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "tja");
		waitForActionToFinish();
		
		// test
		F f = fileSystem.toFile(filename);
		assertTrue("Expected file[" + filename + "] to be present", fileSystem.exists(f));
	}

	@Test
	public void basicFileSystemTestNotExists() throws Exception {
		String filename = "testNotExists" + FILE1;
		
		fileSystem.configure();
		fileSystem.open();

		deleteFile(null, filename);
		waitForActionToFinish();

		// test
		F f = fileSystem.toFile(filename);
		assertFalse("Expected file[" + filename + "] not to be present", fileSystem.exists(f));
	}


	@Override
	protected void equalsCheck(String content, String actual) {
		assertEquals(content, actual);
	}


	@Override
	protected void existsCheck(String filename) throws Exception {
		assertTrue("Expected file [" + filename + "] to be present", _fileExists(filename));
	}



	@Test
	public void basicFileSystemTestDelete() throws Exception {
		String filename = "tobeDeleted" + FILE1;
		
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		existsCheck(filename);

		// test
		F file = fileSystem.toFile(filename);
		fileSystem.deleteFile(file);
		waitForActionToFinish();

		assertFalse("Expected file [" + filename + "] not to be present", _fileExists(filename));
	}

	public void testReadFile(F file, String expectedContents) throws IOException, FileSystemException {
		Message in = fileSystem.readFile(file);
		String actual = in.asString();
		// test
		equalsCheck(expectedContents.trim(), actual.trim());
	}

	@Test
	public void basicFileSystemTestRead() throws Exception {
		String filename = "read" + FILE1;
		String contents = "Tekst om te lezen";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(filename);

		F file = fileSystem.toFile(filename);
		// test
		testReadFile(file, contents);
	}

	@Test
	public void basicFileSystemTestGetName() throws Exception {
		String filename = "readName" + FILE1;
		String contents = "Tekst om te lezen";
		
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, contents);
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		// test
		assertEquals(filename, fileSystem.getName(file));
	}

	@Test
	public void basicFileSystemTestModificationTime() throws Exception {
		String filename = "readModificationTime" + FILE1;
		String contents = "Tekst om te lezen";
		Date date = new Date();

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, contents);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		Date actual = fileSystem.getModificationTime(file);
		long diff = actual.getTime() - date.getTime();

		fileSystem.deleteFile(file);
		waitForActionToFinish();
		// test
		assertFalse(diff > 10000);
	}
	
	
	@Test
	public void basicFileSystemTestMoveFile() throws Exception {
		String filename = "fileTobeMoved.txt";
		String contents = "tja";
		String srcFolder = "srcFolder";
		String dstFolder = "dstFolder";
		
		fileSystem.configure();
		fileSystem.open();

		_createFolder(srcFolder);
		createFile(srcFolder,filename, contents);
		waitForActionToFinish();
		
		assertFileExistsWithContents(srcFolder, filename, contents);
		
		_createFolder(dstFolder);
		waitForActionToFinish();

		assertTrue(_folderExists(dstFolder));
		assertFileDoesNotExist(dstFolder, filename);

		F f = fileSystem.toFile(srcFolder, filename);
		F f2 = fileSystem.toFile(srcFolder, filename);
		F movedFile =fileSystem.moveFile(f, dstFolder, false);
		waitForActionToFinish();

		assertEquals(filename,fileSystem.getName(movedFile));
		
		assertTrue("Destination folder must exist",_folderExists(dstFolder));
		assertFileExistsWithContents(dstFolder, fileSystem.getName(movedFile), contents);
		//TODO: test that contents of file has remained the same
		//TODO: test that file timestamp has not changed
		assertFileDoesNotExist(srcFolder, filename);

		assertFalse("original file should not exist anymore after move", fileSystem.exists(f2));

		try {
			F movedFile2 =fileSystem.moveFile(f2, dstFolder, false);
			assertNull("File should not be moveable again", movedFile2);
		} catch (Exception e) {
			// an exception will do too, to signal that the file cannot be moved again.
			log.debug("exception caught after trying to move file: "+e.getMessage());
		}
	}

	@Test
	public void basicFileSystemTestCopyFile() throws Exception {
		String filename = "fileTobeCopied.txt";
		String contents = "tja";
		String srcFolder = "srcFolder";
		String dstFolder = "dstFolder";
		
		fileSystem.configure();
		fileSystem.open();

		_createFolder(srcFolder);
		createFile(srcFolder,filename, contents);
		waitForActionToFinish();
		
		assertFileExistsWithContents(srcFolder, filename, contents);
		
		_createFolder(dstFolder);
		waitForActionToFinish();

		assertTrue(_folderExists(dstFolder));
		assertFileDoesNotExist(dstFolder, filename);

		F f = fileSystem.toFile(srcFolder, filename);
		F copiedFile =fileSystem.copyFile(f, dstFolder, false);
		waitForActionToFinish();
		
		assertEquals(filename,fileSystem.getName(copiedFile));
		
		assertTrue("Destination folder must exist",_folderExists(dstFolder));
		assertFileExistsWithContents(dstFolder, fileSystem.getName(copiedFile), contents);
		//TODO: test that contents of file has remained the same
		//TODO: test that file timestamp has not changed
		assertFileExistsWithContents(srcFolder, filename, contents);
	}



	
	@Test
	public void basicFileSystemTestExistsMethod() throws Exception {
		String fileName = "fileExists.txt";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, fileName, "");
		waitForActionToFinish();
		F f = fileSystem.toFile(fileName);

		assertTrue(fileSystem.exists(f));
	}

	public void basicFileSystemTestListFile(String folder, int numOfFilesInFolder) throws Exception {
		String contents = "maakt niet uit ";
		
		fileSystem.configure();
		fileSystem.open();

		long beforeFilesCreated=System.currentTimeMillis();
		
		for (int i=0; i<numOfFilesInFolder; i++) {
			createFile(folder, "file_"+i+".txt", contents+i);
		}
		waitForActionToFinish();

		long afterFilesCreated=System.currentTimeMillis();
		
		Set<F> files = new HashSet<F>();
		Set<String> filenames = new HashSet<String>();
		int count = 0;
		try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
			Iterator<F> it = ds.iterator();
			// Count files
			while (it.hasNext()) {
				F f=it.next();
				files.add(f);
				String name=fileSystem.getName(f);
				log.debug("found file ["+name+"]");
				filenames.add(name);
				count++;
			}
		}

		assertEquals("Size of set of files", numOfFilesInFolder, files.size());
		assertEquals("Size of set of filenames", numOfFilesInFolder, filenames.size());
		
		if (folder==null) {
			for (String filename:filenames) {
				F f=fileSystem.toFile(filename);
				assertNotNull("file must be found by filename ["+filename+"]",f);
				assertTrue("file must exist when referred to by filename ["+filename+"]",fileSystem.exists(f));
			}
		}
		try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
			Iterator<F> it = ds.iterator();
			for (int i = 0; i < count; i++) {
				assertTrue(it.hasNext());
				it.next();
			}
			// test
			assertFalse(it.hasNext());
		}

		if (numOfFilesInFolder>0) {
			deleteFile(folder, "file_0.txt");
			int numDeleted = 1;
	
			waitForActionToFinish();

			assertFalse("file should not exist anymore physically after deletion", _fileExists(folder, "file_0.txt"));
	
			try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
				Iterator<F> it = ds.iterator();
				for (int i = 0; i < count - numDeleted; i++) {
					assertTrue(it.hasNext());
					F f=it.next();
					log.debug("found file ["+fileSystem.getName(f)+"]");
					assertTrue("file found should exist", _fileExists(folder, fileSystem.getName(f)));
					long modTime=fileSystem.getModificationTime(f).getTime();
					if (doTimingTests) assertTrue("modtime ["+modTime+"] not after t0 ["+beforeFilesCreated+"]", modTime>=beforeFilesCreated);
					if (doTimingTests) assertTrue("modtime ["+modTime+"] not before t1 ["+afterFilesCreated+"]", modTime<=afterFilesCreated);
				}
				// test
				assertFalse("after a delete the number of files should be one less",it.hasNext());
			}

			if (numOfFilesInFolder>1) {
				deleteFile(folder, "file_1.txt");
				numDeleted++;
		
				try(DirectoryStream<F> ds = fileSystem.listFiles(folder)) {
					Iterator<F> it = ds.iterator();
					for (int i = 0; i < count - numDeleted; i++) {
						assertTrue(it.hasNext());
						it.next();
					}
					// test
					assertFalse(it.hasNext());
				}
			}
		}
	}
	
	@Test
	public void basicFileSystemTestListFileFromRoot() throws Exception {
		basicFileSystemTestListFile(null, 2);
	}
	@Test
	public void basicFileSystemTestListFileFromFolder() throws Exception {
		_createFolder("folder");
		basicFileSystemTestListFile("folder", 2);
	}

	@Test
	public void basicFileSystemTestListFileFromEmptyFolder() throws Exception {
		_createFolder("folder2");
		basicFileSystemTestListFile("folder2", 0);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFromOtherFoldersWhenReadingFromRoot() throws Exception {
		_createFolder("folder");
		_createFolder("Otherfolder");
		createFile("Otherfolder", "otherfile", "maakt niet uit");
		basicFileSystemTestListFile(null, 2);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFromOtherFoldersWhenReadingFromFolder() throws Exception {
		_createFolder("folder");
		_createFolder("Otherfolder");
		createFile("Otherfolder", "otherfile", "maakt niet uit");
		basicFileSystemTestListFile("folder", 2);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFromRootWhenReadingFromFolder() throws Exception {
		_createFolder("folder");
		createFile(null, "otherfile", "maakt niet uit");
		basicFileSystemTestListFile("folder", 2);
	}
	@Test
	public void basicFileSystemTestListFileShouldNotReadFolders() throws Exception {
		String contents1 = "maakt niet uit";
		String contents2 = "maakt ook niet uit";
		String folderName = "subfolder";
		
		fileSystem.configure();
		fileSystem.open();

		
		createFile(null, FILE1, contents1);
		createFile(null, FILE2, contents2);
		_createFolder(folderName);

		Set<F> files = new HashSet<F>();
		Set<String> filenames = new HashSet<String>();
		try(DirectoryStream<F> ds = fileSystem.listFiles(null)) {
			Iterator<F> it = ds.iterator();
			// Count files
			while (it.hasNext()) {
				F f=it.next();
				files.add(f);
				filenames.add(fileSystem.getName(f));
			}
		}

		assertEquals("Size of set of files, should not contain folders", 2, files.size());
		assertEquals("Size of set of filenames, should not contain folders", 2, filenames.size());
		
	}

}