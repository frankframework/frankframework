package org.frankframework.senders;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

public class FixedResultSenderTest extends SenderTestBase<FixedResultSender> {

	@Override
	public FixedResultSender createSender() {
		return new FixedResultSender();
	}

	@Test
	public void basic() {
		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);
		assertThat(e.getMessage(), endsWith("has neither fileName nor returnString specified"));
	}

	@Test
	public void testReturnString() throws Exception {
		String text="Dit is het resultaat";
		sender.setReturnString(text);
		sender.configure();
		sender.start();
		Message input = new Message("dummy message");
		String result = sender.sendMessageOrThrow(input, session).asString();
		assertEquals(text,result);
	}

	@Test
	public void testNullInput() throws Exception {
		String text="Dit is het resultaat";
		sender.setReturnString(text);
		sender.configure();
		sender.start();
		String result = sender.sendMessageOrThrow(null, session).asString();
		assertEquals(text,result);
	}

	@Test
	public void testFilename() throws Exception {
		String filename = "/FixedResult/input.xml";
		sender.setFilename(filename);
		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("dummy message"), session).asString();

		String expected = TestFileUtils.getTestFile(filename);
		assertEquals(expected,result);
	}

	@Test
	public void testWithStylesheet() throws Exception {
		String filename = "/FixedResult/input.xml";
		sender.setFilename(filename);
		sender.setStyleSheetName("/FixedResult/sub.xslt");
		sender.configure();
		sender.start();

		String result = sender.sendMessageOrThrow(new Message("dummy message"), session).asString();

		String expected = TestFileUtils.getTestFile("/FixedResult/result.xml");
		assertEquals(expected,result);
	}

}
