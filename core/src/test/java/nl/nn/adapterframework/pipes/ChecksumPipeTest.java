package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumGenerator;
import nl.nn.adapterframework.pipes.ChecksumPipe.ChecksumType;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class ChecksumPipeTest extends PipeTestBase<ChecksumPipe> {

	@Override
	public ChecksumPipe createPipe() {
		return new ChecksumPipe();
	}

	@Test
	public void rightChecksumGeneratedMD5() throws Exception {
		pipe.setType(ChecksumType.MD5);
		configureAndStartPipe();
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedSHA() throws Exception {
		pipe.setType(ChecksumType.SHA);
		configureAndStartPipe();
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedCRC32() throws Exception {
		pipe.setType(ChecksumType.CRC32);
		configureAndStartPipe();
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}

	@Test
	public void rightChecksumGeneratedADLER32() throws Exception {
		pipe.setType(ChecksumType.ADLER32);
		ChecksumGenerator result = pipe.createChecksumGenerator();

		assertNotNull(result);
	}
	
	@Test(expected = PipeRunException.class)
	public void cantCalculate() throws Exception {
		doPipe(pipe, new Message((String)null), session);
	}

	@Test(expected = PipeRunException.class)
	public void wrongPathToFile() throws Exception {
		pipe.setInputIsFile(true);
		configureAndStartPipe();
		doPipe(pipe,"dummyPathToFile", session);
	}


	@Test(expected = PipeRunException.class)
	public void badCharset() throws Exception {
		pipe.setCharset("dummy");
		configureAndStartPipe();
		doPipe(pipe,"anotherDummy", session);
	}

	@Test
	public void emptyCharset() throws Exception {
		pipe.setCharset("");
		configureAndStartPipe();
		assertNotNull(doPipe(pipe,"anotherDummy", session));
	}

	@Test
	public void testFileChecksumMD5() throws Exception {
		String expected = "506c38ca885a4cebb13caad6c265f417";
		URL file = TestFileUtils.getTestFileURL("/Pipes/2.txt");
		assertEquals(expected, calculateChecksum(file.getPath(), ChecksumType.MD5, true));
		assertEquals(expected, calculateChecksum(file.openStream(), ChecksumType.MD5));
	}

	@Test
	public void testFileChecksumSHA() throws Exception {
		String expected = "be1f0d458a058e1113baca693b505f5bf26fd01a";
		URL file = TestFileUtils.getTestFileURL("/Pipes/2.txt");
		assertEquals(expected, calculateChecksum(file.getPath(), ChecksumType.SHA, true));
		assertEquals(expected, calculateChecksum(file.openStream(), ChecksumType.SHA));
	}

	@Test
	public void testFileChecksumADLER32() throws Exception {
		String expected = "48773695";
		URL file = TestFileUtils.getTestFileURL("/Unzip/ab.zip");
		assertEquals(expected, calculateChecksum(file.getPath(), ChecksumType.ADLER32, true));
		assertEquals(expected, calculateChecksum(file.openStream(), ChecksumType.ADLER32));
	}

	@Test
	public void testFileChecksumCRC32() throws Exception {
		String expected = "e9065fc7";
		URL file = TestFileUtils.getTestFileURL("/Unzip/ab.zip");
		assertEquals(expected, calculateChecksum(file.getPath(), ChecksumType.CRC32, true));
		assertEquals(expected, calculateChecksum(file.openStream(), ChecksumType.CRC32));
	}

	private String calculateChecksum(Object input, ChecksumType type) throws Exception {
		return calculateChecksum(input, type, false);
	}

	private String calculateChecksum(Object input, ChecksumType type, boolean isFile) throws Exception {
		pipe.setInputIsFile(isFile);
		pipe.setType(type);
		configureAndStartPipe();
		PipeRunResult prr = doPipe(pipe, input, session);
		return prr.getResult().asString();
	}

}
