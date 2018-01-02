package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TransformerPoolTest {

	@Test
	public void doNotUseCaching() throws Exception {
		String xml = "<root><message>hello</message></root>";
		String xpath = "root/message";
		TransformerPool transformerPool = TransformerPool.getInstance(XmlUtils
				.createXPathEvaluatorSource(xpath));
		String result = transformerPool.transform(xml, null);
		assertEquals("hello", result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void useCaching() throws Exception {
		String xpath = "root/message";
		String xpathEvaluatorSource = XmlUtils
				.createXPathEvaluatorSource(xpath);
		TransformerPool.getInstance(xpathEvaluatorSource, null, false, true);
		assertEquals(1, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(xpathEvaluatorSource, "xyz", false, true);
		assertEquals(2, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(xpathEvaluatorSource, null, true, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(xpathEvaluatorSource, null, false, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
		String xpath2 = "root/@message";
		TransformerPool.getInstance(
				XmlUtils.createXPathEvaluatorSource(xpath2), null, false, true);
		assertEquals(4, TransformerPool.getTransformerPoolsKeys().size());
	}
}
