package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.CompressPipe.FileFormat;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class CompressPipeTest extends PipeTestBase<CompressPipe> {
	private static final String DUMMY_STRING = "dummyString";
	private static final String DUMMY_STRING_SEMI_COLON = DUMMY_STRING + ";";

	@ClassRule
	public static TemporaryFolder tempFolder = new TemporaryFolder();

	@Override
	public CompressPipe createPipe() {
		return new CompressPipe();
	}

	@Test
	public void testUnzippingAndCollectingResult() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setZipEntryPattern("filebb.log");
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		assertTrue(prr.getResult().isBinary());
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testUnzippingAndCollectingResult2String() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setConvert2String(true);
		pipe.setZipEntryPattern("filebb.log");
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		assertFalse(prr.getResult().isBinary());
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testCompressWithPattern() throws Exception {
		// Arrange
		File folder = tempFolder.newFolder();
		pipe.setOutputDirectory(folder.getAbsolutePath());
		pipe.setCompress(true);
		pipe.setFilenamePattern("blaat-{now,date,yyyy}.zip");
		configureAndStartPipe();
		URL input = TestFileUtils.getTestFileURL("/Util/FileUtils/copyFile.txt");
		assertNotNull(input);

		// Act
		PipeRunResult prr = doPipe(input.getPath());

		// Assert
		assertFalse(prr.getResult().isBinary());

		GregorianCalendar cal = new GregorianCalendar();
		String path = prr.getResult().asString();
		String expectedName = "blaat-"+cal.get(Calendar.YEAR)+".zip";
		assertTrue("path ["+path+"] does not end with ["+expectedName+"]", path.endsWith(expectedName));

		try (ZipInputStream zipin = new ZipInputStream(new FileInputStream(path))) {
			ZipEntry entry = zipin.getNextEntry();
			assertNotNull(entry);
			assertEquals("copyFile.txt", entry.getName());
			assertEquals(Message.asString(input), StreamUtil.readerToString(new InputStreamReader(zipin), null));
		}
	}

	@Test
	public void testDecompressingGz() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setConvert2String(true);
		pipe.setFileFormat(FileFormat.GZ);
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.tar.gz"));
		assertTrue(prr.getResult().asString().startsWith("fileaa.txt"));
	}

	@Test
	public void testExceptionForward() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.registerForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME, "dummy"));

		configureAndStartPipe();
		PipeRunResult prr = doPipe(pipe, DUMMY_STRING_SEMI_COLON, session);

		assertEquals(PipeForward.EXCEPTION_FORWARD_NAME, prr.getPipeForward().getName());
	}
	@Test
	public void testBothMessageAndResultIsContentWithNonRepeatableMessage() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setZipEntryPattern("filebb.log");
		pipe.setFileFormat(FileFormat.ZIP);

		configureAndStartPipe();
		PipeRunResult prr = doPipe(MessageTestUtils.getBinaryMessage("/Unzip/ab.zip", false));

		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testMessageIsNotContent() throws Exception {
		String outputDir = tempFolder.getRoot().getPath();
		pipe.setFilenamePattern("file.txt");
		pipe.setZipEntryPattern("fileaa.txt");
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setOutputDirectory(outputDir);
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		String result = StreamUtil.streamToString(new FileInputStream(prr.getResult().asString()), null, null);
		assertEquals("aaa", result);
	}

	@Test
	public void testMessageIsNotContentAndResultString() throws Exception {
		String outputDir = tempFolder.getRoot().getPath();
		pipe.setFilenamePattern("file.txt");
		pipe.setZipEntryPattern("fileaa.txt");
		pipe.setConvert2String(true);
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setOutputDirectory(outputDir);
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		String result = StreamUtil.streamToString(new FileInputStream(prr.getResult().asString()), null, null);
		assertEquals("aaa", result);
	}

	@Test
	public void testZipMultipleFiles() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setCompress(true);
		configureAndStartPipe();
		String input=TestFileUtils.getTestFilePath("/Util/FileUtils/copyFile.txt")+";"+TestFileUtils.getTestFilePath("/Util/FileUtils/copyFrom.txt");
		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());

		try (ZipInputStream zipin = new ZipInputStream(prr.getResult().asInputStream())) {
			ZipEntry entry = zipin.getNextEntry();
			assertNotNull(entry);
			assertEquals("copyFile.txt", entry.getName());

			URL url = TestFileUtils.getTestFileURL("/Util/FileUtils/"+entry.getName());
			assertNotNull(url);
			assertEquals(Message.asString(url), StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));

			entry = zipin.getNextEntry();
			assertNotNull(entry);
			assertEquals("copyFrom.txt", entry.getName());

			URL url2 = TestFileUtils.getTestFileURL("/Util/FileUtils/"+entry.getName());
			assertNotNull(url2);
			assertEquals(Message.asString(url2), StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
		}
	}

	@Test
	public void testGetterSetterMessageIsContent() {
		pipe.setMessageIsContent(true);
		boolean checkBoolean = pipe.isMessageIsContent();
		assertEquals(true, checkBoolean);

		pipe.setMessageIsContent(false);
		checkBoolean = pipe.isMessageIsContent();
		assertEquals(false, checkBoolean);
	}

	@Test
	public void testGetterSetterResultIsContent() {
		pipe.setResultIsContent(true);
		boolean checkBoolean = pipe.isResultIsContent();
		assertEquals(true, checkBoolean);

		pipe.setResultIsContent(false);
		checkBoolean = pipe.isResultIsContent();
		assertEquals(false, checkBoolean);
	}

	@Test
	public void testGetterSetterOutputDirectory() {
		pipe.setOutputDirectory(DUMMY_STRING);
		String otherString = pipe.getOutputDirectory();
		assertEquals(DUMMY_STRING, otherString);
	}

	@Test
	public void testGetterSetterFilenamePattern() {
		pipe.setFilenamePattern(DUMMY_STRING);
		String otherString = pipe.getFilenamePattern();
		assertEquals(DUMMY_STRING, otherString);
	}

	@Test
	public void testGetterSetterZipEntryPattern() {
		pipe.setZipEntryPattern(DUMMY_STRING);
		String otherString = pipe.getZipEntryPattern();
		assertEquals(DUMMY_STRING, otherString);
	}

	@Test
	public void testGetterSetterCompress() {
		pipe.setCompress(true);
		boolean checkBoolean = pipe.isCompress();
		assertEquals(true, checkBoolean);

		pipe.setCompress(false);
		checkBoolean = pipe.isCompress();
		assertEquals(false, checkBoolean);
	}

	@Test
	public void testGetterSetterConvert2String() {
		pipe.setConvert2String(true);
		boolean checkBoolean = pipe.isConvert2String();
		assertEquals(true, checkBoolean);

		pipe.setConvert2String(false);
		checkBoolean = pipe.isConvert2String();
		assertEquals(false, checkBoolean);
	}

	@Test(expected = ConfigurationException.class)
	public void testCaptureFakeFilePath() throws Exception {
		pipe.setMessageIsContent(false);
		pipe.setCompress(true);

		configureAndStartPipe();
		doPipe(pipe, DUMMY_STRING_SEMI_COLON, session);
	}

	@Test(expected = ConfigurationException.class)
	public void testCaptureUncompressedLegitimateFilePath() throws Exception {
		pipe.setMessageIsContent(false);
		pipe.setCompress(false);
		pipe.setFileFormat(FileFormat.GZ);

		configureAndStartPipe();
		doPipe(pipe, DUMMY_STRING_SEMI_COLON, session);
	}

	@Test
	public void testResultIsContent() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setFileFormat(FileFormat.GZ);

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		GZIPOutputStream gouz = new GZIPOutputStream(bout);
		gouz.write(DUMMY_STRING_SEMI_COLON.getBytes());
		gouz.close();

		configureAndStartPipe();
		final PipeRunResult result = doPipe(pipe, bout.toByteArray(), session);

		final String message = result.getResult().asString();

		assertEquals(DUMMY_STRING_SEMI_COLON, message);
	}
	@Test
	public void testResultIsContentIncorrectFormat() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);

		configureAndStartPipe();
		final PipeRunException pipeRunException = assertThrows(PipeRunException.class, () -> doPipe(pipe, DUMMY_STRING_SEMI_COLON, session));

		assertEquals(ZipException.class, pipeRunException.getCause().getClass());
		assertTrue(pipeRunException.getMessage().endsWith("Not in GZIP format"));
	}

	@Test
	public void testCompressWithLegitimateFileFormat() throws Exception {
		pipe.setFileFormat(FileFormat.GZ);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(true);

		configureAndStartPipe();
		final PipeRunResult pipeRunResult = doPipe(pipe, DUMMY_STRING_SEMI_COLON, session);
		assertNotNull(pipeRunResult);

		GZIPInputStream giz = new GZIPInputStream(pipeRunResult.getResult().asInputStream());
		BufferedReader bur = new BufferedReader(new InputStreamReader(giz));

		String result = bur.readLine();
		bur.close();

		assertEquals(DUMMY_STRING_SEMI_COLON, result);
	}

	@Test
	public void testCompressWithIllegitimateFileFormat() throws Exception {
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, DUMMY_STRING_SEMI_COLON, session)); // TODO should assert proper return value
	}

	@Test
	public void testUncompressedWithIlligimitateFileFormat() throws Exception {
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(false);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, DUMMY_STRING_SEMI_COLON, session)); // TODO should assert proper return value
	}

	@Test
	public void testConvertByteToStringForResultMsg() throws Exception {
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(false);
		pipe.setConvert2String(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, DUMMY_STRING_SEMI_COLON, session)); // TODO should assert proper return value
	}

	@Test(expected = ConfigurationException.class)
	public void testCaptureIllegitimateFilePath() throws Exception {
		pipe.setMessageIsContent(false);
		pipe.setCompress(true);

		configureAndStartPipe();
		doPipe(pipe, DUMMY_STRING_SEMI_COLON, session);
	}

	@Test(expected = PipeRunException.class)
	public void testCaptureIllegitimateByteArray() throws Exception {
		Object input = DUMMY_STRING.getBytes();
		pipe.setMessageIsContent(true);

		configureAndStartPipe();
		doPipe(pipe, input, session);
	}

	@Test(expected = PipeRunException.class)
	public void testCaptureUnconvertableArray() throws Exception {
		Object input = DUMMY_STRING;
		pipe.setMessageIsContent(true);

		configureAndStartPipe();
		doPipe(pipe, input, session);
	}
}
