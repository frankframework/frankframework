package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.hamcrest.core.StringEndsWith;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import lombok.AllArgsConstructor;

import org.frankframework.stream.Message;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.StreamUtil;

public abstract class BasicFileSystemTest<F, FS extends IBasicFileSystem<F>> extends FileSystemTestBase {

	protected FS fileSystem;
	/**
	 * Returns the file system
	 */
	protected abstract FS createFileSystem();

	@BeforeEach
	@Override
	public void setUp() throws Exception {
		fileSystem = createFileSystem();
		super.setUp();
	}

	@AfterEach
	@Override
	public void tearDown() {
		CloseUtils.closeSilently(fileSystem);
		super.tearDown();
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

		String id = createFile(null, filename, "tja");
		waitForActionToFinish();

		// test
		F f = fileSystem.toFile(id);
		assertTrue(fileSystem.exists(f), "Expected file[" + filename + "] to be present");
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
		assertFalse(fileSystem.exists(f), "Expected file[" + filename + "] not to be present");
	}

	@Override
	protected void existsCheck(String filename) throws Exception {
		assertTrue(_fileExists(filename), "Expected file [" + filename + "] to be present");
	}



	@Test
	public void basicFileSystemTestDelete() throws Exception {
		String filename = "tobeDeleted" + FILE1;

		fileSystem.configure();
		fileSystem.open();

		String id = createFile(null, filename, "maakt niet uit");
		waitForActionToFinish();
		existsCheck(id);

		// test
		F file = fileSystem.toFile(id);
		fileSystem.deleteFile(file);
		waitForActionToFinish();

		assertFalse(_fileExists(id), "Expected file [" + filename + "] not to be present");
	}

	public void testReadFile(F file, String expectedContents, String charset) throws IOException, FileSystemException {
		Message in = fileSystem.readFile(file, charset);
		String actual = in.asString();
		// test
		assertEquals(expectedContents.trim(), actual.trim());
	}

	@Test
	public void basicFileSystemTestRead() throws Exception {
		String filename = "read" + FILE1;
		String contents = "Tekst om te lezen";

		fileSystem.configure();
		fileSystem.open();

		String fullname = createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(fullname);

		F file = fileSystem.toFile(fullname);
		// test
		testReadFile(file, contents, null);
	}

	@Test
	public void basicFileSystemTestReadAndPreserve() throws Exception {
		String filename = "read" + FILE1;
		String contents = "Tekst om te lezen";

		fileSystem.configure();
		fileSystem.open();

		String id = createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(id);

		F file = fileSystem.toFile(id);
		// test
		Message in = fileSystem.readFile(file, null);

		// preserve() converts non repeatable messages to byte array
		in.preserve();

		// test if message can actually be read multiple times, without turning it explicitly into a String or byte array.
		// This will fail if a message declared that it was repeatable, but actually was not repeatable.
		String actual1 = StreamUtil.readerToString(in.asReader(), null);
		assertEquals(contents, actual1.trim());
		String actual2 = StreamUtil.readerToString(in.asReader(), null);
		assertEquals(contents, actual2.trim());
	}

	@Test
	public void basicFileSystemTestReadSpecialChars() throws Exception {
		String filename = "readSpecial" + FILE1;
		String contents = "€ $ & ^ % @ < é ë ó ú à è";

		fileSystem.configure();
		fileSystem.open();

		String id = createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(id);

		F file = fileSystem.toFile(id);
		// test
		testReadFile(file, contents, "UTF-8");
	}

	@Test
	public void basicFileSystemTestReadSpecialCharsFails() throws Exception {
		String filename = "readSpecialChars" + FILE1;
		String contents = "€ é";
		String expected = "â¬ Ã©";
		fileSystem.configure();
		fileSystem.open();

		String id = createFile(null, filename, contents);
		waitForActionToFinish();
		// test
		existsCheck(id);

		F file = fileSystem.toFile(id);
		// test
		testReadFile(file, expected, "ISO-8859-1");
	}

