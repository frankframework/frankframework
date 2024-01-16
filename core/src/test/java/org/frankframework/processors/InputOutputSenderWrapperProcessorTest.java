package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.SenderBase;
import org.frankframework.senders.SenderSeries;
import org.frankframework.senders.SenderWrapperBase;
import org.frankframework.stream.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class InputOutputSenderWrapperProcessorTest {

	private PipeLineSession session;
	private String secondSenderOutput;

	@BeforeEach
	public void setUp() {
		session = new PipeLineSession();
		secondSenderOutput = null;
	}

	public void testInputOutputSenderWrapperProcessor(SenderWrapperBase sender, String input, String expectedSecondSenderOutput, String expectedWrapperOutput, String expectedSessionKeyValue) throws Exception {
		InputOutputSenderWrapperProcessor processor = new InputOutputSenderWrapperProcessor();

		SenderWrapperProcessor target = new SenderWrapperProcessor() {

			@Override
			public SenderResult sendMessage(SenderWrapperBase senderWrapperBase, Message message, PipeLineSession session) throws SenderException, TimeoutException {
				return senderWrapperBase.sendMessage(message, session);
			}
		};

		processor.setSenderWrapperProcessor(target);

		SenderResult actual = processor.sendMessage(sender, new Message(input), session);

		assertEquals(expectedSecondSenderOutput, secondSenderOutput, "unexpected output of last sender");
		assertEquals(expectedWrapperOutput, actual.getResult().asString(), "unexpected wrapper output");
		assertEquals(true, actual.isSuccess(), "unexpected wrapper output");
		assertEquals(expectedSessionKeyValue, Message.asString(session.get("storedResult")), "unexpected session variable value");
	}

	public SenderWrapperBase getSenderWrapper() {
		SenderSeries senderSeries = new SenderSeries();
		senderSeries.registerSender(new SenderBase() {
			@Override
			public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
				try {
					return new SenderResult("Sender 1: ["+message.asString()+"]");
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}});
		senderSeries.registerSender(new SenderBase() {
			@Override
			public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
				try {
					secondSenderOutput = "Sender 2: ["+message.asString()+"]";
					return new SenderResult(secondSenderOutput);
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}});
		return senderSeries;
	}

	@Test
	public void testBasic() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValue() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setGetInputFromFixedValue("def");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [def]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testBasicPreserve() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setPreserveInput(true);

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = "abc";
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValuePreserve() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setGetInputFromFixedValue("def");
		sender.setPreserveInput(true);

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [def]]";
		String expectedWrapperOutput = "abc";
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testBasicStoreResult() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setStoreResultInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = expectedSecondSenderOutput;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValueStoreResult() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setGetInputFromFixedValue("def");
		sender.setStoreResultInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [def]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = expectedSecondSenderOutput;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testBasicPreserveStoreResult() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setPreserveInput(true);
		sender.setStoreResultInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = "abc";
		String expectedSessionKeyValue = expectedSecondSenderOutput;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testStoreInput() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setStoreInputInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = input;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValuePreserveStoreResult() throws Exception {
		SenderWrapperBase sender = getSenderWrapper();
		sender.setGetInputFromFixedValue("def");
		sender.setPreserveInput(true);
		sender.setStoreResultInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [def]]";
		String expectedWrapperOutput = "abc";
		String expectedSessionKeyValue = expectedSecondSenderOutput;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

}

