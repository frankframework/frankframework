package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.hash.Algorithm;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;


public class ChecksumPipeTest extends PipeTestBase<ChecksumPipe> {

	@Override
	public ChecksumPipe createPipe() {
		return new ChecksumPipe();
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

	public static Stream<Arguments> fileValues() {
		return Stream.of(
				Arguments.of(Algorithm.MD5, "/Pipes/2.txt", "506c38ca885a4cebb13caad6c265f417", "UGw4yohaTOuxPKrWwmX0Fw=="),
				Arguments.of(Algorithm.SHA, "/Pipes/2.txt", "be1f0d458a058e1113baca693b505f5bf26fd01a", "vh8NRYoFjhETusppO1BfW/Jv0Bo="),
				Arguments.of(Algorithm.SHA256, "/Pipes/2.txt", "3820468c2a496ce70b6bb24af2b7601f404d7f5d5141e5e24315b660261a74fa", "OCBGjCpJbOcLa7JK8rdgH0BNf11RQeXiQxW2YCYadPo="),
				Arguments.of(Algorithm.SHA512, "/Pipes/2.txt", "5adf3f57356b3aaf1d4023602e13619243644a399c41e2817fb03366d9daeae229f803189754c8004c27f9eafaa33475f41fae0d2d265508f4be3c0185312011", "Wt8/VzVrOq8dQCNgLhNhkkNkSjmcQeKBf7AzZtna6uIp+AMYl1TIAEwn+er6ozR19B+uDS0mVQj0vjwBhTEgEQ=="),
				Arguments.of(Algorithm.CRC32, "/Unzip/ab.zip", "e9065fc7", "AOkGX8c="),
				Arguments.of(Algorithm.ADLER32, "/Unzip/ab.zip", "48773695", "SHc2lQ==")
		);
	}

	@MethodSource("fileValues")
	@ParameterizedTest
	public void testChecksumAndForFiles(Algorithm algorithm, String fileUrl, String expectedHex, String expectedBase64) throws Exception {
		URL file = TestFileUtils.getTestFileURL(fileUrl);
		assertEquals(expectedHex, calculateChecksum(file.getPath(), algorithm, true, HashPipe.HashEncoding.Hex));
		assertEquals(expectedHex, calculateChecksum(file.openStream(), algorithm, HashPipe.HashEncoding.Hex));

		assertEquals(expectedBase64, calculateChecksum(file.getPath(), algorithm, true, HashPipe.HashEncoding.Base64));
		assertEquals(expectedBase64, calculateChecksum(file.openStream(), algorithm, HashPipe.HashEncoding.Base64));
	}

	private String calculateChecksum(Object input, Algorithm type, HashPipe.HashEncoding hashEncoding) throws Exception {
		return calculateChecksum(input, type, false, hashEncoding);
	}

	private String calculateChecksum(Object input, Algorithm type, boolean isFile, HashPipe.HashEncoding hashEncoding) throws Exception {
		pipe.setInputIsFile(isFile);
		pipe.setAlgorithm(type);
		pipe.setHashEncoding(hashEncoding);
		configureAndStartPipe();

		PipeRunResult prr = doPipe(pipe, input, session);

		return prr.getResult().asString();
	}
}
