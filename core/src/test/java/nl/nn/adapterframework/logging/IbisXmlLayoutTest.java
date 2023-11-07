/*
   Copyright 2020 WeAreFrank!

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package nl.nn.adapterframework.logging;

import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.MutableLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

/**
 * This class tests StringLayouts without starting/using log4j2.
 */
public class IbisXmlLayoutTest {

	private IbisXmlLayout layout = new IbisXmlLayout(null, null, false);
	private static final String UNICODE_CHARACTERS = " aâΔع你好ಡತ ";

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
				"</event>", result.trim());
	}

	@Test
	public void testCdataMessage() {
		LogEvent logEvent = generateLogEvent("my <![CDATA[cdata]]> test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &lt;![CDATA[cdata]]&gt; test message!</message>\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testUnicodeMessage() {
		LogEvent logEvent = generateLogEvent("my "+UNICODE_CHARACTERS+" test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my \\u0010 a\\u00E2\\u0394\\u0639\\u4F60\\u597D\\u0CA1\\u0CA4  test message!</message>\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testUnicodeAndCdataMessage() {
		LogEvent logEvent = generateLogEvent("my <![CDATA[cdata]]> test message with "+UNICODE_CHARACTERS+" unicode!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &lt;![CDATA[cdata]]&gt; test message with \\u0010 a\\u00E2\\u0394\\u0639\\u4F60\\u597D\\u0CA1\\u0CA4  unicode!</message>\n" + 
				"</event>", result.trim());
	}

	@Test
	public void testPdfParsedWithWrongCharset() throws IOException {
		String pfd = TestFileUtils.getTestFile("/Logging/pdf-parsed-with-wrong-charset.pdf");
		String expected = TestFileUtils.getTestFile("/Logging/xml-of-pdf-file.log");

		LogEvent logEvent = generateLogEvent(pfd);
		String actual = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF(expected, actual);
	}

	@Test
	public void testUnicodeInCdataMessage() {
		LogEvent logEvent = generateLogEvent("my <![CDATA[cdata with "+UNICODE_CHARACTERS+" unicode]]> test message!");
		String result = layout.toSerializable(logEvent);

		TestAssertions.assertEqualsIgnoreCRLF("<event timestamp=\"0\" level=\"DEBUG\">\n" + 
				"  <message>my &lt;![CDATA[cdata with \\u0010 a\\u00E2\\u0394\\u0639\\u4F60\\u597D\\u0CA1\\u0CA4  unicode]]&gt; test message!</message>\n" + 
				"</event>", result.trim());
	}
}
