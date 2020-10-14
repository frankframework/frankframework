package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

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
	public void localFileSystemTestCreateNewFileAbsolute() throws Exception {
		String filename = "createFileAbsolute" + FILE1;
		String contents = "regeltje tekst";
		
		fileSystem.configure();
		fileSystem.open();

		deleteFile(null, filename);
		waitForActionToFinish();
		
		File file = fileSystem.toFile(fileSystem.getRoot()+"/"+filename);
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
	public void localFileSystemTestToFileAbsoluteLongFilenameInRoot() throws Exception {
		assumeTrue(System.getProperty("os.name").startsWith("Windows"));
		String filename = "FileInLongRoot.txt";
		String contents = "regeltje tekst";
		
		String rootFolderLongName = "VeryLongRootFolderName";
		String rootFolderShortName = "VERYLO~1";
		_createFolder(rootFolderLongName);
		createFile(rootFolderLongName, filename, contents);
		
		LocalFileSystem fsLong = new LocalFileSystem();
		fsLong.setRoot(folder.getRoot().getAbsolutePath()+"\\"+rootFolderLongName);
		fsLong.configure();
		fsLong.open();

		LocalFileSystem fsShort = new LocalFileSystem();
		fsShort.setRoot(folder.getRoot().getAbsolutePath()+"\\"+rootFolderShortName);
		fsShort.configure();
		fsShort.open();
		
		File fLong = fsLong.toFile(filename);
		File fShort = fsShort.toFile(filename);
		
		System.out.println("Flong ["+fLong.getName()+"] absolute path ["+fLong.getAbsolutePath()+"]");
		System.out.println("Fshort ["+fShort.getName()+"] absolute path ["+fShort.getAbsolutePath()+"]");
		
		assertTrue(fsLong.exists(fLong));
		assertTrue(fsShort.exists(fShort));
		
		assertTrue(fsLong.exists(fsLong.toFile(fLong.getAbsolutePath())));
		assertTrue(fsShort.exists(fsShort.toFile(fShort.getAbsolutePath())));

		assertTrue("LocalFileSystem with short path root must accept absolute filename with long path root", fsShort.exists(fsShort.toFile(fLong.getAbsolutePath())));
		assertTrue("LocalFileSystem with long path root must accept absolute filename with short path root", fsLong.exists(fsLong.toFile(fShort.getAbsolutePath())));
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
