package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import org.junit.Test;

public class TransformerPoolTest {

	@Test
	public void doNotUseCachingXpath() throws Exception {
		TransformerPool.clearTransformerPools();
		String xml = "<root><message>hello</message></root>";
		String xpath = "root/message";
		TransformerPool transformerPool = TransformerPool.getInstance(XmlUtils
				.createXPathEvaluatorSource(xpath));
		String result = transformerPool.transform(xml, null);
		assertEquals("hello", result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void useCachingXpath() throws Exception {
		TransformerPool.clearTransformerPools();
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

	@Test
	public void doNotUseCachingUrl() throws Exception {
		TransformerPool.clearTransformerPools();
		String xml = "<root><message active=\"false\">hello</message></root>";
		URL url = ClassUtils.getResourceURL(this, "xml/xsl/active.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(url);
		String result = transformerPool.transform(xml, null);
		String expected = "<root/>" + System.getProperty("line.separator");
		assertEquals(expected, result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void useCachingUrl() throws Exception {
		TransformerPool.clearTransformerPools();
		URL url = ClassUtils.getResourceURL(this, "xml/xsl/active.xsl");
		TransformerPool.getInstance(url, false, true);
		assertEquals(1, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(url, true, true);
		assertEquals(2, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(url, false, true);
		assertEquals(2, TransformerPool.getTransformerPoolsKeys().size());
		URL url2 = ClassUtils.getResourceURL(this,
				"xml/xsl/AttributesGetter.xsl");
		TransformerPool.getInstance(url2, false, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
	}

}
