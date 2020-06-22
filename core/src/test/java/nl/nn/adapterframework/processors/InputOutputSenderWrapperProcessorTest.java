package nl.nn.adapterframework.processors;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.senders.SenderBase;
import nl.nn.adapterframework.senders.SenderSeries;
import nl.nn.adapterframework.senders.SenderWrapperBase;
import nl.nn.adapterframework.stream.Message;

public class InputOutputSenderWrapperProcessorTest {

	private IPipeLineSession session; 
	private String secondSenderOutput;
	
	@Before
	public void setUp() {
		session = new PipeLineSessionBase();
		secondSenderOutput = null;
	}
	
	public void testInputOutputSenderWrapperProcessor(SenderWrapperBase sender, String input, String expectedSecondSenderOutput, String expectedWrapperOutput, String expectedSessionKeyValue) throws Exception {
		InputOutputSenderWrapperProcessor processor = new InputOutputSenderWrapperProcessor();
		
		SenderWrapperProcessor target = new SenderWrapperProcessor() {

			@Override
			public Message sendMessage(SenderWrapperBase senderWrapperBase, Message message, IPipeLineSession session) throws SenderException, TimeOutException {
				return senderWrapperBase.sendMessage(message, session);
			}
		};
		
		processor.setSenderWrapperProcessor(target);

		Message actual = processor.sendMessage(sender, new Message(input), session);
		
		assertEquals(expectedSecondSenderOutput, secondSenderOutput);
		assertEquals(expectedWrapperOutput, actual.asString());
		assertEquals(expectedSessionKeyValue, Message.asString(session.get("storedResult")));
	}
	
	public SenderWrapperBase getSenderWrapper() {
		SenderSeries senderSeries = new SenderSeries();
		senderSeries.setSender(new SenderBase() {
			@Override
			public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
				try {
					return new Message("Sender 1: ["+message.asString()+"]");
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}});
		senderSeries.setSender(new SenderBase() {
			@Override
			public Message sendMessage(Message message, IPipeLineSession session) throws SenderException, TimeOutException {
				try {
					secondSenderOutput = "Sender 2: ["+message.asString()+"]";
					return new Message(secondSenderOutput);
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

