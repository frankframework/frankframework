package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.CompressPipe.FileFormat;
import nl.nn.adapterframework.testutil.MessageTestUtils;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.StreamUtil;

public class CompressPipeTest extends PipeTestBase<CompressPipe> {
	private String dummyString = "dummyString";
	private String dummyStringSemiColon = dummyString + ";";

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
		PipeRunResult prr = doPipe(TestFileUtils.getTestFileURL("/Unzip/ab.zip").getPath());
		assertTrue(prr.getResult().isBinary());
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testUnzippingAndCollectingResult2String() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setConvert2String(true);
		pipe.setZipEntryPattern("filebb.log");
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFileURL("/Unzip/ab.zip").getPath());
		assertFalse(prr.getResult().isBinary());
		assertEquals("bbb", prr.getResult().asString());
	}

	@Test
	public void testDecompressingGz() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setConvert2String(true);
		pipe.setFileFormat(FileFormat.GZ);
		configureAndStartPipe();
		PipeRunResult prr = doPipe(TestFileUtils.getTestFileURL("/Unzip/ab.tar.gz").getPath());
		assertTrue(prr.getResult().asString().startsWith("fileaa.txt"));
	}

	@Test
	public void testExceptionForward() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.registerForward(new PipeForward(PipeForward.EXCEPTION_FORWARD_NAME, "dummy"));

		configureAndStartPipe();
		PipeRunResult prr = doPipe(pipe, dummyStringSemiColon, session);

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
		PipeRunResult prr = doPipe(TestFileUtils.getTestFileURL("/Unzip/ab.zip").getPath());
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
		PipeRunResult prr = doPipe(TestFileUtils.getTestFileURL("/Unzip/ab.zip").getPath());
		String result = StreamUtil.streamToString(new FileInputStream(prr.getResult().asString()), null, null);
		assertEquals("aaa", result);
	}

	@Test
	public void testZipMultipleFiles() throws Exception {
		pipe.setResultIsContent(true);
		pipe.setCompress(true);
		configureAndStartPipe();
		String input=TestFileUtils.getTestFileURL("/Util/FileUtils/copyFile.txt").getPath()+";"+TestFileUtils.getTestFileURL("/Util/FileUtils/copyFrom.txt").getPath();
		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());
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
	public void testGetterSetterOuputDirectory() {
		pipe.setOutputDirectory(dummyString);
		String otherString = pipe.getOutputDirectory();
		assertEquals(dummyString, otherString);
	}

	@Test
	public void testGetterSetterFilenamePattern() {
		pipe.setFilenamePattern(dummyString);
		String otherString = pipe.getFilenamePattern();
		assertEquals(dummyString, otherString);
	}

	@Test
	public void testGetterSetterZipEntryPattern() {
		pipe.setZipEntryPattern(dummyString);
		String otherString = pipe.getZipEntryPattern();
		assertEquals(dummyString, otherString);
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
		doPipe(pipe, dummyStringSemiColon, session);
	}

	@Test(expected = ConfigurationException.class)
	public void testCaptureUncompressedLegitimateFilePath() throws Exception {
		pipe.setMessageIsContent(false);
		pipe.setCompress(false);
		pipe.setFileFormat(FileFormat.GZ);

		configureAndStartPipe();
		doPipe(pipe, dummyStringSemiColon, session);
	}

	@Test(expected = PipeRunException.class)
	public void testResultIsContent() throws Exception {
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);

		configureAndStartPipe();
		doPipe(pipe, dummyStringSemiColon, session);
	}

	@Test
	public void testCompressWithLegitimateFileFormat() throws Exception {
		pipe.setFileFormat(FileFormat.GZ);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session));
	}

	@Test
	public void testCompressWithIllegimitateFileFormat() throws Exception {
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session)); // TODO should assert proper return value
	}

	@Test
	public void testUncompressedWithIlligimitateFileFormat() throws Exception {
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(false);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session)); // TODO should assert proper return value
	}

	@Test
	public void testConvertByteToStringForResultMsg() throws Exception {
		pipe.setFileFormat(FileFormat.ZIP);
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(false);
		pipe.setConvert2String(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session)); // TODO should assert proper return value
	}

	@Test(expected = ConfigurationException.class)
	public void testCaptureIllegitimateFilePath() throws Exception {
		pipe.setMessageIsContent(false);
		pipe.setCompress(true);

		configureAndStartPipe();
		doPipe(pipe, dummyStringSemiColon, session);
	}

	@Test(expected = PipeRunException.class)
	public void testCaptureIllegitimateByteArray() throws Exception {
		Object input = dummyString.getBytes();
		pipe.setMessageIsContent(true);

		configureAndStartPipe();
		doPipe(pipe, input, session);
	}

	@Test(expected = PipeRunException.class)
	public void testCaptureUnconvertableArray() throws Exception {
		Object input = dummyString;
		pipe.setMessageIsContent(true);

		configureAndStartPipe();
		doPipe(pipe, input, session);
	}
}
