package nl.nn.adapterframework.filesystem;

import static org.junit.Assume.assumeTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.Misc;

public class LocalFileSystemTest extends FileSystemTest<Path, LocalFileSystem>{

	@TempDir
	public Path folder;

	@Override
	protected LocalFileSystem createFileSystem() {
		LocalFileSystem result=new LocalFileSystem();
		result.setRoot(folder.toAbsolutePath().toString());
		return result;
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
		assertEquals(contents.trim(), actual.trim());

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
		fsLong.setRoot(folder.toAbsolutePath()+"\\"+rootFolderLongName);
		fsLong.configure();
		fsLong.open();

		LocalFileSystem fsShort = new LocalFileSystem();
		fsShort.setRoot(folder.toAbsolutePath()+"\\"+rootFolderShortName);
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

		assertTrue(fsShort.exists(fsShort.toFile(fLong.toAbsolutePath().toString())), "LocalFileSystem with short path root must accept absolute filename with long path root");
		assertTrue(fsLong.exists(fsLong.toFile(fShort.toAbsolutePath().toString())), "LocalFileSystem with long path root must accept absolute filename with short path root");
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
