package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;

/**
 * UploadFilePipe Tester.
 *
 * @author <Sina Sen>
 * 
 */
public class UploadFilePipeTest extends PipeTestBase<UploadFilePipe> {

	private static ZipInputStream zis;
	@ClassRule
	public static TemporaryFolder testFolderSource = new TemporaryFolder();

	private static String sourceFolderPath;

	private static File newFile;
	private static File newFile2;

	@Override
	public UploadFilePipe createPipe() {
		return new UploadFilePipe();
	}

	@BeforeClass
	public static void beforeClass() throws Exception {
		sourceFolderPath = testFolderSource.getRoot().getPath();
		newFile = testFolderSource.newFile("1.txt");
		newFile2 = testFolderSource.newFile("1.zip");
		assert newFile.exists();
		assert newFile2.exists();
	}

	@Before
	public void before() throws Exception {

		FileInputStream fis = new FileInputStream(sourceFolderPath + "/1.zip");
		BufferedInputStream bis = new BufferedInputStream(fis);
		zis = new ZipInputStream(bis);

	}

	/**
	 * Method: configure()
	 */
	@Test
	public void testNullSessionKey() throws Exception {
		exception.expect(PipeRunException.class);
		configureAndStartPipe();
		doPipe(pipe, "das", session);
		fail("this is expected to fail");

	}

	@Test
	public void testNullInputStream() throws Exception {
		// exception.expect(PipeRunException.class);
		exception.expectMessage("Pipe [UploadFilePipe under test] msgId [null] got null value from session under key [fdsf123]");
		pipe.setSessionKey("fdsf123");
		configureAndStartPipe();
		doPipe(pipe, "das", session);
		fail("this is expected to fail");

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
		exception.expect(PipeRunException.class);
		exception.expectMessage("Pipe [UploadFilePipe under test] msgId [null] file extension [txt] should be 'zip'");
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectory(sourceFolderPath);
		session.put("key", zis);
		session.put("fileName", "1.txt");
		configureAndStartPipe();
		doPipe(pipe, "dsfdf", session);
		fail("this is expected to fail");

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
