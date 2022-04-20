package nl.nn.adapterframework.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.credentialprovider.util.Misc;

public class LocalFileSystemTest extends FileSystemTest<Path, LocalFileSystem>{

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

		Path file = fileSystem.toFile(fileSystem.getRoot()+"/"+filename);
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
		assumeTrue("Test is for long and short filename compatibility, which is a Windows only thing", System.getProperty("os.name").startsWith("Windows"));
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

		Path fLong = fsLong.toFile(filename);
		Path fShort = fsShort.toFile(filename);

		log.info("Flong ["+fLong.getFileName().toString()+"] absolute path ["+fLong.toAbsolutePath()+"]");
		log.info("Fshort ["+fShort.getFileName().toString()+"] absolute path ["+fShort.toAbsolutePath()+"]");

		assertTrue(fsLong.exists(fLong));
		assertTrue(fsShort.exists(fShort));

		assertTrue(fsLong.exists(fsLong.toFile(fLong.toAbsolutePath().toString())));
		assertTrue(fsShort.exists(fsShort.toFile(fShort.toAbsolutePath().toString())));

		assertTrue("LocalFileSystem with short path root must accept absolute filename with long path root", fsShort.exists(fsShort.toFile(fLong.toAbsolutePath().toString())));
		assertTrue("LocalFileSystem with long path root must accept absolute filename with short path root", fsLong.exists(fsLong.toFile(fShort.toAbsolutePath().toString())));
	}

	@Test
	public void fileSystemCharset() throws Exception {
		fileSystem.configure();
		fileSystem.open();

		URL testFile = TestFileUtils.getTestFileURL("/Util/MessageUtils/iso-8859-1.txt");
		assertNotNull(testFile);

		Message result = fileSystem.readFile(new File(testFile.toURI()).toPath(), "auto");
		assertEquals(Misc.streamToString(testFile.openStream(), "iso-8859-1"), result.asString());
	}
}
