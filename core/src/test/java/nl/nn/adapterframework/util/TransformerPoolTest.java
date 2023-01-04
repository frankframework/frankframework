package nl.nn.adapterframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.xml.transform.Source;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import nl.nn.adapterframework.core.Resource;

public class TransformerPoolTest {
	protected Logger log = LogUtil.getLogger(this);

	private String xml = "<root><message authAliasTest=\"false\">hello</message></root>";
	private String xpath = "root/message";
	private String expectedXpath = "hello";

	private String stylesheetURL = "xml/xsl/authAlias.xsl";

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
		assertEquals("<authEntries>   <entry alias=\"false\"/></authEntries>", result);
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
