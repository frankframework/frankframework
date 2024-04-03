package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.PipeStartException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class MoveFilePipeTest extends PipeTestBase<MoveFilePipe> {

	private final String pipeForwardThen = "success";

	@TempDir
	public static Path testFolderSource;

	private static String sourceFolderPath;

	@TempDir
	public static Path testFolderDest;

	private static String destFolderPath;

	@TempDir
	public static Path testFolderCantDelete;

	private static String cantdeleteFolderPath;

	@TempDir
	public static Path testFolderDelete;

	private static String deleteFolderPath;

	@Override
	public MoveFilePipe createPipe() {
		return new MoveFilePipe();
	}

	@BeforeAll
	public static void setUpTest() throws IOException {
		sourceFolderPath = testFolderSource.toString();
		destFolderPath = testFolderDest.toString();
		cantdeleteFolderPath = testFolderCantDelete.toString();
		deleteFolderPath = testFolderDelete.toString();

		Files.createFile(testFolderSource.resolve("1.txt"));
		Files.createFile(testFolderSource.resolve("2.txt"));
		Files.createFile(testFolderSource.resolve("3.txt"));
		Files.createFile(testFolderSource.resolve("a.md"));
		Files.createFile(testFolderSource.resolve("b.md"));
		Files.createFile(testFolderSource.resolve("sad.lk"));
		Files.createFile(testFolderSource.resolve("notCompatible.txt"));
		Files.createFile(testFolderSource.resolve("createDirectory.txt"));
		Files.createFile(testFolderSource.resolve("cantmove.sc"));
		Files.createFile(testFolderSource.resolve("prefixsuffix.txt"));
		Files.createFile(testFolderSource.resolve("toAppend1.txt"));
		Files.createFile(testFolderSource.resolve("toAppend2.txt"));
		Files.createFile(testFolderSource.resolve("xx.txt"));
		Files.createFile(testFolderSource.resolve("test.txt"));
		Files.createFile(testFolderDest.resolve("cantmove.sc"));
		Files.createFile(testFolderDest.resolve("notcompatible.asd"));
		Files.createFile(testFolderDest.resolve("toBeAppended.txt"));
		Files.createFile(testFolderCantDelete.resolve("deletable.sd"));
		Files.createFile(testFolderCantDelete.resolve("deletionInterrupter.mz"));
		Files.createFile(testFolderDelete.resolve("moveAndDeleteDirectory.txt"));
	}

	@Test
	public void nonExistingFileWithSourceAndTargetDirectories() throws ConfigurationException, PipeStartException, PipeRunException {

		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename(null);
		pipe.configure();
		pipe.start();

		PipeRunResult res = doPipe(pipe, "xx.txt", session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());

	}

	@Test
	public void fileToFolderTransferWithoutWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("test.txt");
		pipe.setNumberOfBackups(0);
		pipe.configure();
		pipe.start();

		PipeRunResult res = doPipe(pipe, "xdfgfx", session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void appendFileToFileInAnotherDirectoryWithoutWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("toAppend1.txt");
		pipe.setMove2file("toBeAppended.txt");
		pipe.setAppend(true);
		pipe.configure();
		pipe.start();

		PipeRunResult res = pipe.doPipe(null, session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void appendFileToFileWithSessionKey() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("toAppend2.txt");
		pipe.setMove2fileSessionKey("a");
		session.put("a", "toBeAppended.txt");
		pipe.setAppend(true);
		pipe.configure();
		pipe.start();

		PipeRunResult res = pipe.doPipe(null, session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void moveFileAndDeleteDirectory() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(deleteFolderPath);
		pipe.setDeleteEmptyDirectory(true);
		pipe.setFilename("moveAndDeleteDirectory.txt");
		pipe.configure();
		pipe.start();

		PipeRunResult res = doPipe(pipe, "xx", session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void moveToNewlyCreatedDirectory() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setCreateDirectory(true);
		pipe.setMove2dir(destFolderPath + "/new");// for MAC, different for Windows
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("createDirectory.txt");
		pipe.configure();
		pipe.start();

		PipeRunResult res = doPipe(pipe, "xx", session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void moveFilesWithWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setWildcard("*.md");
		pipe.configure();
		pipe.start();

		PipeRunResult res = pipe.doPipe(null, session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void moveWithPrefixAndSuffixChange() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("prefixsuffix.txt");
		pipe.setSuffix(".md");
		pipe.setPrefix("1");
		pipe.configure();
		pipe.start();

		PipeRunResult res = doPipe(pipe, "xx", session);
		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void testThrowException() throws ConfigurationException, PipeStartException {
		pipe.setThrowException(true);
		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void moveFilesWithWildcardSessionKeyTest() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setWildcardSessionKey("a");
		session.put("a", "*.txt");
		pipe.configure();
		pipe.start();
		PipeRunResult res = doPipe(pipe, "sd", session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void nonExistingFileWithEverythingNull() throws ConfigurationException, PipeStartException, PipeRunException {
		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void everythingNull() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setFilename(null);
		assertThrows(ConfigurationException.class, pipe::configure);
	}

	@Test
	public void cantMoveAsItAlreadyExists() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setMove2file("cantmove.sc");
		pipe.setFilename("cantmove.sc");
		pipe.setNumberOfBackups(0);
		pipe.setThrowException(true);
		pipe.configure();
		pipe.start();

		assertThrows(PipeRunException.class, ()-> pipe.doPipe(null, session));
	}

	@Test
	public void cantMoveFileAsItsDirectoryIsFalse() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath + "/itswrong");
		pipe.setFilename("cantmove.sc");
		pipe.setNumberOfAttempts(1);
		pipe.configure();
		pipe.start();

		assertThrows(PipeRunException.class, ()-> doPipe(pipe, "xdfgfx", session));
	}

	@Test
	public void appendFilesNotCompatible() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("notCompatible.txt");
		pipe.setMove2file("notCompatible.asd");
		pipe.setOverwrite(false);
		pipe.setAppend(true);
		pipe.configure();
		pipe.start();

		PipeRunResult res = pipe.doPipe(null, session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void cantMoveFilesWithWildcardTest() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory(sourceFolderPath);
		pipe.setWildcard("*.xd");
		pipe.configure();
		pipe.start();
		PipeRunResult res = pipe.doPipe(null, session);
		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void cantDeleteDirectoryAsWrongName() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);
		pipe.setDirectory("/Users/apollo11/Desktop/iaf/core/src/test/java/cantbedeleteddd");// some random, wrong directory path
		pipe.setDeleteEmptyDirectory(true);
		pipe.setFilename("deletable.sd");
		pipe.configure();
		pipe.setNumberOfAttempts(1);
		pipe.start();

		assertThrows(PipeRunException.class, ()-> doPipe(pipe, "xx", session));
	}

	@Test
	public void cantDeleteDirectoryAsItIsNotEmpty() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setMove2dir(destFolderPath);// for MAC, different for Windows
		pipe.setDirectory(cantdeleteFolderPath);// for MAC, different for Windows
		pipe.setDeleteEmptyDirectory(true);
		pipe.setFilename("deletable.sd");
		pipe.configure();
		pipe.start();

		PipeRunResult res = doPipe(pipe, "xx", session);

		assertEquals(pipeForwardThen, res.getPipeForward().getName());
	}

	@Test
	public void failCreatingNewDirectory() throws ConfigurationException, PipeStartException, PipeRunException {
		pipe.setCreateDirectory(false);
		pipe.setMove2dir(destFolderPath + "/newas");
		pipe.setDirectory(sourceFolderPath);
		pipe.setFilename("sad.lk");
		pipe.setNumberOfAttempts(1);
		pipe.configure();
		pipe.start();

		assertThrows(PipeRunException.class, ()-> doPipe(pipe, "xx", session));
	}
}
