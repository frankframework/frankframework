package nl.nn.adapterframework.filesystem;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;

import liquibase.util.StreamUtil;
import nl.nn.adapterframework.configuration.ConfigurationException;

public abstract class FileSystemTest<F, FS extends IFileSystemBase<F>> {

	public String FILE1 = "file1.txt";
	public String FILE2 = "file2.txt";
	public String DIR1 = "testDirectory/";
	public String DIR2 = "testDirectory2/";

	protected abstract FS getFileSystem() throws ConfigurationException;

	//	protected abstract F getFileHandle(String filename);
	//	protected abstract String getFileInfo(F f); // expected to return a XML 

	protected abstract boolean _fileExists(String filename);

	protected abstract void _deleteFile(String filename);

	protected abstract OutputStream _createFile(String filename) throws IOException;

	protected abstract InputStream _readFile(String filename) throws FileNotFoundException;

	public abstract void _createFolder(String filename) throws IOException;

	protected FS fileSystem;

	@Before
	public void setup() throws IOException, ConfigurationException {
		fileSystem = getFileSystem();
		fileSystem.configure();
	}

	public void deleteFile(String filename) {
		if (_fileExists(filename)) {
			_deleteFile(filename);
		}
	}

	public void createFile(String filename, String contents) throws IOException {
		OutputStream out = _createFile(filename);
		if (contents != null)
			out.write(contents.getBytes());
		out.close();
	}

	public String readFile(String filename) throws IOException {
		InputStream in = _readFile(filename);
		return StreamUtil.getReaderContents(new InputStreamReader(in));
	}

	@Test
	public void testExists() throws IOException, FileSystemException {
		//setup negative
		String filename = "testExists" + FILE1;
		deleteFile(filename);
		// test
		F file = fileSystem.toFile(filename);
		assertFalse("Expected file[" + filename + "] not to be present", fileSystem.exists(file));

	}

	@Test
	public void testExistsContinue() throws FileSystemException, IOException {
		String filename = FILE1;
		// setup positive
		F file = fileSystem.toFile(filename);
		createFile(filename, "tja");
		// test
		assertTrue("Expected file[" + filename + "] to be present", fileSystem.exists(file));
	}

	@Test
	public void testCreateNewFile() throws IOException, FileSystemException {
		String filename = "create" + FILE1;
		String contents = "regeltje tekst";
		deleteFile(filename);

		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);

		PrintWriter pw = new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		fileSystem.finalizeAction();

		assertTrue(_fileExists(filename));
		String actual = readFile(filename);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void testCreateOverwriteFile() throws IOException, FileSystemException {
		String filename = "overwrited" + FILE1;
		createFile(filename, "Eerste versie van de file");
		String contents = "Tweede versie van de file";

		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);

		PrintWriter pw = new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		fileSystem.finalizeAction();

		assertTrue(_fileExists(filename));
		String actual = readFile(filename);
		assertEquals(contents.trim(), actual.trim());
	}

	@Test
	public void testTruncateFile() throws IOException, FileSystemException {
		String filename = "truncated" + FILE1;
		createFile(filename, "Eerste versie van de file");

		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.createFile(file);
		out.close();
		fileSystem.finalizeAction();

		assertTrue(_fileExists(filename));
		String actual = readFile(filename);
		assertEquals("", actual.trim());
	}

	@Test
	public void testAppendFile() throws IOException, FileSystemException {
		String filename = FILE1;
		String regel1 = "Eerste regel in de file";
		String regel2 = "Tweede regel in de file";
		String expected = regel1 + regel2;
		createFile(filename, regel1);

		F file = fileSystem.toFile(filename);
		OutputStream out = fileSystem.appendFile(file);

		PrintWriter pw = new PrintWriter(out);
		pw.println(regel2);
		pw.close();
		out.close();
		fileSystem.finalizeAction();

		assertTrue(_fileExists(filename));
		String actual = readFile(filename);
		assertEquals(expected.trim(), actual.trim());
	}

	@Test
	public void testDelete() throws IOException, FileSystemException {
		String filename = "tobeDeleted" + FILE1;
		createFile(filename, "maakt niet uit");
		assertTrue(_fileExists(filename));

		F file = fileSystem.toFile(filename);
		fileSystem.deleteFile(file);

		assertFalse(fileSystem.exists(file));
	}

	//	public void assertFileEquals(String filename, String expectedContents) throws IOException {
	//		InputStream in = new FileInputStream(filename);
	//
	//		String actual=StreamUtil.getReaderContents(new InputStreamReader(in));
	//		assertEquals(expectedContents.trim(),actual.trim());
	//	}

	public void testReadFile(F file, String expectedContents)
			throws IOException, FileSystemException {
		InputStream in = fileSystem.readFile(file);

		String actual = StreamUtil.getReaderContents(new InputStreamReader(in));
		assertEquals(expectedContents.trim(), actual.trim());
	}

	@Test
	public void testRead() throws IOException, FileSystemException {
		String filename = FILE1;
		String contents = "Tekst om te lezen";
		createFile(filename, contents);
		assertTrue(_fileExists(filename));

		F file = fileSystem.toFile(filename);
		testReadFile(file, contents);
	}

	public void testFileInfo(F f) throws FileSystemException {
		String fiString = fileSystem.getInfo(f);
		assertThat(fiString, containsString("name"));
		assertThat(fiString, containsString("lastModified"));
	}

	public void testFileInfo() throws FileSystemException {
		testFileInfo(fileSystem.toFile(FILE1));
		testFileInfo(fileSystem.toFile(FILE2));
	}

	@Test
	/*
	 * TODO 2019-01-04 GvB fix this test: order of files is not guaranteed! fails on travis
	 */
	public void testListFile() throws IOException, FileSystemException {
		String contents1 = "maakt niet uit";
		String contents2 = "maakt ook niet uit";
		createFile(FILE1, contents1);
		createFile(FILE2, contents2);
		Iterator<F> it = fileSystem.listFiles();

		F file;
		for (int i = 0; i < 8; i++) {
			assertTrue(it.hasNext());
			file = it.next();
		}
		assertFalse(it.hasNext());

		deleteFile(FILE1);

		it = fileSystem.listFiles();
		for (int i = 0; i < 7; i++) {
			assertTrue(it.hasNext());
			file = it.next();
		}
		assertFalse(it.hasNext());
	}
}