package org.frankframework.filesystem.exchange;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import org.frankframework.filesystem.AbstractMailListener;
import org.frankframework.filesystem.AbstractMailListener.MessageType;
import org.frankframework.filesystem.BasicFileSystemListenerTest;
import org.frankframework.filesystem.IFileSystemTestHelper;
import org.frankframework.receivers.ExchangeMailListener;
import org.frankframework.receivers.RawMessageWrapper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MatchUtils;

@Tag("slow")
@Tag("unstable") // Relies on a remote API that is not always available for the tests
public class ExchangeFileSystemListenerTest extends BasicFileSystemListenerTest<MailItemId, ExchangeFileSystem> {

	@BeforeAll
	public static void beforeAll() {
		assumeTrue(ExchangeConnectionCache.validateCredentials());
	}

	@AfterAll
	public static void afterAll() {
		ExchangeConnectionCache.close();
	}

	@Override
	protected IFileSystemTestHelper getFileSystemTestHelper() {
		setWaitMillis(ExchangeFileSystemTestHelper.WAIT_MILLIS);

		return ExchangeConnectionCache.getExchangeFileSystemTestHelper();
	}

	@Override
	public ExchangeMailListener createFileSystemListener() {
		return ExchangeConnectionCache.getExchangeMailListener();
	}

	@SuppressWarnings("java:S2699") // Sonar 'Add at least one assertion to this test case.'
	@Test
	@Override
	public void fileListenerTestGetStringFromRawMessageFilename() throws Exception {
		if (fileSystemListener instanceof AbstractMailListener amfs) {
			amfs.setMessageType(MessageType.NAME);
		}
		super.fileListenerTestGetStringFromRawMessageFilename();
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageEmail() throws Exception {
		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		// We should set fileSystemListener.setMessageType(MessageType.EMAIL); but it's the default...
		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.start();

		String id = createFile(null, filename, contents);

		RawMessageWrapper<MailItemId> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message = fileSystemListener.extractMessage(rawMessage, threadContext);
		MatchUtils.assertXmlEquals("""
				<email name="%s">
					<recipients>
						<recipient type="to">Sergi Philipsen &lt;sergi@frankframework.org&gt;</recipient>
					</recipients>
					<subject>rawMessageFile</subject>
					<DateTimeSent>STUB</DateTimeSent>
					<DateTimeReceived>STUB</DateTimeReceived>
					<message>Test Message Contents</message>
					<attachments/>
					<headers/>
				</email>""".formatted(id), message.asString()
						.replaceAll("<DateTimeSent>.*</DateTimeSent>", "<DateTimeSent>STUB</DateTimeSent>")
						.replaceAll("<DateTimeReceived>.*</DateTimeReceived>", "<DateTimeReceived>STUB</DateTimeReceived>")
				);
	}

	@Test
	public void fileListenerTestGetStringFromRawMessageMime() throws Exception {
		if (fileSystemListener instanceof AbstractMailListener amfs) {
			amfs.setMessageType(MessageType.MIME);
		}

		String filename = "rawMessageFile";
		String contents = "Test Message Contents";

		fileSystemListener.setMinStableTime(0);
		fileSystemListener.configure();
		fileSystemListener.start();

		createFile(null, filename, contents);

		RawMessageWrapper<MailItemId> rawMessage = fileSystemListener.getRawMessage(threadContext);
		assertNotNull(rawMessage);

		Message message = fileSystemListener.extractMessage(rawMessage, threadContext);
		String result = message.asString();

		/*
		 * To: John Doe <john@doe.org>
		 * Subject: rawMessageFile
		 * Thread-Topic: rawMessageFile
		 * Thread-Index: AQHcyPC6XmczCv4TMEKXQ66wpqa9Ng==
		 * Date: Fri, 10 Apr 2026 13:48:35 +0000
		 * Message-ID: <VI0PR02MB12206CD80D435CC24FD980D93B8592@VI0PR02MB12206.eurprd02.prod.outlook.com>
		 * Content-Language: en-US
		 * X-MS-Has-Attach:
		 * X-MS-TNEF-Correlator:
		 * X-MS-Exchange-Organization-RecordReviewCfmType: 0
		 * Content-Type: text/plain; charset="us-ascii"
		 * MIME-Version: 1.0

		 * Test Message Contents
		 */

		assertThat(result, containsString("Subject: rawMessageFile"));
		assertThat(result, containsString("Thread-Topic: rawMessageFile"));
		assertThat(result, containsString("Date: "));
		assertThat(result, containsString("MIME-Version: 1.0"));
		assertThat(result, containsString("Test Message Contents"));
	}
}
