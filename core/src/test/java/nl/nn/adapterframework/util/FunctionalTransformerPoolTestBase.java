package nl.nn.adapterframework.util;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

public class FunctionalTransformerPoolTestBase {

	public void testTransformerPool(TransformerPool tp, String input, String expected, boolean namespaceAware, String message) throws DomBuilderException, TransformerException, IOException {
		String actual=tp.transform(input, null, namespaceAware);
		TestAssertions.assertEqualsIgnoreCRLF(message,expected,actual);
	}

	public void testTransformerPool(TransformerPool tp, Source input, String expected, boolean namespaceAware, String message) throws DomBuilderException, TransformerException, IOException {
		String actual=tp.transform(input, null);
		TestAssertions.assertEqualsIgnoreCRLF(message,expected,actual);
	}

	public void testTransformerPool(TransformerPool tp, String input, String expected) throws DomBuilderException, TransformerException, IOException {
		testTransformerPool(tp, input, expected, true, "String input");
	}

	public void testXslt(String xslt, String input, String expected) throws DomBuilderException, TransformerException, IOException {
		testXslt(xslt, input, expected, 0);
	}

	public void testXslt(String xslt, String input, String expected, int xsltVersion) throws DomBuilderException, TransformerException, IOException {
		TransformerPool tp = TransformerPool.getInstance(xslt,xsltVersion);
		testTransformerPool(tp,input,expected);
	}
}
