package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class FixedResultSenderTest extends SenderTestBase<FixedResultSender> {

	@Override
	public FixedResultSender createSender() {
		return new FixedResultSender();
	}

	@Test
	public void basic() throws SenderException, TimeOutException, ConfigurationException {
		exception.expectMessage("has neither fileName nor returnString specified");
		sender.configure();
		sender.open();
		String input = "<dummy/>";
		String result = sender.sendMessage(null, input);
		assertEquals(input, result);
	}
	
	@Test
	public void testReturnString() throws Exception {
		String text="Dit is het resultaat";
		sender.setReturnString(text);
		sender.configure();
		sender.open();
		String result = sender.sendMessage("fakeCorrelationId", "dummy message");
		assertEquals(text,result);
	}

	@Test
	public void testNullInput() throws Exception {
		String text="Dit is het resultaat";
		sender.setReturnString(text);
		sender.configure();
		sender.open();
		String result = sender.sendMessage("fakeCorrelationId", null);
		assertEquals(text,result);
	}

	@Test
	public void testFilename() throws Exception {
		String filename = "/FixedResult/input.xml";
		sender.setFileName(filename);
		sender.configure();
		sender.open();
		
		String result = sender.sendMessage("fakeCorrelationId", "dummy message");
		
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
		
		String result = sender.sendMessage("fakeCorrelationId", "dummy message");
		
		String expected = TestFileUtils.getTestFile("/FixedResult/result.xml");
		assertEquals(expected,result);
	}

}
