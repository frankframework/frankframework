package org.frankframework.filesystem;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.util.Iterator;

import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class SelfContainedBasicFileSystemTest<F, FS extends IBasicFileSystem<F>> extends BasicFileSystemTestBase<F, FS>{

//	private String testFolderPrefix = "fs_test_"+DateUtils.format(new Date(),"yyyy-MM-dd_HHmmss.SSS");
	private final String testFolderPrefix = "fs_test";
	private final String sourceOfMessages_folder = null;

	public void testFolders() throws FileSystemException {
		String folderName = testFolderPrefix+"_testFolders";

		try {
			fileSystem.createFolder(folderName);
			assertTrue(fileSystem.folderExists(folderName));
		} finally {
			fileSystem.removeFolder(folderName, false);
			assertFalse(fileSystem.folderExists(folderName));
		}
	}

	public void displayFile(F f) throws FileSystemException {
		log.debug("file subject ["+fileSystem.getAdditionalFileProperties(f).get("subject")+"] name ["+fileSystem.getName(f)+"]");
		//log.debug("file canonical name ["+fileSystem.getCanonicalName(f)+"]");
//		log.debug("file size ["+fileSystem.getFileSize(f)+"]");
//		log.debug("file subject ["+fileSystem.getAdditionalFileProperties(f).get("subject")+"]");
//		log.debug("file props ["+fileSystem.getAdditionalFileProperties(f)+"]");
	}

//	public void listAllFilesInFolder(String folderName) throws FileSystemException {
//		Iterator<F> it = fileSystem.list(folderName);
//		while (it!=null && it.hasNext()) {
//			displayFile(it.next());
//		}
//	}

	public void testFiles() throws FileSystemException, IOException {
		String folderName = testFolderPrefix+"_testFiles";
		String folderName2 = folderName+"-2";

		if (fileSystem.folderExists(folderName)) {
			fileSystem.removeFolder(folderName, false);
			assertFalse(fileSystem.folderExists(folderName));
		}
		fileSystem.createFolder(folderName);
		assertTrue(fileSystem.folderExists(folderName));
		try(DirectoryStream<F> ds = fileSystem.list(folderName, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			if (it!=null && it.hasNext()) {
				displayFile(it.next());
				fail("just created folder ["+folderName+"] should be emtpty");
			}
			assertFalse(it!=null && it.hasNext(), "just created folder ["+folderName+"] should be emtpty");
		}
		F sourceFile = null;
		F destFile1 = null;
		try(DirectoryStream<F> ds = fileSystem.list(sourceOfMessages_folder, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			assertTrue(it!=null && it.hasNext(), "there must be at least one messsage in the sourceOfMessages_folder ["+sourceOfMessages_folder+"]");

			sourceFile =  it.next();
			assertTrue(fileSystem.exists(sourceFile), "file retrieved from folder should exist");
			//displayFile(sourceFile);

			destFile1 = fileSystem.copyFile(sourceFile, folderName, false);
			assertTrue(fileSystem.exists(sourceFile), "source file should still exist after copy");

			//displayFile(destFile1);
			assertNotNull(destFile1, "destination file should be not null after copy");
			assertTrue(fileSystem.exists(destFile1), "destination file should exist after copy");
		}
		try(DirectoryStream<F> ds = fileSystem.list(folderName, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			assertTrue(it!=null && it.hasNext(), "must be able to find file just copied to folder ["+folderName+"]");
		}
		if (!fileSystem.folderExists(folderName2)) {
			fileSystem.createFolder(folderName2);
		}
		assertTrue(fileSystem.folderExists(folderName2));

		F destFile1copy = fileSystem.toFile(folderName, fileSystem.getName(destFile1));
		assertTrue(fileSystem.exists(destFile1copy));

		F destFile2 = fileSystem.moveFile(destFile1, folderName2, false);
		assertTrue(fileSystem.exists(sourceFile));
		assertFalse(fileSystem.exists(destFile1copy), "moved file should not exist in source folder anymore");
		assertTrue(fileSystem.exists(destFile2));

		fileSystem.deleteFile(destFile2);
		assertFalse(fileSystem.exists(destFile2), "file should not exist anymore after being deleted");

		fileSystem.removeFolder(folderName2, false);
		assertFalse(fileSystem.folderExists(folderName2), "folder ["+folderName2+"] should not exist anymore after being deleted");

		fileSystem.removeFolder(folderName, false);
		assertFalse(fileSystem.folderExists(folderName), "folder ["+folderName+"] should not exist anymore after being deleted");
	}

	public void testFileSystemUtils() throws Exception {
		String folderName = testFolderPrefix+"_testFileSystemUtils";
		String folderName2 = folderName+"-2";

		if (fileSystem.folderExists(folderName)) {
			fileSystem.removeFolder(folderName, false);
			assertFalse(fileSystem.folderExists(folderName));
		}
		fileSystem.createFolder(folderName);
		assertTrue(fileSystem.folderExists(folderName));

		try(DirectoryStream<F> ds = fileSystem.list(folderName, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			if (it!=null && it.hasNext()) {
				displayFile(it.next());
				fail("just created folder ["+folderName+"] should be emtpty");
			}
			assertFalse(it!=null && it.hasNext(), "just created folder ["+folderName+"] should be emtpty");
		}
		F sourceFile = null;
		F destFile1 = null;
		try(DirectoryStream<F> ds = fileSystem.list(sourceOfMessages_folder, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			assertTrue(it!=null && it.hasNext(), "there must be at least one messsage in the sourceOfMessages_folder ["+sourceOfMessages_folder+"]");

			sourceFile =  it.next();
			assertTrue(fileSystem.exists(sourceFile), "source file should exist");
			//assertFalse("name of source file should not appear in just created folder", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(sourceFile)));

			destFile1 = FileSystemUtils.copyFile(fileSystem, sourceFile, folderName, true, 0, false, true);
			assertTrue(fileSystem.exists(sourceFile), "source file should still exist after copy");

			//displayFile(destFile1);
			assertTrue(fileSystem.exists(destFile1), "destination file should exist after copy");
			//assertTrue("name of destination file should exist in folder after copy", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)));
		}
		try(DirectoryStream<F> ds = fileSystem.list(folderName, TypeFilter.FILES_ONLY)) {
			Iterator<F> it = ds.iterator();
			assertTrue(it!=null && it.hasNext(), "must be able to find file just copied to folder ["+folderName+"]");
		}
		if (!fileSystem.folderExists(folderName2)) {
			fileSystem.createFolder(folderName2);
		}
		assertTrue(fileSystem.folderExists(folderName2));

		F destFile2 = FileSystemUtils.moveFile(fileSystem, destFile1, folderName2, true, 0, false, true);
		assertTrue(fileSystem.exists(sourceFile));
		//assertFalse(fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)), "moved file should not exist in source folder anymore");
		assertTrue(fileSystem.exists(destFile2));

		fileSystem.deleteFile(destFile2);
		assertFalse(fileSystem.exists(destFile2));

		fileSystem.removeFolder(folderName2, false);
		assertFalse(fileSystem.folderExists(folderName2));

		fileSystem.removeFolder(folderName, false);
		assertFalse(fileSystem.folderExists(folderName));
	}

	@Test
	public void doAllTests() throws Exception {
		testFolders();
		testFiles();
		testFileSystemUtils();
	}

}