	@Test
	public void basicFileSystemTestGetFileName() throws Exception {
		String filename = "readName" + FILE1;
		String contents = "Tekst om te lezen";

		fileSystem.configure();
		fileSystem.open();

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		F file = fileSystem.toFile(id);
		// test
		assertEquals(id, fileSystem.getName(file));
	}

	@Test
	public void basicFileSystemTestGetFolderName() throws Exception {
		String foldername = "dummy/folder/";

		fileSystem.configure();
		fileSystem.open();

		_createFolder(foldername);
		waitForActionToFinish();

		F file = fileSystem.toFile(foldername);
		// test
		assertEquals("folder", fileSystem.getName(file));
	}

	@Test
	public void basicFileSystemTestModificationTime() throws Exception {
		String filename = "readModificationTime" + FILE1;
		String contents = "Tekst om te lezen";
		Date date = new Date();

		fileSystem.configure();
		fileSystem.open();

		String id = createFile(null, filename, contents);
		waitForActionToFinish();

		F file = fileSystem.toFile(id);
		Date actual1 = fileSystem.getModificationTime(file);
		Date actual2 = fileSystem.getModificationTime(file);
		assertEquals(actual1, actual2);

		fileSystem.deleteFile(file);
		waitForActionToFinish();

		// test
		long diff = actual2.getTime() - date.getTime();
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

		assertTrue(_folderExists(dstFolder), "Destination folder must exist");
		assertFileExistsWithContents(dstFolder, fileSystem.getName(movedFile), contents);
		//TODO: test that contents of file has remained the same
		//TODO: test that file timestamp has not changed
		assertFileDoesNotExist(srcFolder, filename);

		assertFalse(fileSystem.exists(f2), "original file should not exist anymore after move");

		try {
			F movedFile2 =fileSystem.moveFile(f2, dstFolder, false);
			assertNull(movedFile2, "File should not be moveable again");
		} catch (Exception e) {
			// an exception will do too, to signal that the file cannot be moved again.
			log.debug("exception caught after trying to move file: "+e.getMessage());
		}
	}

	@Test
	public void basicFileSystemTestMoveFileMustFailWhenTargetAlreadyExists() throws Exception {
		String filename = "fileTobeMoved.txt";
		String srcContents = "fakeSourceContents";
		String dstContents = "fakeDestinationContents";
		String srcFolder = "srcFolder";
		String dstFolder = "dstFolder";

		fileSystem.configure();
		fileSystem.open();

		_createFolder(srcFolder);
		createFile(srcFolder,filename, srcContents);
		_createFolder(dstFolder);
		createFile(dstFolder,filename, dstContents);
		waitForActionToFinish();

		assertFileExistsWithContents(srcFolder, filename, srcContents);
		assertFileExistsWithContents(dstFolder, filename, dstContents);

		F f = fileSystem.toFile(srcFolder, filename);

		assertThrows(FileSystemException.class, ()-> fileSystem.moveFile(f, dstFolder, false) );
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
		String id = createFile(srcFolder,filename, contents);
		waitForActionToFinish();

		assertFileExistsWithContents(srcFolder, id, contents);

		_createFolder(dstFolder);
		waitForActionToFinish();

		assertTrue(_folderExists(dstFolder));
		assertFileDoesNotExist(dstFolder, filename);

		F f = fileSystem.toFile(srcFolder, id);
		F copiedFile = fileSystem.copyFile(f, dstFolder, false);
		waitForActionToFinish();

		assertEquals(id, fileSystem.getName(copiedFile));

		assertTrue(_folderExists(dstFolder), "Destination folder must exist");
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

		String id = createFile(null, fileName, "");
		waitForActionToFinish();
		F f = fileSystem.toFile(id);

		assertTrue(fileSystem.exists(f));
	}

