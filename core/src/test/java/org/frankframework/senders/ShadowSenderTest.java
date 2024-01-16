package org.frankframework.senders;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ISender;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.stream.Message;
import org.frankframework.util.XmlUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import lombok.Getter;
import lombok.Setter;

public class ShadowSenderTest extends ParallelSendersTest {

	private static final String INPUT_MESSAGE = "test input, geen xml";
	private static final String ORIGINAL_SENDER_NAME = "originalSender";
	private static final String ORIGINAL_SENDER_RESULT = "original-sender-result";
	private static final String RESULT_SENDER_NAME = "resultSender";

	@Override
	public ShadowSender createSender() throws Exception {
		ShadowSender ps = new ShadowSender();

		ps.setOriginalSender(ORIGINAL_SENDER_NAME);
		ps.registerSender(createOriginalSender());

		ps.setResultSender(RESULT_SENDER_NAME);
		ps.registerSender(createResultSender());

		return ps;
	}

	@Override
	protected String getExpectedTestFile(String path) {
		return ORIGINAL_SENDER_RESULT; //Should always return the result of the originalSender
	}

	private ResultSender createResultSender() {
		ResultSender resultSender = new ResultSender();
		resultSender.setName(RESULT_SENDER_NAME);
		getConfiguration().autowireByName(resultSender);
		return resultSender;
	}

	private static final class ResultSender extends EchoSender {
		private @Getter @Setter Message result;

		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
			result = Message.asMessage(message);
			return super.sendMessage(message, session);
		}
	}

	private ISender createOriginalSender() {
		EchoSender originalSender = new EchoSender() {
			@Override
			public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
				return new SenderResult(ORIGINAL_SENDER_RESULT);
			}
		};
		originalSender.setName(ORIGINAL_SENDER_NAME);
		getConfiguration().autowireByName(originalSender);
		return originalSender;
	}

	@Test
	public void testWithDefaultResultSender() throws Exception {
		((ShadowSender)sender).setResultSender(null);
		sender.configure();
		sender.open();
		assertEquals("resultSender", ((ShadowSender)sender).getResultSenderName());
	}

	@Test
	public void testWithoutDefaultOriginalSender() throws Exception {
		((ShadowSender)sender).setOriginalSender(null);
		sender.configure();
		sender.open();
		assertEquals("originalSender", ((ShadowSender)sender).getOriginalSenderName());
	}

	@Test
	public void testMultipleResultSenders() throws Exception {
		ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
			sender.registerSender(createResultSender());
			sender.configure();
			sender.open();
		});
		assertEquals("resultSender can only be defined once", exception.getMessage());
	}

	@Test
	public void testMultipleOriginalSenders() throws Exception {
		ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
			sender.registerSender(createOriginalSender());
			sender.configure();
			sender.open();
		});
		assertEquals("originalSender can only be defined once", exception.getMessage());
	}

	@Test
	public void testNoSenders() throws Exception {
		ConfigurationException exception = assertThrows(ConfigurationException.class, () -> {
			ShadowSender ps = new ShadowSender();
			ps.configure();
			ps.open();
		});
		assertEquals("ShadowSender should contain at least 2 Senders, none found", exception.getMessage());
	}

	@ParameterizedTest
	@ValueSource(booleans = {true, false})
	public void testNoShadowSenders(boolean waitForCompletionOfShadows) throws Exception {
		((ShadowSender)sender).setWaitForShadowsToFinish(waitForCompletionOfShadows);
		sender.configure();
		sender.open();

		String result = sender.sendMessageOrThrow(new Message(INPUT_MESSAGE), session).asString();
		assertEquals(ORIGINAL_SENDER_RESULT, result);

		if (!waitForCompletionOfShadows) {
			Thread.sleep(1000); // wait for results to be collected in the background
		}
		Message senderResult = null;
		for(ISender sender : sender.getSenders()) {
			if(RESULT_SENDER_NAME.equals(sender.getName())) {
				senderResult = ((ResultSender)sender).getResult();
			}
		}
		if(senderResult == null) {
			fail("no sender result");
		}

		Element el = XmlUtils.buildDomDocument(senderResult.asInputSource(), false).getDocumentElement();

		String origMsg = XmlUtils.getChildTagAsString(el, "originalMessage");
		assertEquals(INPUT_MESSAGE, origMsg);

		Element origResult = XmlUtils.getFirstChildTag(el, "originalResult");
		assertEquals(ORIGINAL_SENDER_RESULT, XmlUtils.getStringValue(origResult, true));
		assertEquals(ORIGINAL_SENDER_NAME, origResult.getAttribute("senderName"));
		int duration = Integer.parseInt(origResult.getAttribute("duration"));
		assertTrue(duration < 200, "test took more then [200ms] duration ["+duration+"]");

		Collection<Node> shadowResults = XmlUtils.getChildTags(el, "shadowResult");
		assertEquals(0, shadowResults.size());
	}

	@Test
	public void testResultSenderResultWith3ShadowSenders() throws Exception {
		sender.registerSender(new TestSender("shadowSenderWithDelay1"));
		sender.registerSender(new TestSender("shadowSenderWithDelay2"));
		sender.registerSender(new TestSender("shadowSenderWithDelay3"));

		sender.configure();
		sender.open();
		String result = sender.sendMessageOrThrow(new Message(INPUT_MESSAGE), session).asString();
		assertEquals(ORIGINAL_SENDER_RESULT, result);

		Thread.sleep(3000); // wait for results to be collected in the background
		Message senderResult = null;
		for(ISender sender : sender.getSenders()) {
			if(RESULT_SENDER_NAME.equals(sender.getName())) {
				senderResult = ((ResultSender)sender).getResult();
			}
		}
		if(senderResult == null) {
			fail("no sender result");
		}

		Element el = XmlUtils.buildDomDocument(senderResult.asInputSource(), false).getDocumentElement();

		String origMsg = XmlUtils.getChildTagAsString(el, "originalMessage");
		assertEquals(INPUT_MESSAGE, origMsg);

		Element origResult = XmlUtils.getFirstChildTag(el, "originalResult");
		assertEquals(ORIGINAL_SENDER_RESULT, XmlUtils.getStringValue(origResult, true));
		assertEquals(ORIGINAL_SENDER_NAME, origResult.getAttribute("senderName"));
		assertThat(Integer.parseInt(origResult.getAttribute("duration")), lessThan(200));

		Collection<Node> shadowResults = XmlUtils.getChildTags(el, "shadowResult");
		assertEquals(3, shadowResults.size());
		for(Node node : shadowResults) {
			Element shadowResult = (Element) node;
			assertEquals(INPUT_MESSAGE, XmlUtils.getStringValue(shadowResult, true));
			assertTrue(shadowResult.getAttribute("senderName").startsWith("shadowSenderWithDelay"));
			int duration = Integer.parseInt(shadowResult.getAttribute("duration"));
			assertThat("test duration was ["+duration+"]", duration, is(both(greaterThanOrEqualTo(2000)).and(lessThan(2150))));
		}
	}

	@Test
	@Override
	@Disabled("Test not suited for ShadowSender")
	public void testSingleExceptionHandling() throws Exception {
		//Test not suited for ShadowSender
	}

	@Test
	@Override
	@Disabled("Test not suited for ShadowSender")
	public void testExceptionHandling() throws Exception {
		//Test not suited for ShadowSender
	}

}
