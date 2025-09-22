package org.frankframework.filesystem;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.nio.file.DirectoryStream;
import java.util.Iterator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.stream.Message;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;

public abstract class FileSystemTest<F, FS extends IWritableFileSystem<F>> extends HelperedBasicFileSystemTest<F,FS> {

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		autowireBeanByNameInAdapter(fileSystem);
	}

	@Test
	void fileSystemTestAfterClosingAndOpening() throws Exception {
		// Arrange
		String filename = "create2" + FILE1;
		createFile(null, filename, "tja");
		waitForActionToFinish();

		fileSystem.configure();
		fileSystem.open();

		// Assert 1
		assertTrue(fileSystem.exists(fileSystem.toFile(filename)), "Expected file[" + filename + "] to be present");

		// Close & Open FS
		fileSystem.close();
		fileSystem.open();

		// Assert 2
		F f = fileSystem.toFile(filename);
		fileSystem.deleteFile(f);
		assertFalse(fileSystem.exists(f));
	}

	@Test
	public void writableFileSystemTestCreateNewFile() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";

		fileSystem.configure();
		fileSystem.open();

		deleteFile(null, filename);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		fileSystem.createFile(file, new ThrowingAfterCloseInputStream(new ByteArrayInputStream(contents.getBytes())));
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void writableFileSystemTestCreateOverwriteFile() throws Exception {
		String filename = "overwrited" + FILE1;

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "Eerste versie van de file");
		waitForActionToFinish();

		String contents = "Tweede versie van de file";
		F file = fileSystem.toFile(filename);
		fileSystem.createFile(file, new ThrowingAfterCloseInputStream(new ByteArrayInputStream(contents.getBytes())));
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}


	@Test
	public void writableFileSystemTestTruncateFile() throws Exception {
		String filename = "truncated" + FILE1;

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "Eerste versie van de file");
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		fileSystem.createFile(file, null);
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		assertEquals("", actual.trim());
	}

	@Test
	public void writableFileSystemTestAppendExistingFile() throws Exception {
		String filename = "append" + FILE1;
		String regel1 = "Eerste regel in de file";
		String regel2 = "Tweede regel in de file";
		String expected = regel1 + regel2;

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, regel1);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		fileSystem.appendFile(file, new ByteArrayInputStream(regel2.getBytes()));

		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		assertEquals(expected.trim(), actual.trim());
	}

	@Test
	public void writableFileSystemTestAppendNewFile() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";

		fileSystem.configure();
		fileSystem.open();

		deleteFile(null, filename);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		fileSystem.appendFile(file, new ByteArrayInputStream(contents.getBytes()));

		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		assertEquals(contents.trim(), actual.trim());
	}


	@Test
	public void writableFileSystemTestCreateFolder() throws Exception {
		String folderName = "dummyFolder";

		fileSystem.configure();
		fileSystem.open();

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
			waitForActionToFinish();
			assertFalse( _folderExists(folderName), "could not remove folder before test");
		}

		fileSystem.createFolder(folderName);
		waitForActionToFinish();

		assertTrue(_folderExists(folderName), "folder does not exist after creation");
	}

	@Test
	public void writableFileSystemTestRemoveFolder() throws Exception {
		String folderName = "dummyFolder";

		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName);

		fileSystem.removeFolder(folderName, false);
		waitForActionToFinish();

		assertFalse(_folderExists(folderName), "folder still exists after removal");
	}

	@Test
	public void writableFileSystemTestRemoveNonEmptyFolder() throws Exception {
		String folderName = "dummyTestFolder";

		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName);

		for(int i=0;i<3;i++) {
			createFile(folderName, "file_"+i+".txt", "some text here");
		}

		fileSystem.removeFolder(folderName, true);
		waitForActionToFinish();

		assertFalse(_folderExists(folderName), "folder still exists after removal");
	}

	@Test
	public void writableFileSystemTestRemoveFolderRecursive() throws Exception {
		String folderName = "dummyTestFolder";
		String innerFolder = folderName+"/innerfolder";
		String innerFolder2 = innerFolder + "/innerFolder2";
		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName); // Needed for SMB
		createFolderIfNotExists(innerFolder);  // Needed for SMB
		createFolderIfNotExists(innerFolder2);

		for (int i = 0; i < 3; i++) {
			createFile(folderName, "file_" + i + ".txt", "some text here");
			createFile(innerFolder, "file_" + i + ".txt", "some text here");
		}

		fileSystem.removeFolder(innerFolder2, true);
		waitForActionToFinish();
		assertTrue(_folderExists(innerFolder), "folder is removed while it should not be");

		fileSystem.removeFolder(folderName, true);
		waitForActionToFinish();

		assertFalse(_folderExists(folderName), "folder still exists after removal");
	}

	@Test
	public void writableFileSystemTestFolderExists() throws Exception {
		String folderName = "dummyFolder";

		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName);

		assertTrue(fileSystem.folderExists(folderName), "existing folder is not seen");
	}

	@Test
	public void writableFileSystemTestFolderExistsWithSlash() throws Exception {
		String folderName = "dummyFolder/";

		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName);

		assertTrue(fileSystem.folderExists(folderName), "existing folder is not seen");
	}

	@Test
	public void writableFileSystemTestFolderDoesNotExist() throws Exception {
		String folderName = "dummyFolder";

		fileSystem.configure();
		fileSystem.open();

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
			waitForActionToFinish();
			assertFalse(_folderExists(folderName), "could not remove folder before test");
		}

		assertFalse(fileSystem.folderExists(folderName), "non existing folder is seen");
	}

	@Test
	public void writableFileSystemTestFileIsNotAFolder() throws Exception {
		String folderName = "dummyFile";

		fileSystem.configure();
		fileSystem.open();

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
			waitForActionToFinish();
			assertFalse(_folderExists(folderName), "could not remove folder before test");
		}

		if (!_fileExists(folderName)) {
			createFile(null, folderName, "tja");
			waitForActionToFinish();
			assertTrue(_fileExists(folderName), "file must exist before test");
		}

		assertFalse(fileSystem.folderExists(folderName), "file must not be seen as folder");
	}


	@Test
	public void writableFileSystemTestRenameTo() throws Exception {
		String fileName = "fileTobeRenamed.txt";

		fileSystem.configure();
		fileSystem.open();

		createFile(null,fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(fileName));

		String destination = "fileRenamed.txt";
		deleteFile(null, destination);
		waitForActionToFinish();

		F f = fileSystem.toFile(fileName);
		F d = fileSystem.toFile(destination);
		fileSystem.renameFile(f, d);
		waitForActionToFinish();

		assertTrue(_fileExists(destination), "Destination must exist");
		assertFalse(_fileExists(fileName), "Origin must have disappeared");
	}

	@Test
	public void writableFileSystemTestRenameToOtherFolder() throws Exception {
		String sourceFolder = "srcFolder";
		String destinationFolder = "dstFolder";
		String fileName = "fileTobeRenamed.txt";
		String destination = "fileRenamed.txt";

		fileSystem.configure();
		fileSystem.open();

		_createFolder(sourceFolder);
		_createFolder(destinationFolder);
		createFile(sourceFolder,fileName, "");
		waitForActionToFinish();

		assertTrue(_fileExists(sourceFolder, fileName));

		deleteFile(destinationFolder, destination);
		assertFalse(_fileExists(destinationFolder, destination));
		waitForActionToFinish();

		F f = fileSystem.toFile(sourceFolder, fileName);
		F d = fileSystem.toFile(destinationFolder, destination);
		fileSystem.renameFile(f, d);
		waitForActionToFinish();

		assertTrue(_fileExists(destinationFolder, destination), "Destination must exist");
		assertFalse(_fileExists(sourceFolder, fileName), "Origin must have disappeared");
	}

	@Test
	public void writableFileSystemTestRemovingNonExistingDirectory() throws Exception {
		String foldername = "nonExistingFolder";
		fileSystem.configure();
		fileSystem.open();
		if(_folderExists(foldername)) {
			_deleteFolder(foldername);
		}

		FileSystemException e = assertThrows(FileSystemException.class, () -> fileSystem.removeFolder(foldername, false));
		assertThat(e.getMessage(), containsString("Directory does not exist."));
	}

	@Test
	public void writableFileSystemTestCreateExistingFolder() throws Exception {
		String folderName = "existingFolder";
		fileSystem.configure();
		fileSystem.open();
		_createFolder(folderName);
		waitForActionToFinish();

		FileSystemException e = assertThrows(FileSystemException.class, () -> fileSystem.createFolder(folderName));
		assertTrue(e.getMessage().endsWith("Directory already exists."));
	}

	@Test
	public void writableFileSystemTestFileSize() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, contents);
		waitForActionToFinish();
		F f = null;
		try(DirectoryStream<F> ds = fileSystem.list((String) null, TypeFilter.FILES_ONLY)) {
			Iterator<F> files = ds.iterator();
			if(files.hasNext()) {
				f = files.next();
			}
			else {
				fail("File not found");
			}
		}
		long size=fileSystem.getFileSize(f);

		if (size< contents.length()/2 || size> contents.length()*2) {
			fail("fileSize ["+size+"] out of range compared to ["+contents.length()+"]");
		}
	}

	@Test
	public void writableFileSystemTestGetCanonicalName() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, contents);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		String canonicalName=fileSystem.getCanonicalName(file);

		assertNotNull(canonicalName, "Canonical name should not be null");
	}

	@Test
	public void writableFileSystemTestDeleteDownloadedFile() throws Exception{
		String filename = "fileToBeDownloadedAndDeleted.txt";
		String content = "some content";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, content);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		assertTrue(_fileExists(filename), "Expected the file ["+filename+"] to be present");

		Message result = fileSystem.readFile(file, null);
		assertEquals(content, result.asString());

		fileSystem.deleteFile(file);
		waitForActionToFinish();
		assertFalse(_fileExists(filename), "Expected the file ["+filename+"] not to be present");
	}

	@Test
	public void writableFileSystemTestDeleteUploadedFile() throws Exception{
		String filename = "fileToBeUploadedAndDeleted.txt";
		String content = "some content";

		fileSystem.configure();
		fileSystem.open();

		F file = fileSystem.toFile(filename);
		fileSystem.createFile(file, new ThrowingAfterCloseInputStream(new ByteArrayInputStream(content.getBytes())));

		assertTrue(_fileExists(filename), "Expected the file ["+filename+"] to be present");

		fileSystem.deleteFile(file);
		waitForActionToFinish();
		assertFalse(_fileExists(filename), "Expected the file ["+filename+"] not to be present");

	}

	@Test
	public void writableFileSystemTestDeleteAppendedFile() throws Exception{
		String filename = "fileToBeAppendedAndDeleted.txt";
		String content = "some content";

		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, content);
		waitForActionToFinish();

		F file = fileSystem.toFile(filename);
		assertTrue(_fileExists(filename), "Expected the file ["+filename+"] to be present");

		fileSystem.appendFile(file, new ByteArrayInputStream(content.getBytes()));

		fileSystem.deleteFile(file);
		waitForActionToFinish();

		assertFalse(_fileExists(filename), "Expected the file ["+filename+"] not to be present");
	}

	@Test
	public void writableFileSystemTestReferToFileInFolder() throws Exception {
		String folder = "folder";
		String filename = "fileToBeReferred.txt";
		String content = "some content";

		fileSystem.configure();
		fileSystem.open();

		_createFolder(folder);
		createFile(folder, filename, content);

		F file1 = fileSystem.toFile(folder,filename);
		assertTrue(fileSystem.exists(file1));
		assertThat(fileSystem.getCanonicalName(file1),anyOf(endsWith(folder+"/"+filename),endsWith(folder+"\\"+filename)));
		assertThat(fileSystem.getName(file1),endsWith(filename));

		String absoluteName1 = folder+"/"+filename;
		String absoluteName2 = folder+"\\"+filename;
		F file2 = fileSystem.toFile(absoluteName1);
		assertTrue(fileSystem.exists(file2));
		assertThat(fileSystem.getCanonicalName(file2),anyOf(endsWith(absoluteName1),endsWith(absoluteName2)));
		assertThat(fileSystem.getName(file2),endsWith(filename));
	}

	@Test
	public void writeableFileSystemTestCreateLockfile() throws Exception {
		String filename = "lockFile.txt";

		fileSystem.configure();
		fileSystem.open();

		fileSystem.createFile(fileSystem.toFile(filename), null);
		assertTrue(fileSystem.exists(fileSystem.toFile(filename)));
	}

	@Test
	public void writableFileSystemTestCopyFileToNonExistentDirectoryCreateFolderFalse() throws Exception {
		String filename = "filetobecopied.txt";
		String folderName = "dummyFolder";
		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName);
		createFile(folderName, filename, "dummy");
		waitForActionToFinish();
		F file = fileSystem.toFile(folderName, filename);
		assertThrows(FileSystemException.class, () -> fileSystem.copyFile(file, "folder", false), "Expected that file could not be copied, because folder should not be created");
	}

	@Test
	public void writableFileSystemTestCopyFileToNonExistentDirectoryCreateFolderTrue() throws Exception {
		String filename = "filetobecopied.txt";
		String folderName = "dummyFolder";
		fileSystem.configure();
		fileSystem.open();

		createFolderIfNotExists(folderName);
		createFile(folderName, filename, "dummy");
		waitForActionToFinish();
		F f = fileSystem.copyFile(fileSystem.toFile(folderName, filename), "folder", true);

		assertNotNull(f, "Copied file cannot be null");
	}

	private void createFolderIfNotExists(final String folderName) throws Exception {
		if (!_folderExists(folderName)) {
			log.debug("creating folder [{}]", folderName);
			_createFolder(folderName);
			waitForActionToFinish();
			assertTrue(_folderExists(folderName), "could not create folder for test");
		}
	}
}
