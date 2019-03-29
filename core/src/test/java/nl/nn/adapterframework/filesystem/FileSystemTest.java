package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.io.PrintWriter;

import org.junit.Test;

public abstract class FileSystemTest<F, FS extends IWritableFileSystem<F>> extends BasicFileSystemTest<F,FS> {

	@Test
	public void fileSystemTestCreateNewFile() throws Exception {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";
		
		deleteFile(filename);
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
		
		String actual = readFile(filename);
		// test
		equalsCheck(contents.trim(), actual.trim());

	}

	@Test
	public void fileSystemTestCreateOverwriteFile() throws Exception {
		String filename = "overwrited" + FILE1;
		
		createFile(filename, "Eerste versie van de file");
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

		String actual = readFile(filename);
		// test
		equalsCheck(contents.trim(), actual.trim());
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
		PrintWriter pw = new PrintWriter(out);
		pw.println(regel2);
		pw.close();
		out.close();
		waitForActionToFinish();
		// test
		existsCheck(filename);

		String actual = readFile(filename);
		// test
		equalsCheck(expected.trim(), actual.trim());
	}

	@Test
	public void fileSystemTestCreateAndRemoveFolder() throws Exception {
		String folderName = "dummyFolder";
		
		_createFolder(folderName);
		waitForActionToFinish();
		
		assertTrue("folder does not exist after creation",_folderExists(folderName));
		
		F f = fileSystem.toFile(folderName);
		fileSystem.removeFolder(f);
		waitForActionToFinish();
		
		assertFalse("folder still exists after removal", _folderExists(folderName));
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
		fileSystem.renameFile(f, destination);
		waitForActionToFinish();
		
		assertTrue("Destination must exist",_fileExists(destination));
		assertFalse("Origin must have disappeared",_fileExists(fileName));
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
		fileSystem.renameFile(f, destination);
		waitForActionToFinish();
		
		assertTrue("Origin must still exist",_fileExists(fileName));
		assertTrue("Destination must exist",_fileExists(destination));
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
		String folderName = "existingFolder";
		
		_createFolder(folderName);
		waitForActionToFinish();
		F f = fileSystem.toFile(folderName);
		fileSystem.createFolder(f);
	}
	

}