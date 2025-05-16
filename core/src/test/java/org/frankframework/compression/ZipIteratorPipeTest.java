package org.frankframework.compression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.spy;

import java.io.File;
import java.net.URL;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.senders.EchoSender;
import org.frankframework.stream.FileMessage;
import org.frankframework.stream.Message;
import org.frankframework.stream.MessageContext;
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
	void testZipIteratorPipeSenderMessage() throws Exception {
		EchoSender sender = spy(getConfiguration().createBean(EchoSender.class));
		doAnswer(e -> {
			Message zipEntryName = e.getArgument(0);
			PipeLineSession localSession = e.getArgument(1);
			Message zipEntry = localSession.getMessage(pipe.getContentsSessionKey());

			assertEquals(zipEntryName.asString(), zipEntry.getContext().get(MessageContext.METADATA_NAME));
			return new Message(StringUtils.join(zipEntry.getContext().getAll()));
		}).when(sender).sendMessageOrThrow(any(Message.class), any(PipeLineSession.class));

		pipe.setSender(sender);
		pipe.configure();
		pipe.start();

		URL zip = TestFileUtils.getTestFileURL("/Unzip/ab.zip");

		PipeRunResult prr = doPipe(new FileMessage(new File(zip.getFile())));

		assertEquals("""
				<results>
				<result item="1">
				{Metadata.Name=fileaa.txt, Metadata.Size=3, Metadata.ModificationTime=2021-04-07 15:09:20.000}
				</result>
				<result item="2">
				{Metadata.Name=filebb.log, Metadata.Size=3, Metadata.ModificationTime=2021-04-07 15:09:28.000}
				</result>
				</results>""", prr.getResult().asString());
	}

	@Test
	void testEmptyInput() throws Exception {
		pipe.setSender(new EchoSender());
		pipe.configure();
		pipe.start();

		assertThrows(PipeRunException.class, () -> doPipe(Message.nullMessage()));
	}
}
