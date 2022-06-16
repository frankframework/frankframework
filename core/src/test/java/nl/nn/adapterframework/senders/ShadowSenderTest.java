package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Test;
import org.w3c.dom.Element;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.core.ISender;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlUtils;

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
	protected String getTestFile(String path) throws IOException {
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
		public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
			result = Message.asMessage(message);
			return super.sendMessage(message, session);
		}
	}

	private ISender createOriginalSender() {
		EchoSender originalSender = new EchoSender() {
			@Override
			public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
				return new Message(ORIGINAL_SENDER_RESULT);
			}
		};
		originalSender.setName(ORIGINAL_SENDER_NAME);
		getConfiguration().autowireByName(originalSender);
		return originalSender;
	}

	@Test
	public void testResultSenderResult() throws Exception {
		sender.registerSender(new TestSender("shadowSenderWithDelay"));

		sender.configure();
		sender.open();
		String result = sender.sendMessage(new Message(INPUT_MESSAGE), session).asString();
		assertEquals(ORIGINAL_SENDER_RESULT, result);

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
		assertTrue(Integer.parseInt(origResult.getAttribute("duration")) < 10);

		Element shadowResult = XmlUtils.getFirstChildTag(el, "shadowResult");
		assertEquals(INPUT_MESSAGE, XmlUtils.getStringValue(shadowResult, true));
		assertEquals(ORIGINAL_SENDER_NAME, origResult.getAttribute("senderName"));
		int duration = Integer.parseInt(shadowResult.getAttribute("duration"));
		assertTrue(duration > 2000 && duration < 2050);
	}
}
