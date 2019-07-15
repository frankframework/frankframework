package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;

import java.net.URL;

import javax.xml.transform.Source;

import org.apache.log4j.Logger;
import org.junit.Test;

public class TransformerPoolTest {
	protected Logger log = LogUtil.getLogger(this);

	private String xml = "<root><message active=\"false\">hello</message></root>";
	private String xpath = "root/message";
	private String expectedXpath = "hello";

	private String stylesheetURL = "xml/xsl/active.xsl";
	private String expectedStylesheetURL = "<root/>";

	@Test
	public void plainXPath() throws Exception {
		TransformerPool.clearTransformerPools();
		String xpathEvaluatorSource = XmlUtils.createXPathEvaluatorSource(xpath);
		TransformerPool transformerPool = TransformerPool.getInstance(xpathEvaluatorSource);
		String result = transformerPool.transform(xml, null);
		assertEquals(expectedXpath, result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void plainXPathUsingMultiUseSource() throws Exception {
		TransformerPool.clearTransformerPools();
		String xpathEvaluatorSource = XmlUtils.createXPathEvaluatorSource(xpath);
		TransformerPool transformerPool = TransformerPool.getInstance(xpathEvaluatorSource);
		Source source = XmlUtils.stringToSource(xml);
		String result = transformerPool.transform(source, null);
		assertEquals(expectedXpath, result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void plainViaUrl() throws Exception {
		TransformerPool.clearTransformerPools();
		URL url = ClassUtils.getResourceURL(this, stylesheetURL);
		TransformerPool transformerPool = TransformerPool.getInstance(url);
		String result = transformerPool.transform(xml, null);
		result=result.replaceAll("[\n\r]", "");		
		assertEquals(expectedStylesheetURL, result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void useCachingXpath() throws Exception {
		TransformerPool.clearTransformerPools();
		String xpath = "root/message";
		String xpathEvaluatorSource = XmlUtils.createXPathEvaluatorSource(xpath);
		TransformerPool.getInstance(xpathEvaluatorSource, null, 1, true);
		assertEquals(1, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(xpathEvaluatorSource, "xyz", 1, true);
		assertEquals(2, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(xpathEvaluatorSource, null, 2, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(xpathEvaluatorSource, null, 1, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
		String xpath2 = "root/@message";
		TransformerPool.getInstance(XmlUtils.createXPathEvaluatorSource(xpath2), null, 1, true);
		assertEquals(4, TransformerPool.getTransformerPoolsKeys().size());
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
		URL url2 = ClassUtils.getResourceURL(this, "xml/xsl/AttributesGetter.xsl");
		TransformerPool.getInstance(url2, false, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
	}

}
