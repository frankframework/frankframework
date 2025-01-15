package org.frankframework.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

class ZipIteratorPipeTest extends PipeTestBase<ZipIteratorPipe> {

	public static final String EXPECTED = """
			<results>
			<result item="1">
			fileaa.txt
			</result>
			<result item="2">
			filebb.log
			</result>
			</results>""";

	@Override
	public ZipIteratorPipe createPipe() throws ConfigurationException {
		return new ZipIteratorPipe();
	}

	@Test
	void testSessionKeyIsSet() {
		pipe.setSender(new EchoSender());
		pipe.setContentsSessionKey("");
		assertThrows(ConfigurationException.class, this::configurePipe);
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	void testZipIteratorPipe(boolean streamingContents) throws Exception {
		pipe.setSender(new EchoSender());
		pipe.setStreamingContents(streamingContents);
		pipe.configure();
		pipe.start();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		PipeRunResult prr = doPipe(new FileMessage(new File(zip.getFile())));

		assertEquals(EXPECTED, prr.getResult().asString());
	}

	@Test
	void testEmptyInput() throws Exception {
		pipe.setSender(new EchoSender());
		pipe.configure();
		pipe.start();

		assertThrows(PipeRunException.class, () -> doPipe(Message.nullMessage()));
	}
}
