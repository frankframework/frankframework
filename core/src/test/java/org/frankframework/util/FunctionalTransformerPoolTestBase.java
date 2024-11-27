package org.frankframework.util;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import org.frankframework.testutil.TestAssertions;

public class FunctionalTransformerPoolTestBase {

	public void testTransformerPool(TransformerPool tp, String input, String expected, boolean namespaceAware, String message) throws TransformerException, IOException, SAXException {
		String actual=tp.transform(input, null, namespaceAware);
		TestAssertions.assertEqualsIgnoreCRLF(expected,actual,message);
	}

	public void testTransformerPool(TransformerPool tp, Source input, String expected, boolean namespaceAware, String message) throws TransformerException, IOException {
		String actual=tp.transform(input);
		TestAssertions.assertEqualsIgnoreCRLF(expected,actual,message);
	}

	public void testTransformerPool(TransformerPool tp, String input, String expected) throws TransformerException, IOException, SAXException {
		testTransformerPool(tp, input, expected, true, "String input");
	}
}
