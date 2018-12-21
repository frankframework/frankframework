package nl.nn.adapterframework.filesystem;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.*;

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

public abstract class FileSystemTest<F,FS extends IFileSystemBase<F>> {

	public String FILE1="file1.txt";
	public String FILE2="file2.txt";

	protected abstract FS getFileSystem();
	protected abstract F getFileHandle(String filename);
//	protected abstract String getFileInfo(F f); // expected to return a XML 
	
	protected abstract boolean _fileExists(F f);
	protected abstract void _deleteFile(F f);
	protected abstract OutputStream _createFile(F f) throws IOException;
	protected abstract InputStream _readFile(F f) throws FileNotFoundException;

	protected FS fileSystem;
	
	@Before
	public void setup() throws IOException {
		fileSystem=getFileSystem();
	}

	
	public void deleteFile(F f) {
		if (_fileExists(f)) {
			_deleteFile(f);
		}
	}
	public void createFile(F f, String contents) throws IOException {
		OutputStream out=_createFile(f);
		if (contents!=null) out.write(contents.getBytes());
		out.close();
	}
		
	public String readFile(F f) throws IOException {
		InputStream in =_readFile(f);
		return StreamUtil.getReaderContents(new InputStreamReader(in));
	}
	
	@Test
	public void testExists() throws IOException {
		//setup negative
		F file = getFileHandle(FILE1);
		deleteFile(file);
		// test
		assertFalse(fileSystem.exists(file));

		// setup positive
		createFile(file,"tja");
		// test
		assertTrue(fileSystem.exists(file));
	}
	
	@Test
	public void testCreateNewFile() throws IOException {
		F file = getFileHandle(FILE1);
		String contents="regeltje tekst";
		deleteFile(file);
		
		OutputStream out=fileSystem.createFile(file);

		PrintWriter pw=new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		assertTrue(_fileExists(file));
		String actual=readFile(file);
		assertEquals(contents.trim(),actual.trim());
	}

	@Test
	public void testCreateOverwriteFile() throws IOException {
		F file = getFileHandle(FILE1);
		createFile(file,"Eerste versie van de file");
		String contents="Tweede versie van de file";
		
		OutputStream out=fileSystem.createFile(file);

		PrintWriter pw=new PrintWriter(out);
		pw.println(contents);
		pw.close();
		out.close();
		assertTrue(_fileExists(file));
		String actual=readFile(file);
		assertEquals(contents.trim(),actual.trim());
	}

	@Test
	public void testTruncateFile() throws IOException {
		F file = getFileHandle(FILE1);
		createFile(file,"Eerste versie van de file");
		
		OutputStream out=fileSystem.createFile(file);
		
		out.close();
		assertTrue(_fileExists(file));
		String actual=readFile(file);
		assertEquals("",actual.trim());
	}

	@Test
	public void testAppendFile() throws IOException {
		F file = getFileHandle(FILE1);
		String regel1="Eerste regel in de file";
		String regel2="Tweede regel in de file";
		String expected=regel1+regel2;
		createFile(file,regel1);
		
		OutputStream out=fileSystem.appendFile(file);

		PrintWriter pw=new PrintWriter(out);
		pw.println(regel2);
		pw.close();
		out.close();
		assertTrue(_fileExists(file));
		String actual=readFile(file);
		assertEquals(expected.trim(),actual.trim());
	}

	@Test
	public void testDelete() throws IOException {
		F file = getFileHandle(FILE1);
		createFile(file,"maakt niet uit");
		assertTrue(fileSystem.exists(file));
		
		fileSystem.deleteFile(file);
		
		assertFalse(fileSystem.exists(file));
	}

//	public void assertFileEquals(String filename, String expectedContents) throws IOException {
//		InputStream in = new FileInputStream(filename);
//
//		String actual=StreamUtil.getReaderContents(new InputStreamReader(in));
//		assertEquals(expectedContents.trim(),actual.trim());
//	}

	
	public void testReadFile(F file, String expectedContents) throws IOException {
		InputStream in = fileSystem.readFile(file);

		String actual=StreamUtil.getReaderContents(new InputStreamReader(in));
		assertEquals(expectedContents.trim(),actual.trim());
	}

	
	@Test
	public void testRead() throws IOException {
		F file = getFileHandle(FILE1);
		String contents="Tekst om te lezen";
		createFile(file,contents);
		assertTrue(fileSystem.exists(file));
		
		testReadFile(file,contents);
	}

	public void testFileInfo(F f) {
		String fiString=fileSystem.getInfo(f);
		assertThat(fiString,containsString("name"));
		assertThat(fiString,containsString("lastmodified"));
	}
	
	public void testFileInfo() {
		testFileInfo(getFileHandle(FILE1));
		testFileInfo(getFileHandle(FILE2));
	}
	
	@Test
	public void testListFile() throws IOException {
		F file1 = getFileHandle(FILE1);
		F file2 = getFileHandle(FILE2);
		String contents1="maakt niet uit";
		String contents2="maakt ook niet uit";
		createFile(file1,contents1);
		createFile(file2,contents2);
		
		System.out.println("file 1=["+file1+"]");
		
		Iterator<F> it =fileSystem.listFiles();
		assertTrue(it.hasNext());
		F file=it.next();
		System.out.println("file =["+file+"]");
		testReadFile(file,contents1);
		assertTrue(it.hasNext());
		file=it.next();
		testReadFile(file,contents2);
		assertFalse(it.hasNext());
		
		deleteFile(file1);
		
		it =fileSystem.listFiles();
		assertTrue(it.hasNext());
		file=it.next();
		testReadFile(file,contents2);
		assertFalse(it.hasNext());
	}

}