	public void basicFileSystemTestListFile(String folder, int numOfFilesInFolder) throws Exception {
		fileSystem.configure();
		fileSystem.open();

		long beforeFilesCreated=System.currentTimeMillis();

		String contents = "does not matter";
		List<String> fileIds = new ArrayList<>();
		for (int i=0; i<numOfFilesInFolder; i++) {
			String id = createFile(folder, "file_"+i+".txt", contents+i);
			fileIds.add(id);
		}
		waitForActionToFinish();
		long afterFilesCreated=System.currentTimeMillis();

		FolderContent folderContent = getFolderContents(folder, TypeFilter.FILES_ONLY);
		Set<String> filenames = new HashSet<>(folderContent.objectNames);
		assertEquals(numOfFilesInFolder, folderContent.objects.size(), "Size of set of files");
		assertEquals(numOfFilesInFolder, filenames.size(), "Size of set of filenames");

		if (folder == null) {
			for (String filename : filenames) {
				F f = fileSystem.toFile(filename);
				assertNotNull(f, "file must be found by filename [" + filename + "]");
				assertTrue(fileSystem.exists(f), "file must exist when referred to by filename [" + filename + "]");
			}
		}
		if (numOfFilesInFolder == 0) {
			return;
		}
		deleteFile(folder, fileIds.get(0));
		int numDeleted = 1;
		waitForActionToFinish();
		assertFalse(_fileExists(folder, fileIds.get(0)), "file should not exist anymore physically after deletion");

		folderContent = getFolderContents(folder, TypeFilter.FILES_ONLY);
		assertEquals(numOfFilesInFolder - numDeleted, folderContent.objects.size(), "Size of set of files after deletion");

		folderContent.objects.forEach(file -> {
			try {
				assertTrue(_fileExists(folder, fileSystem.getName(file)), "file found should exist");
				long modTime = fileSystem.getModificationTime(file).getTime();
				if (doTimingTests) {
					assertTrue(modTime >= beforeFilesCreated, "modtime [" + modTime + "] not after t0 [" + beforeFilesCreated + "]");
					assertTrue(modTime <= afterFilesCreated, "modtime [" + modTime + "] not before t1 [" + afterFilesCreated + "]");
				}
			} catch (Exception e) {
				fail("exception caught: " + e.getMessage());
			}
		});

		deleteFile(folder, fileIds.get(1));
		numDeleted++;
		folderContent = getFolderContents(folder, TypeFilter.FILES_ONLY);
		assertEquals(numOfFilesInFolder - numDeleted, folderContent.objects.size(), "Size of set of files after deletion");
	}

	protected FolderContent getFolderContents(String folder, TypeFilter filter) throws Exception {
		try (DirectoryStream<F> ds = fileSystem.list(folder, filter)) {
			Iterator<F> it = ds.iterator();
			List<String> fileNames = new ArrayList<>();
			List<F> files = new ArrayList<>();
			while (it.hasNext()) {
				F next = it.next();
				String filename = fileSystem.getName(next);
				fileNames.add(filename);
				files.add(next);
				log.debug("found file [{}]", filename);
			}
			return new FolderContent(fileNames, files);
		}
	}

	@AllArgsConstructor
	protected class FolderContent {
		public List<String> objectNames; // can be files or folders
		public List<F> objects;
	}

	@Test
	public void basicFileSystemTestListFileFromRoot() throws Exception {
		_deleteFolder(null); //Clean root folder
		basicFileSystemTestListFile(null, 2);
	}
	@Test
	public void basicFileSystemTestListFileFromFolder() throws Exception {
		_deleteFolder(null); //Clean root folder
		_createFolder("folder");
		basicFileSystemTestListFile("folder", 2);
	}

