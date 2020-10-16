package nl.nn.adapterframework.util;

import nl.nn.adapterframework.core.Resource;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import javax.xml.transform.Source;

import static org.junit.Assert.assertEquals;

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
		String result = transformerPool.transform(source);
		assertEquals(expectedXpath, result);
		assertEquals(0, TransformerPool.getTransformerPoolsKeys().size());
	}

	@Test
	public void plainViaUrl() throws Exception {
		TransformerPool.clearTransformerPools();
		Resource resource = Resource.getResource(stylesheetURL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
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
		Resource resource = Resource.getResource("xml/xsl/active.xsl");
		TransformerPool.getInstance(resource, 1, true);
		assertEquals(1, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(resource, 2, true);
		assertEquals(2, TransformerPool.getTransformerPoolsKeys().size());
		TransformerPool.getInstance(resource, 1, true);
		assertEquals(2, TransformerPool.getTransformerPoolsKeys().size());
		Resource resource2 = Resource.getResource("xml/xsl/AttributesGetter.xsl");
		TransformerPool.getInstance(resource2, 2, true);
		assertEquals(3, TransformerPool.getTransformerPoolsKeys().size());
	}

}
