package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.DirectoryStream;
import java.util.Iterator;

import org.junit.Test;

import static org.hamcrest.core.AnyOf.anyOf;
import static org.hamcrest.core.StringEndsWith.endsWith;

import nl.nn.adapterframework.stream.Message;

public abstract class FileSystemTest<F, FS extends IWritableFileSystem<F>> extends HelperedBasicFileSystemTest<F,FS> {

	@Test
	public void writableFileSystemTestCreateNewFile() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";
		
		fileSystem.configure();
		fileSystem.open();

		deleteFile(null, filename);
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);
		PrintWriter pw = new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);
		
		String actual = readFile(null, filename);
		// test
		equalsCheck(contents.trim(), actual.trim());

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
		OutputStream out = fileSystem.createFile(file);
		PrintWriter pw = new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		equalsCheck(contents.trim(), actual.trim());
	}


	@Test
	public void writableFileSystemTestTruncateFile() throws Exception {
		String filename = "truncated" + FILE1;
		
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename, "Eerste versie van de file");
		waitForActionToFinish();
		
		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);
		
		String actual = readFile(null, filename);
		// test
		equalsCheck("", actual.trim());
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
		OutputStream out = fileSystem.appendFile(file);
		PrintWriter pw = new PrintWriter(out);
		pw.println(regel2);
		pw.close();
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(null, filename);
		// test
		equalsCheck(expected.trim(), actual.trim());
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
		OutputStream out = fileSystem.appendFile(file);
		PrintWriter pw = new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);
		
		String actual = readFile(null, filename);
		// test
		equalsCheck(contents.trim(), actual.trim());

	}


	@Test
	public void writableFileSystemTestCreateFolder() throws Exception {
		String folderName = "dummyFolder";
		
		fileSystem.configure();
		fileSystem.open();

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
			waitForActionToFinish();
			assertFalse("could not remove folder before test", _folderExists(folderName));
		}

		fileSystem.createFolder(folderName);
		waitForActionToFinish();
		
		assertTrue("folder does not exist after creation",_folderExists(folderName));
	}
	
	@Test
	public void writableFileSystemTestRemoveFolder() throws Exception {
		String folderName = "dummyFolder";
		
		fileSystem.configure();
		fileSystem.open();

		if (!_folderExists(folderName)) {
			_createFolder(folderName);
			waitForActionToFinish();
			assertTrue("could not create folder before test", _folderExists(folderName));
		}
		
		fileSystem.removeFolder(folderName, false);
		waitForActionToFinish();
		
		assertFalse("folder still exists after removal", _folderExists(folderName));
	}

	@Test
	public void writableFileSystemTestRemoveNonEmptyFolder() throws Exception {
		String folderName = "dummyTestFolder";
		
		fileSystem.configure();
		fileSystem.open();

		if (!_folderExists(folderName)) {
			_createFolder(folderName);
			waitForActionToFinish();
			assertTrue("could not create folder before test", _folderExists(folderName));
		}
		
		for(int i=0;i<3;i++) {
			createFile(folderName, "file_"+i+".txt", "some text here");
		}

		fileSystem.removeFolder(folderName, true);
		waitForActionToFinish();

		assertFalse("folder still exists after removal", _folderExists(folderName));
	}
	
	@Test
	public void writableFileSystemTestRemoveFolderRecursive() throws Exception {
		String folderName = "dummyTestFolder";
		String innerFolder = folderName+"/innerfolder";
		fileSystem.configure();
		fileSystem.open();

		if (!_folderExists(folderName)) {
			_createFolder(folderName);
			waitForActionToFinish();
			assertTrue("could not create folder before test", _folderExists(folderName));
		}
		
		if (!_folderExists(innerFolder)) {
			_createFolder(innerFolder);
			waitForActionToFinish();
			assertTrue("could not create folder before test", _folderExists(folderName));
		}
		
		for(int i=0;i<3;i++) {
			createFile(folderName, "file_"+i+".txt", "some text here");
			createFile(innerFolder, "file_"+i+".txt", "some text here");
		}

		fileSystem.removeFolder(folderName, true);
		waitForActionToFinish();

		assertFalse("folder still exists after removal", _folderExists(folderName));
	}
	
	@Test
	public void writableFileSystemTestFolderExists() throws Exception {
		String folderName = "dummyFolder";
		
		fileSystem.configure();
		fileSystem.open();

		if (!_folderExists(folderName)) {
			_createFolder(folderName);
			waitForActionToFinish();
			assertTrue("could not create folder before test", _folderExists(folderName));
		}
		
		assertTrue("existing folder is not seen", fileSystem.folderExists(folderName));
	}

	@Test
	public void writableFileSystemTestFolderDoesNotExist() throws Exception {
		String folderName = "dummyFolder";
		
		fileSystem.configure();
		fileSystem.open();

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
			waitForActionToFinish();
			assertFalse("could not remove folder before test", _folderExists(folderName));
		}

		assertFalse("non existing folder is seen", fileSystem.folderExists(folderName));
	}
	
	@Test
	public void writableFileSystemTestFileIsNotAFolder() throws Exception {
		String folderName = "dummyFile";
		
		fileSystem.configure();
		fileSystem.open();

		if (_folderExists(folderName)) {
			_deleteFolder(folderName);
			waitForActionToFinish();
			assertFalse("could not remove folder before test", _folderExists(folderName));
		}
		
		if (!_fileExists(folderName)) {
			createFile(null, folderName, "tja");
			waitForActionToFinish();
			assertTrue("file must exist before test", _fileExists(folderName));
		}

		assertFalse("file must not be seen as folder", fileSystem.folderExists(folderName));
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
		
		assertTrue("Destination must exist",_fileExists(destination));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
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
		
		assertTrue("Destination must exist",_fileExists(destinationFolder, destination));
		assertFalse("Origin must have disappeared",_fileExists(sourceFolder, fileName));
	}
	
	@Test
	public void writableFileSystemTestRemovingNonExistingDirectory() throws Exception {
		thrown.expectMessage("Directory does not exist.");
		String foldername = "nonExistingFolder";

		fileSystem.configure();
		fileSystem.open();

		if(_folderExists(foldername)) {
			_deleteFolder(foldername);
		}
		fileSystem.removeFolder(foldername, false);
	}
	
	@Test
	public void writableFileSystemTestCreateExistingFolder() throws Exception {
		thrown.expectMessage("Directory already exists.");
		String folderName = "existingFolder";
		
		fileSystem.configure();
		fileSystem.open();

		_createFolder(folderName);
		waitForActionToFinish();
		fileSystem.createFolder(folderName);
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
		try(DirectoryStream<F> ds = fileSystem.listFiles(null)) {
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
		
		assertNotNull("Canonical name should not be null", canonicalName);
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
		assertTrue("Expected the file ["+filename+"] to be present", _fileExists(filename));
		
		Message result = fileSystem.readFile(file, null);
		assertEquals(content, result.asString());
		
		fileSystem.deleteFile(file);
		waitForActionToFinish();
		assertFalse("Expected the file ["+filename+"] not to be present", _fileExists(filename));
	}
	
	@Test
	public void writableFileSystemTestDeleteUploadedFile() throws Exception{
		String filename = "fileToBeUploadedAndDeleted.txt";
		String content = "some content";
		
		fileSystem.configure();
		fileSystem.open();
		
		F file = fileSystem.toFile(filename);
		
		OutputStream out = fileSystem.createFile(file);
		out.write(content.getBytes());
		out.close();
		
		assertTrue("Expected the file ["+filename+"] to be present",_fileExists(filename));

		fileSystem.deleteFile(file);
		waitForActionToFinish();
		assertFalse("Expected the file ["+filename+"] not to be present", _fileExists(filename));
		
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
		assertTrue("Expected the file ["+filename+"] to be present", _fileExists(filename));
		
		OutputStream out = fileSystem.appendFile(file);
		out.write(content.getBytes());
		out.close();
		
		
		fileSystem.deleteFile(file);
		waitForActionToFinish();
		
		assertFalse("Expected the file ["+filename+"] not to be present", _fileExists(filename));
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

		try (OutputStream out1 = fileSystem.createFile(fileSystem.toFile(filename))) {
		}
		assertTrue(fileSystem.exists(fileSystem.toFile(filename)));

	}
	
}