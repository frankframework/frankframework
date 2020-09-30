package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class LocalFileSystemTest extends FileSystemTest<File, LocalFileSystem>{

	public TemporaryFolder folder;


	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.getRoot().getAbsolutePath());
		return result;
	}

	@Override
	public void setUp() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		super.setUp();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		return new LocalFileSystemTestHelper(folder);
	}
	

	@Test
	public void localFileSystemTestListWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename+".bak";
		String filename2 = filename+".xml";
		String contents = "regeltje tekst";
		
		fileSystem.setWildcard("*.xml");
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();
		
		Iterator<File> files = fileSystem.listFiles(null);
		File f = null;
		if(files.hasNext()) {
			f = files.next();
		}
		else {
			fail("No file found");
		}
		assertEquals(filename2,fileSystem.getName(f));
		assertFalse(files.hasNext());
	}

	@Test
	public void localFileSystemTestListExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename+".bak";
		String filename2 = filename+".xml";
		String contents = "regeltje tekst";
		
		fileSystem.setExcludeWildcard("*.bak");
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();
		
		Iterator<File> files = fileSystem.listFiles(null);
		File f = null;
		if(files.hasNext()) {
			f = files.next();
		}
		else {
			fail("No file found");
		}
		assertEquals(filename2,fileSystem.getName(f));
		assertFalse(files.hasNext());
	}

	@Test
	public void localFileSystemTestListIncludeExcludeWildcard() throws Exception {
		String filename = "create" + FILE1;
		String filename1 = filename+".oud.xml";
		String filename2 = filename+".xml";
		String contents = "regeltje tekst";
		
		fileSystem.setWildcard("*.xml");
		fileSystem.setExcludeWildcard("*.oud.xml");
		fileSystem.configure();
		fileSystem.open();

		createFile(null, filename1, contents);
		createFile(null, filename2, contents);
		waitForActionToFinish();
		
		Iterator<File> files = fileSystem.listFiles(null);
		File f = null;
		if(files.hasNext()) {
			f = files.next();
		}
		else {
			fail("No file found");
		}
		assertEquals(filename2,fileSystem.getName(f));
		assertFalse(files.hasNext());
	}


}