	@Test
	public void basicFileSystemTestListFileFromEmptyFolder() throws Exception {
		_deleteFolder(null); //Clean root folder
		_createFolder("folder2");
		basicFileSystemTestListFile("folder2", 0);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFromOtherFoldersWhenReadingFromRoot() throws Exception {
		_deleteFolder(null); //Clean root folder
		_createFolder("folder");
		_createFolder("Otherfolder");
		createFile("Otherfolder", "otherfile", "maakt niet uit");
		basicFileSystemTestListFile(null, 2);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFromOtherFoldersWhenReadingFromFolder() throws Exception {
		_deleteFolder(null); //Clean root folder
		_createFolder("folder");
		_createFolder("Otherfolder");
		createFile("Otherfolder", "otherfile", "maakt niet uit");
		basicFileSystemTestListFile("folder", 2);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFromRootWhenReadingFromFolder() throws Exception {
		_deleteFolder(null); //Clean root folder
		_createFolder("folder");
		createFile(null, "otherfile", "maakt niet uit");
		basicFileSystemTestListFile("folder", 2);
	}

	@Test
	public void basicFileSystemTestListFileShouldNotReadFolders() throws Exception {
		_deleteFolder(null);
		String contents1 = "maakt niet uit";
		String contents2 = "maakt ook niet uit";
		String folderName = "subfolder";

		fileSystem.configure();
		fileSystem.open();


		createFile(null, FILE1, contents1);
		createFile(null, FILE2, contents2);
		_createFolder(folderName);

		Set<F> files = new HashSet<>();
		Set<String> filenames = new HashSet<>();

		try(DirectoryStream<F> ds = fileSystem.list(null, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			// Count files
			while (it.hasNext()) {
				F f=it.next();
				files.add(f);
				filenames.add(fileSystem.getName(f));
			}
		}

		assertEquals(2, files.size(), "Size of set of files, should not contain folders");
		assertEquals( 2, filenames.size(), "Size of set of filenames, should not contain folders");

	}

	@Test
	public void basicFileSytemTestGetNumberOfFilesInFolder() throws Exception {
		// arrange
		String contents1 = "maakt niet uit";
		String contents2 = "maakt ook niet uit";
		String folderName = "folder_for_counting";

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
		}
		_createFolder(folderName);

		fileSystem.configure();
		fileSystem.open();

		// act
		int fileCount = fileSystem.getNumberOfFilesInFolder(folderName);

		// assert
		assertEquals(0, fileCount);

		// arrange 2
		createFile(folderName, FILE1, contents1);
		createFile(folderName, FILE2, contents2);

		// act 2
		fileCount = fileSystem.getNumberOfFilesInFolder(folderName);

		// assert 2
		assertEquals(2, fileCount);
	}

	@Test
	// getParentFolder() is used when attribute deleteEmptyFolder=true, and in action RENAME
	public void basicFileSystemTestGetParentOfTheDeletedFile() throws Exception {
		String folderName = "parentFolder";

		fileSystem.configure();
		fileSystem.open();

		_createFolder(folderName);
		String id = createFile(folderName, FILE1, "text");

		F f = fileSystem.toFile(folderName, id);

		fileSystem.deleteFile(f);

		String parentFolder = fileSystem.getParentFolder(f);
		parentFolder = FilenameUtils.normalizeNoEndSeparator(parentFolder);

		assertThat(parentFolder, StringEndsWith.endsWith(folderName));
	}

	@Test
	public void basicFileSystemTestListDirsAndOrFolders() throws Exception {
		fileSystem.configure();
		fileSystem.open();

		// Arrange files and folder structure
		_deleteFolder(null); //Clean root folder
		_createFolder("folder");
		_createFolder("Otherfolder");
		_createFolder("Otherfolder/Folder2");
		createFile("Otherfolder", "otherfile", "maakt niet uit");

		// Assert files only
		List<String> otherFolder = getFolderContents("Otherfolder", TypeFilter.FILES_ONLY).objectNames;
		assertEquals(1, otherFolder.size());
		assertEquals("otherfile", otherFolder.get(0));

		// Assert directories only
		otherFolder = getFolderContents("Otherfolder", TypeFilter.FOLDERS_ONLY).objectNames;
		assertEquals(1, otherFolder.size());
		assertTrue(otherFolder.contains("Folder2") || otherFolder.contains("Folder2/")); // Remove trailing slash on folder names for SambaFS1.

		// Assert files and directories (order does not matter, so using contains instead of get(0))
		otherFolder = getFolderContents("Otherfolder", TypeFilter.FILES_AND_FOLDERS).objectNames;
		assertEquals(2, otherFolder.size());
		assertTrue(otherFolder.contains("Folder2") || otherFolder.contains("Folder2/"));
		assertTrue(otherFolder.contains("otherfile"));
	}
}
