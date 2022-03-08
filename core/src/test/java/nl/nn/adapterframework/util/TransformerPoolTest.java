package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.xml.transform.Source;

import org.apache.logging.log4j.Logger;
import org.junit.Test;

import nl.nn.adapterframework.core.Resource;

public class TransformerPoolTest {
	protected Logger log = LogUtil.getLogger(this);

	private String xml = "<root><message active=\"false\">hello</message></root>";
	private String xpath = "root/message";
	private String expectedXpath = "hello";

	private String stylesheetURL = "xml/xsl/active.xsl";
	private String expectedStylesheetURL = "<root/>";

	@Test
	public void plainXPath() throws Exception {
		String xpathEvaluatorSource = XmlUtils.createXPathEvaluatorSource(xpath);
		TransformerPool transformerPool = TransformerPool.getInstance(xpathEvaluatorSource);
		String result = transformerPool.transform(xml, null);
		assertEquals(expectedXpath, result);
	}

	@Test
	public void plainXPathUsingMultiUseSource() throws Exception {
		String xpathEvaluatorSource = XmlUtils.createXPathEvaluatorSource(xpath);
		TransformerPool transformerPool = TransformerPool.getInstance(xpathEvaluatorSource);
		Source source = XmlUtils.stringToSource(xml);
		String result = transformerPool.transform(source);
		assertEquals(expectedXpath, result);
	}

	@Test
	public void plainViaUrl() throws Exception {
		Resource resource = Resource.getResource(stylesheetURL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		String result = transformerPool.transform(xml, null);
		result=result.replaceAll("[\n\r]", "");		
		assertEquals(expectedStylesheetURL, result);
	}

	@Test
	public void testGetConfigMap() throws Exception {
		Resource resource = Resource.getResource(stylesheetURL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		Map<String,String> configMap = transformerPool.getConfigMap();
		
		assertEquals("{stylesheet-version=2.0, output-method=xml, output-indent=yes, output-omit-xml-declaration=yes, disable-output-escaping=no}", configMap.toString());
		assertFalse(transformerPool.getDisableOutputEscaping());
		assertTrue(transformerPool.getIndent());
	}
	
}
