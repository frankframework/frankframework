package nl.nn.adapterframework.pipes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;

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

	@Test
	public void cantCalculate() {
		assertThrows(Exception.class, () -> doPipe(pipe, new Message((String) null), session));
	}

	@Test
	public void wrongPathToFile() throws Exception {
		pipe.setInputIsFile(true);
		configureAndStartPipe();
		assertThrows(Exception.class, () -> doPipe(pipe, "dummyPathToFile", session));
	}


	@Test
	public void badCharset() throws Exception {
		pipe.setCharset("dummy");
		configureAndStartPipe();
		assertThrows(PipeRunException.class, () -> doPipe(pipe, "anotherDummy", session));
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
	public void testFileChecksumSHA256() throws Exception {
		String expected = "3820468c2a496ce70b6bb24af2b7601f404d7f5d5141e5e24315b660261a74fa";
		URL file = TestFileUtils.getTestFileURL("/Pipes/2.txt");
		assertEquals(expected, calculateChecksum(file.getPath(), ChecksumType.SHA256, true));
		assertEquals(expected, calculateChecksum(file.openStream(), ChecksumType.SHA256));
	}

	@Test
	public void testFileChecksumSHA512() throws Exception {
		String expected = "5adf3f57356b3aaf1d4023602e13619243644a399c41e2817fb03366d9daeae229f803189754c8004c27f9eafaa33475f41fae0d2d265508f4be3c0185312011";
		URL file = TestFileUtils.getTestFileURL("/Pipes/2.txt");
		assertEquals(expected, calculateChecksum(file.getPath(), ChecksumType.SHA512, true));
		assertEquals(expected, calculateChecksum(file.openStream(), ChecksumType.SHA512));
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
