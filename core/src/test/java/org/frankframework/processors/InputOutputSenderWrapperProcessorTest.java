package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.AbstractSender;
import org.frankframework.senders.SenderSeries;
import org.frankframework.senders.AbstractSenderWrapper;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;

public class InputOutputSenderWrapperProcessorTest {

	private final TestConfiguration configuration = new TestConfiguration();
	private PipeLineSession session;
	private String secondSenderOutput;
	private SenderSeries sender;

	@BeforeEach
	public void setUp() {
		session = new PipeLineSession();
		secondSenderOutput = null;

		sender = configuration.createBean(SenderSeries.class);
		sender.addSender(new AbstractSender() {
			@Override
			public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
				try {
					return new SenderResult("Sender 1: [" + message.asString() + "]");
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}
		});
		sender.addSender(new AbstractSender() {
			@Override
			public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
				try {
					secondSenderOutput = "Sender 2: [" + message.asString() + "]";
					return new SenderResult(secondSenderOutput);
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}
		});
	}

	private void testInputOutputSenderWrapperProcessor(AbstractSenderWrapper sender, String input, String expectedSecondSenderOutput, String expectedWrapperOutput, String expectedSessionKeyValue) throws Exception {
		InputOutputSenderWrapperProcessor processor = new InputOutputSenderWrapperProcessor();

		SenderWrapperProcessor target = new SenderWrapperProcessor() {

			@Override
			public SenderResult sendMessage(AbstractSenderWrapper abstractSenderWrapper, Message message, PipeLineSession session) throws SenderException, TimeoutException {
				return abstractSenderWrapper.sendMessage(message, session);
			}
		};

		processor.setSenderWrapperProcessor(target);

		SenderResult actual = processor.sendMessage(sender, new Message(input), session);

		assertEquals(expectedSecondSenderOutput, secondSenderOutput, "unexpected output of last sender");
		assertEquals(expectedWrapperOutput, actual.getResult().asString(), "unexpected wrapper output");
		assertTrue(actual.isSuccess(), "unexpected wrapper output");
		assertEquals(expectedSessionKeyValue, session.getString("storedResult"), "unexpected session variable value");
	}

	@Test
	public void testBasic() throws Exception {
		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedSecondSenderOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValue() throws Exception {
		sender.setGetInputFromFixedValue("def");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [def]]";
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedSecondSenderOutput, expectedSessionKeyValue);
	}

	@Test
	public void testBasicPreserve() throws Exception {
		sender.setPreserveInput(true);

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = "abc";
		String expectedSessionKeyValue = null;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValuePreserve() throws Exception {
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
		sender.setStoreResultInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = expectedSecondSenderOutput;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValueStoreResult() throws Exception {
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
		sender.setStoreInputInSessionKey("storedResult");

		String input = "abc";
		String expectedSecondSenderOutput = "Sender 2: [Sender 1: [abc]]";
		String expectedWrapperOutput = expectedSecondSenderOutput;
		String expectedSessionKeyValue = input;

		testInputOutputSenderWrapperProcessor(sender, input, expectedSecondSenderOutput, expectedWrapperOutput, expectedSessionKeyValue);
	}

	@Test
	public void testGetInputFromFixedValuePreserveStoreResult() throws Exception {
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
