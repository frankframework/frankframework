package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

public class FunctionalTransformerPoolTestBase {

	public void testTransformerPool(TransformerPool tp, String input, String expected, boolean namespaceAware, String message) throws DomBuilderException, TransformerException, IOException {
		String actual=tp.transform(input, null, namespaceAware);
		assertEquals(message,expected.trim(),actual.trim());
	}

	public void testTransformerPool(TransformerPool tp, Source input, String expected, boolean namespaceAware, String message) throws DomBuilderException, TransformerException, IOException {
		String actual=tp.transform(input, null);
		assertEquals(message,expected.trim(),actual.trim());
	}

	public void testTransformerPool(TransformerPool tp, String input, String expected) throws DomBuilderException, TransformerException, IOException {
		testTransformerPool(tp, input, expected, true, "String input");
	}
	
	public void testXslt(String xslt, String input, String expected) throws DomBuilderException, TransformerException, IOException {
		testXslt(xslt, input, expected, false);
	}
	
	public void testXslt(String xslt, String input, String expected, boolean xslt2) throws DomBuilderException, TransformerException, IOException {
		TransformerPool tp = TransformerPool.getInstance(xslt,xslt2);
		testTransformerPool(tp,input,expected);
	}
	
}
