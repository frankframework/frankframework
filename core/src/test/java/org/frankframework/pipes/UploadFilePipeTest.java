package org.frankframework.pipes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipInputStream;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class UploadFilePipeTest extends PipeTestBase<UploadFilePipe> {

	private ZipInputStream zis;

	@TempDir
	public static Path testFolderSource;

	private static String sourceFolderPath;

	@Override
	public UploadFilePipe createPipe() {
		return new UploadFilePipe();
	}

	@BeforeAll
	public static void beforeClass() throws Exception {
		sourceFolderPath = testFolderSource.toString();
		Files.createFile(testFolderSource.resolve("1.txt"));
		Files.createFile(testFolderSource.resolve("1.zip"));
	}

	@BeforeEach
	public void beforeEach() throws Exception {
		FileInputStream fis = new FileInputStream(sourceFolderPath + "/1.zip");
		BufferedInputStream bis = new BufferedInputStream(fis);
		zis = new ZipInputStream(bis);
	}

	@AfterEach
	public void teardown() throws IOException {
		if (zis != null) {
			zis.close();
		}
	}

	/**
	 * Method: configure()
	 */
	@Test
	public void testNullSessionKey() throws Exception {
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "das", session));
		assertThat(e.getMessage(), Matchers.endsWith("got null value from session under key [file]"));
	}

	@Test
	public void testNullInputStream() throws Exception {
		pipe.setSessionKey("fdsf123");
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "das", session));
		assertThat(e.getMessage(), Matchers.containsString("got null value from session under key [fdsf123]"));
	}

	/**
	 * Method: doPipe(Object input, PipeLineSession session)
	 */
	@Test
	public void testDoPipeSuccess() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectory(sourceFolderPath);
		session.put("key", zis);
		session.put("fileName", "1.zip");
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, "dsfdf", session);
		assertEquals(sourceFolderPath, res.getResult().asString());
	}

	/**
	 * Method: doPipe(Object input, PipeLineSession session)
	 */
	@Test
	public void testDoPipeFailWrongExtension() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectory(sourceFolderPath);
		session.put("key", zis);
		session.put("fileName", "1.txt");
		configureAndStartPipe();

		PipeRunException e = assertThrows(PipeRunException.class, () -> doPipe(pipe, "dsfdf", session));
		assertThat(e.getMessage(), Matchers.containsString("file extension [txt] should be 'zip'"));
	}

	/**
	 * Method: doPipe(Object input, PipeLineSession session)
	 */
	@Test
	public void testDoPipeSuccessWithDirectorySessionKey() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectorySessionKey("key2");
		session.put("key", zis);
		session.put("fileName", "1.zip");
		session.put("key2", sourceFolderPath);
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, "dsfdf", session);
		assertEquals(sourceFolderPath, res.getResult().asString());
	}

	/**
	 * Method: doPipe(Object input, PipeLineSession session)
	 */
	@Test
	public void testDoPipeSuccessWithoutDirectory() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectorySessionKey("");
		session.put("key", zis);
		session.put("fileName", "1.zip");
		File parent = new File(sourceFolderPath);
		File zipFile = new File(parent, "hoooray.zip");
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, zipFile.getAbsolutePath(), session);
		assertEquals(zipFile.getPath(), res.getResult().asString());
	}

	/**
	 * Method: doPipe(Object input, PipeLineSession session)
	 */
	@Test
	public void testDoPipeCreateNonExistingDirectory() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectorySessionKey("key2");
		session.put("key", zis);
		session.put("fileName", "1.zip");
		session.put("key2", sourceFolderPath + "/new_dir");
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, "dsfdf", session);
		assertEquals(sourceFolderPath + File.separator + "new_dir", res.getResult().asString());
	}
}
