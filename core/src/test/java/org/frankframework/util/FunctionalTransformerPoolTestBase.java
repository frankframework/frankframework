package org.frankframework.util;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.frankframework.testutil.TestAssertions.assertEqualsIgnoreCRLF;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import org.frankframework.stream.Message;

public class FunctionalTransformerPoolTestBase {

	public void testTransformerPool(TransformerPool tp, String input, String expected, String message) throws TransformerException, IOException, SAXException {
		// Test with String input/output
		String actual=tp.transform(input, null, true);
		assertEqualsIgnoreCRLF(expected, actual, message);

		// Test with Message input/output
		Message messageOut = tp.transform(new Message(input), null);
		String messageOutString = messageOut.asString();
		if ("XML".equals(tp.getOutputMethod())) {
			// If XML output then do XML compare since the formatting of the XML can be slightly different from the other transformer.
			assertXmlEquals(message, expected, messageOutString);
		} else {
			assertEqualsIgnoreCRLF(expected, actual, message);
		}
	}

	public void testTransformerPool(TransformerPool tp, Source input, String expected, String message) throws TransformerException, IOException {
		String actual=tp.transform(input);
		assertEqualsIgnoreCRLF(expected,actual,message);
	}

	public void testTransformerPool(TransformerPool tp, String input, String expected) throws TransformerException, IOException, SAXException {
		testTransformerPool(tp, input, expected, "String input");
	}
}
