package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class FixedResultSenderTest extends SenderTestBase<FixedResultSender> {

	@Override
	public FixedResultSender createSender() {
		return new FixedResultSender();
	}

	@Test
	public void basic() throws Exception {
		exception.expectMessage("has neither fileName nor returnString specified");
		sender.configure();
		sender.open();
		Message input = new Message("<dummy/>");
		String result = sender.sendMessage(input, session).asString();
		assertEquals(input.asString(), result);
	}
	
	@Test
	public void testReturnString() throws Exception {
		String text="Dit is het resultaat";
		sender.setReturnString(text);
		sender.configure();
		sender.open();
		Message input = new Message("dummy message");
		String result = sender.sendMessage(input, session).asString();
		assertEquals(text,result);
	}

	@Test
	public void testNullInput() throws Exception {
		String text="Dit is het resultaat";
		sender.setReturnString(text);
		sender.configure();
		sender.open();
		String result = sender.sendMessage(null, session).asString();
		assertEquals(text,result);
	}

	@Test
	public void testFilename() throws Exception {
		String filename = "/FixedResult/input.xml";
		sender.setFileName(filename);
		sender.configure();
		sender.open();
		
		String result = sender.sendMessage(new Message("dummy message"), session).asString();
		
		String expected = TestFileUtils.getTestFile(filename);
		assertEquals(expected,result);
	}

	@Test
	public void testWithStylesheet() throws Exception {
		String filename = "/FixedResult/input.xml";
		sender.setFileName(filename);
		sender.setStyleSheetName("/FixedResult/sub.xslt");
		sender.configure();
		sender.open();
		
		String result = sender.sendMessage(new Message("dummy message"), session).asString();
		
		String expected = TestFileUtils.getTestFile("/FixedResult/result.xml");
		assertEquals(expected,result);
	}

}
