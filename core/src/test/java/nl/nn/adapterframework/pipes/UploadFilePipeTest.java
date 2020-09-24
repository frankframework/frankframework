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
import org.mockito.Mock;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
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

	@Mock
	private IPipeLineSession session1 = new PipeLineSessionBase();

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
		doPipe(pipe, "das", session1);
		fail("this is expected to fail");

	}

	/**
	 * Method: doPipe(Object input, IPipeLineSession session)
	 */
	@Test
	public void testDoPipeWrongInputFormat() throws Exception {
		exception.expect(ClassCastException.class);
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectory(sourceFolderPath);
		session1.put("key", "32434");
		configureAndStartPipe();
		doPipe(pipe, "dsfdfs", session1);
		fail("this is expected to fail");

	}

	/**
	 * Method: doPipe(Object input, IPipeLineSession session)
	 */
	@Test
	public void testDoPipeSuccess() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectory(sourceFolderPath);
		session1.put("key", zis);
		session1.put("fileName", "1.zip");
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, "dsfdf", session1);
		assertEquals(sourceFolderPath, res.getResult().asString());
	}

	/**
	 * Method: doPipe(Object input, IPipeLineSession session)
	 */
	@Test
	public void testDoPipeFailWrongExtension() throws Exception {
		exception.expect(PipeRunException.class);
		exception.expectMessage("Pipe [UploadFilePipe under test] msgId [null] file extension [txt] should be 'zip'");
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectory(sourceFolderPath);
		session1.put("key", zis);
		session1.put("fileName", "1.txt");
		configureAndStartPipe();
		doPipe(pipe, "dsfdf", session1);
		fail("this is expected to fail");

	}

	/**
	 * Method: doPipe(Object input, IPipeLineSession session)
	 */
	@Test
	public void testDoPipeSuccessWithDirectorySessionKey() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectorySessionKey("key2");
		session1.put("key", zis);
		session1.put("fileName", "1.zip");
		session1.put("key2", sourceFolderPath);
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, "dsfdf", session1);
		assertEquals(sourceFolderPath, res.getResult().asString());
	}

	/**
	 * Method: doPipe(Object input, IPipeLineSession session)
	 */
	@Test
	public void testDoPipeSuccessWithoutDirectory() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectorySessionKey("");
		session1.put("key", zis);
		session1.put("fileName", "1.zip");
		File parent = new File(sourceFolderPath);
		File zipFile = new File(parent, "hoooray.zip");
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, zipFile.getAbsolutePath(), session1);
		assertEquals(zipFile.getPath(), res.getResult().asString());
	}

	/**
	 * Method: doPipe(Object input, IPipeLineSession session)
	 */
	@Test
	public void testDoPipeCreateNonExistingDirectory() throws Exception {
		String key = "key";
		pipe.setSessionKey(key);
		pipe.setDirectorySessionKey("key2");
		session1.put("key", zis);
		session1.put("fileName", "1.zip");
		session1.put("key2", sourceFolderPath + "/new_dir");
		configureAndStartPipe();
		PipeRunResult res = doPipe(pipe, "dsfdf", session1);
		assertEquals(sourceFolderPath + File.separator + "new_dir", res.getResult().asString());
	}
}
