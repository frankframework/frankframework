package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.junit.Test;

public abstract class FileSystemTest<F, FS extends IWritableFileSystem<F>> extends BasicFileSystemTest<F,FS> {

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
	public void writableFileSystemTestCreateAndRemoveFolder() throws Exception {
		String folderName = "dummyFolder";
		
		fileSystem.configure();
		fileSystem.open();

		_createFolder(folderName);
		waitForActionToFinish();
		
		assertTrue("folder does not exist after creation",_folderExists(folderName));
		
		fileSystem.removeFolder(folderName);
		waitForActionToFinish();
		
		assertFalse("folder still exists after removal", _folderExists(folderName));
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
		fileSystem.renameFile(f, destination, false);
		waitForActionToFinish();
		
		assertTrue("Destination must exist",_fileExists(destination));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
	}
	
	@Test
	public void writableFileSystemTestRenameToExisting() throws Exception {
		exception.expectMessage("Cannot rename file. Destination file already exists.");
		String fileName = "fileToBeRenamedExisting.txt";
		
		fileSystem.configure();
		fileSystem.open();

		createFile(null, fileName, "");
		waitForActionToFinish();
		
		assertTrue(_fileExists(fileName));
		
		String destination = "fileRenamedExists.txt";
		createFile(null, destination, "");
		waitForActionToFinish();
		
		F f = fileSystem.toFile(fileName);
		fileSystem.renameFile(f, destination, false);
		waitForActionToFinish();
		
		assertTrue("Origin must still exist",_fileExists(fileName));
		assertTrue("Destination must exist",_fileExists(destination));
	}

	@Test
	public void writableFileSystemTestRemovingNonExistingDirectory() throws Exception {
		exception.expectMessage("Directory does not exist.");
		String foldername = "nonExistingFolder";

		fileSystem.configure();
		fileSystem.open();

		if(_folderExists(foldername)) {
			_deleteFolder(foldername);
		}
		fileSystem.removeFolder(foldername);
	}
	
	@Test
	public void writableFileSystemTestCreateExistingFolder() throws Exception {
		exception.expectMessage("Directory already exists.");
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
		
		Iterator<F> files = fileSystem.listFiles(null);
		F f = null;
		if(files.hasNext()) {
			f = files.next();
		}
		else {
			fail("File not found");
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

}