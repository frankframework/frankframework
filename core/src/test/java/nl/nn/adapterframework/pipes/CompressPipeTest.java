package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeRunException;

public class CompressPipeTest extends PipeTestBase<CompressPipe> {
	private String dummyString = "dummyString";
	private String dummyStringSemiColon = dummyString + ";";

	@Override
	public CompressPipe createPipe() {
		return new CompressPipe();
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
		pipe.setFileFormat("gz");

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
		pipe.setFileFormat("gz");
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session));
	}

	@Test
	public void testCompressWithIllegimitateFileFormat() throws Exception {
		pipe.setFileFormat("notNull");
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(true);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session)); // TODO should assert proper return value
	}

	@Test
	public void testUncompressedWithIlligimitateFileFormat() throws Exception {
		pipe.setFileFormat("notNull");
		pipe.setMessageIsContent(true);
		pipe.setResultIsContent(true);
		pipe.setCompress(false);

		configureAndStartPipe();
		assertNotNull(doPipe(pipe, dummyStringSemiColon, session)); // TODO should assert proper return value
	}

	@Test
	public void testConvertByteToStringForResultMsg() throws Exception {
		pipe.setFileFormat("notNull");
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