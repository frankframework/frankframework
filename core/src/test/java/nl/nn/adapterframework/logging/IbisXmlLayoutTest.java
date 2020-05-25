package nl.nn.adapterframework.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.Test;

import nl.nn.adapterframework.testutil.TestAssertions;

/**
 * This class tests StringLayouts without starting/using log4j2.
 */
public class IbisXmlLayoutTest {

	IbisXmlLayout layout = new IbisXmlLayout(null, null, true);

	private LogEvent generateLogEvent(String string) {
		MutableLogEvent logEvent = new MutableLogEvent();
		logEvent.setLevel(Level.DEBUG);
		Message message = new SimpleMessage(string);
		logEvent.setMessage(message);
		return logEvent;
	}

	@Test
	public void testNormalMessage() {
		LogEvent logEvent = generateLogEvent("my test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my test message!</message>\n" + 
				"  <throwable />\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testCdataMessage() {
		LogEvent logEvent = generateLogEvent("my <![CDATA[cdata]]> test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &amp;lt;![CDATA[cdata]]&amp;gt; test message!</message>\n" + 
				"  <throwable />\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testUnicodeMessage() {
		LogEvent logEvent = generateLogEvent("my  test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &amp;#16; test message!</message>\n" + 
				"  <throwable />\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testUnicodeAndCdataMessage() {
		LogEvent logEvent = generateLogEvent("my <![CDATA[cdata]]> test message with  unicode!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &amp;lt;![CDATA[cdata]]&amp;gt; test message with &amp;#16; unicode!</message>\n" + 
				"  <throwable />\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testUnicodeInCdataMessage() {
		LogEvent logEvent = generateLogEvent("my <![CDATA[cdata with  unicode]]> test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &amp;lt;![CDATA[cdata with &amp;#16; unicode]]&amp;gt; test message!</message>\n" + 
				"  <throwable />\n" + 
				"</event>", result.trim());
	}
}
