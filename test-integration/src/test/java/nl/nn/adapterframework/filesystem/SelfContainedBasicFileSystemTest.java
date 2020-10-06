package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Iterator;

import org.junit.Test;

public abstract class SelfContainedBasicFileSystemTest<F, FS extends IBasicFileSystem<F>> extends BasicFileSystemTestBase<F, FS>{

//	private String testFolderPrefix = "fs_test_"+DateUtils.format(new Date(),"yyyy-MM-dd_HHmmss.SSS");
	private String testFolderPrefix = "fs_test";
	private String sourceOfMessages_folder=null;
	


	
	public void testFolders()  throws Exception{
		String folderName = testFolderPrefix+"_testFolders";
		
		fileSystem.createFolder(folderName);
		assertTrue(fileSystem.folderExists(folderName));

		fileSystem.removeFolder(folderName);
		assertFalse(fileSystem.folderExists(folderName));
	}
	
	public void displayFile(F f) throws FileSystemException {
		log.debug("file subject ["+fileSystem.getAdditionalFileProperties(f).get("subject")+"] name ["+fileSystem.getName(f)+"]");
		//log.debug("file canonical name ["+fileSystem.getCanonicalName(f)+"]");
//		log.debug("file size ["+fileSystem.getFileSize(f)+"]");
//		log.debug("file subject ["+fileSystem.getAdditionalFileProperties(f).get("subject")+"]");
//		log.debug("file props ["+fileSystem.getAdditionalFileProperties(f)+"]");
	}
	
//	public void listAllFilesInFolder(String folderName) throws FileSystemException {
//		Iterator<F> it = fileSystem.listFiles(folderName);
//		while (it!=null && it.hasNext()) {
//			displayFile(it.next());
//		}		
//	}
	
	public void testFiles()  throws Exception{
		String folderName = testFolderPrefix+"_testFiles";
		String folderName2 = folderName+"-2";
		
		if (fileSystem.folderExists(folderName)) {
			fileSystem.removeFolder(folderName);
			assertFalse(fileSystem.folderExists(folderName));
		}
		fileSystem.createFolder(folderName);
		assertTrue(fileSystem.folderExists(folderName));
		
		Iterator<F> it;
		it = fileSystem.listFiles(folderName);
		if (it!=null && it.hasNext()) {
			displayFile(it.next());
			fail("just created folder ["+folderName+"] should be emtpty");
		}
		assertFalse("just created folder ["+folderName+"] should be emtpty", it!=null && it.hasNext());

		it = fileSystem.listFiles(sourceOfMessages_folder);
		assertTrue("there must be at least one messsage in the sourceOfMessages_folder ["+sourceOfMessages_folder+"]", it!=null && it.hasNext());
		
		F sourceFile =  it.next();
		assertTrue("source file should exist", fileSystem.exists(sourceFile));
		//assertFalse("name of source file should not appear in just created folder", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(sourceFile)));
		//displayFile(sourceFile);
		
		F destFile1 = fileSystem.copyFile(sourceFile, folderName, false);
		assertTrue("source file should still exist after copy", fileSystem.exists(sourceFile));

		//displayFile(destFile1);
		assertTrue("destination file should exist after copy", fileSystem.exists(destFile1));
		//assertTrue("name of destination file should exist in folder after copy", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)));
		
		it = fileSystem.listFiles(folderName);
		assertTrue("must be able to find file just copied to folder ["+folderName+"]", it!=null && it.hasNext());
		
		if (!fileSystem.folderExists(folderName2)) {
			fileSystem.createFolder(folderName2);
		}
		assertTrue(fileSystem.folderExists(folderName2));
		
		F destFile2 = fileSystem.moveFile(destFile1, folderName2, false);
		assertTrue(fileSystem.exists(sourceFile));
		//assertFalse("moved file should not exist in source folder anymore", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)));
		assertTrue(fileSystem.exists(destFile2));
		
		fileSystem.deleteFile(destFile2);
		assertFalse(fileSystem.exists(destFile2));

		fileSystem.removeFolder(folderName2);
		assertFalse(fileSystem.folderExists(folderName2));

		fileSystem.removeFolder(folderName);
		assertFalse(fileSystem.folderExists(folderName));
	}

	public void testFileSystemUtils()  throws Exception{
		String folderName = testFolderPrefix+"_testFileSystemUtils";
		String folderName2 = folderName+"-2";
		
		if (fileSystem.folderExists(folderName)) {
			fileSystem.removeFolder(folderName);
			assertFalse(fileSystem.folderExists(folderName));
		}
		fileSystem.createFolder(folderName);
		assertTrue(fileSystem.folderExists(folderName));
		
		Iterator<F> it;
		it = fileSystem.listFiles(folderName);
		if (it!=null && it.hasNext()) {
			displayFile(it.next());
			fail("just created folder ["+folderName+"] should be emtpty");
		}
		assertFalse("just created folder ["+folderName+"] should be emtpty", it!=null && it.hasNext());

		it = fileSystem.listFiles(sourceOfMessages_folder);
		assertTrue("there must be at least one messsage in the sourceOfMessages_folder ["+sourceOfMessages_folder+"]", it!=null && it.hasNext());
		
		F sourceFile =  it.next();
		assertTrue("source file should exist", fileSystem.exists(sourceFile));
		//assertFalse("name of source file should not appear in just created folder", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(sourceFile)));
		
		F destFile1 = FileSystemUtils.copyFile(fileSystem, sourceFile, folderName, true, 0, false);
		assertTrue("source file should still exist after copy", fileSystem.exists(sourceFile));

		//displayFile(destFile1);
		assertTrue("destination file should exist after copy", fileSystem.exists(destFile1));
		//assertTrue("name of destination file should exist in folder after copy", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)));
		
		it = fileSystem.listFiles(folderName);
		assertTrue("must be able to find file just copied to folder ["+folderName+"]", it!=null && it.hasNext());
		
		if (!fileSystem.folderExists(folderName2)) {
			fileSystem.createFolder(folderName2);
		}
		assertTrue(fileSystem.folderExists(folderName2));
		
		F destFile2 = FileSystemUtils.moveFile(fileSystem, destFile1, folderName2, true, 0, false);
		assertTrue(fileSystem.exists(sourceFile));
		//assertFalse("moved file should not exist in source folder anymore", fileSystem.filenameExistsInFolder(folderName, fileSystem.getName(destFile1)));
		assertTrue(fileSystem.exists(destFile2));
		
		fileSystem.deleteFile(destFile2);
		assertFalse(fileSystem.exists(destFile2));

		fileSystem.removeFolder(folderName2);
		assertFalse(fileSystem.folderExists(folderName2));

		fileSystem.removeFolder(folderName);
		assertFalse(fileSystem.folderExists(folderName));
	}

	@Test
	public void doAllTests() throws Exception {
		testFolders();
		testFiles();
		testFileSystemUtils();
	}

}