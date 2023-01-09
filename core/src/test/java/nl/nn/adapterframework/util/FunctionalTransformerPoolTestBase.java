package nl.nn.adapterframework.util;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.xml.sax.SAXException;

import nl.nn.adapterframework.testutil.TestAssertions;

public class FunctionalTransformerPoolTestBase {

	public void testTransformerPool(TransformerPool tp, String input, String expected, boolean namespaceAware, String message) throws TransformerException, IOException, SAXException {
		String actual=tp.transform(input, null, namespaceAware);
		TestAssertions.assertEqualsIgnoreCRLF(expected,actual,message);
	}

	public void testTransformerPool(TransformerPool tp, Source input, String expected, boolean namespaceAware, String message) throws DomBuilderException, TransformerException, IOException {
		String actual=tp.transform(input);
		TestAssertions.assertEqualsIgnoreCRLF(expected,actual,message);
	}

	public void testTransformerPool(TransformerPool tp, String input, String expected) throws TransformerException, IOException, SAXException {
		testTransformerPool(tp, input, expected, true, "String input");
	}

	public void testXslt(String xslt, String input, String expected) throws TransformerException, IOException, SAXException {
		testXslt(xslt, input, expected, 0);
	}

	public void testXslt(String xslt, String input, String expected, int xsltVersion) throws TransformerException, IOException, SAXException {
		TransformerPool tp = TransformerPool.getInstance(xslt,xsltVersion);
		testTransformerPool(tp,input,expected);
	}
}
