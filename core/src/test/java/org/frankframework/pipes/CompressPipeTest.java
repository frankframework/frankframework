package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.Parameter;
import org.frankframework.pipes.CompressPipe.FileFormat;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.StreamUtil;

public class CompressPipeTest extends PipeTestBase<CompressPipe> {
	private static final String DUMMY_STRING = "dummyString";
	private static final String DUMMY_STRING_SEMI_COLON = DUMMY_STRING + ";";

	@TempDir
	public Path tempFolder;

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
	public void testUnzippingAndCollectingResultWithPatternFromSession() throws Exception {
		pipe.setResultIsContent(true);
		session.put("file", "filebb");
		session.put("ext", "log");
		pipe.setZipEntryPattern("{file}.{ext}");
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		assertTrue(prr.getResult().isBinary());
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testUnzippingAndCollectingResultWithPattermFromParameter() throws Exception {
		pipe.setResultIsContent(true);
		pipe.addParameter(new Parameter("zipEntryPattern", "filebb.log"));
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		assertTrue(prr.getResult().isBinary());
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testUnzippingAndCollectingResult2String() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setZipEntryPattern("filebb.log");
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.zip"));
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testCompressWithPattern() throws Exception {
		// Arrange
		String outputDir = tempFolder.toString();
		pipe.setOutputDirectory(outputDir);
		pipe.setCompress(true);
		pipe.setFilenamePattern("blaat-{now,date,yyyy}.zip");
		configureAndStartPipe();
		String file = "/Pipes/CompressPipe/copyFile.txt";
		String input = TestFileUtils.getTestFilePath(file);
		assertNotNull(input);

		// Act
		PipeRunResult prr = doPipe(input);

		// Assert
		assertFalse(prr.getResult().isBinary());

		GregorianCalendar cal = new GregorianCalendar();
		String path = prr.getResult().asString();
		String expectedName = "blaat-" + cal.get(Calendar.YEAR) + ".zip";
		assertTrue(path.endsWith(expectedName), "path [" + path + "] does not end with [" + expectedName + "]");

		try (ZipInputStream zipin = new ZipInputStream(new FileInputStream(path))) {
			ZipEntry entry = zipin.getNextEntry();
			assertNotNull(entry);
			assertEquals("copyFile.txt", entry.getName());
			assertEquals(TestFileUtils.getTestFile(file), StreamUtil.readerToString(new InputStreamReader(zipin), null));
		}
	}

	@Test
	public void testDecompressingGz() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setFileFormat(FileFormat.GZ);
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFilePath("/Unzip/ab.tar.gz"));
		assertTrue(prr.getResult().asString().startsWith("fileaa.txt"));
	}

	@Test
	public void testExceptionForward() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.addForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME, "dummy"));

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
		String outputDir = tempFolder.toString();
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
		String outputDir = tempFolder.toString();
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
	public void testZipMultipleFiles() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setCompress(true);
		configureAndStartPipe();
		String input = TestFileUtils.getTestFilePath("/Pipes/CompressPipe/copyFile.txt") + ";" + TestFileUtils.getTestFilePath("/Pipes/CompressPipe/copyFrom.txt");
		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());

		try (prr; ZipInputStream zipin = new ZipInputStream(prr.getResult().asInputStream())) {
			ZipEntry entry = zipin.getNextEntry();
			assertNotNull(entry);
			assertEquals("copyFile.txt", entry.getName());

			URL url = TestFileUtils.getTestFileURL("/Pipes/CompressPipe/" + entry.getName());
			assertNotNull(url);
			UrlMessage urlMessage = new UrlMessage(url);
			assertEquals(urlMessage.asString(), StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
			urlMessage.close();

			entry = zipin.getNextEntry();
			assertNotNull(entry);
			assertEquals("copyFrom.txt", entry.getName());

			URL url2 = TestFileUtils.getTestFileURL("/Pipes/CompressPipe/" + entry.getName());
			assertNotNull(url2);
			UrlMessage urlMessage2 = new UrlMessage(url2);
			assertEquals(urlMessage2.asString(), StreamUtil.readerToString(StreamUtil.dontClose(new InputStreamReader(zipin)), null));
			urlMessage2.close();
		}
	}

	@Test
	public void testGetterSetterMessageIsContent() {
		pipe.setMessageIsContent(true);
		boolean checkBoolean = pipe.isMessageIsContent();
		assertTrue(checkBoolean);

		pipe.setMessageIsContent(false);
		checkBoolean = pipe.isMessageIsContent();
		assertFalse(checkBoolean);
	}

	@Test
	public void testGetterSetterResultIsContent() {
		assertFalse(pipe.isResultIsContent()); //Default NULL

		pipe.setResultIsContent(true);
		assertTrue(pipe.isResultIsContent());

		pipe.setResultIsContent(false);
		assertFalse(pipe.isResultIsContent());
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
		assertTrue(checkBoolean);

		pipe.setCompress(false);
		checkBoolean = pipe.isCompress();
		assertFalse(checkBoolean);
	}

	@Test
	public void testCaptureFakeFilePath() {
		pipe.setResultIsContent(false);
		pipe.setCompress(true);

		assertThrows(ConfigurationException.class, this::configureAndStartPipe);
	}

	@Test
	public void testCaptureUncompressedLegitimateFilePath() {
		pipe.setResultIsContent(false);
		pipe.setCompress(false);
		pipe.setFileFormat(FileFormat.GZ);

		assertThrows(ConfigurationException.class, this::configureAndStartPipe);
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

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, DUMMY_STRING_SEMI_COLON, session)); // TODO should assert proper return value
	}

	@Test
	public void testCaptureIllegitimateFilePath() {
		pipe.setResultIsContent(false);
		pipe.setCompress(true);

		assertThrows(ConfigurationException.class, this::configureAndStartPipe);
	}

	@Test
	public void testCaptureIllegitimateByteArray() throws Exception {
		pipe.setMessageIsContent(true);

		configureAndStartPipe();
		assertThrows(PipeRunException.class, () -> doPipe(new Message(DUMMY_STRING.getBytes())));
	}

	@Test
	public void testCaptureInconvertibleArray() throws Exception {
		pipe.setMessageIsContent(true);

		configureAndStartPipe();
		assertThrows(PipeRunException.class, () -> doPipe(DUMMY_STRING));
	}
}
