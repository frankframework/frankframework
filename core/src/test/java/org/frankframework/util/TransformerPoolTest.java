package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import javax.xml.transform.Source;

import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import org.frankframework.core.Resource;

public class TransformerPoolTest {
	protected Logger log = LogUtil.getLogger(this);

	private final String xml = "<root><message authAliasTest=\"false\">hello</message></root>";
	private final String xpath = "root/message";
	private final String expectedXpath = "hello";

	private final String stylesheetURL = "xml/xsl/authAlias.xsl";

	private final String transformURL = "Xslt/xslWithTransformRootTag.xslt";

	@Test
	public void plainXPath() throws Exception {
		TransformerPool transformerPool = TransformerPool.getXPathTransformerPool(xpath, XmlUtils.DEFAULT_XSLT_VERSION);
		String result = transformerPool.transformToString(xml, null);
		assertEquals(expectedXpath, result);
	}

	@Test
	public void plainXPathUsingMultiUseSource() throws Exception {
		TransformerPool transformerPool = TransformerPool.getXPathTransformerPool(xpath, XmlUtils.DEFAULT_XSLT_VERSION);
		Source source = XmlUtils.stringToSource(xml);
		String result = transformerPool.transformToString(source);
		assertEquals(expectedXpath, result);
	}

	@Test
	public void plainViaUrl() throws Exception {
		Resource resource = Resource.getResource(stylesheetURL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		String result = transformerPool.transformToString(xml, null);
		result=result.replaceAll("[\n\r]", "");
		assertEquals("<authEntries>   <entry alias=\"false\"/></authEntries>", result);
	}

	@Test
	public void testGetConfigMapWithStylesheet() throws Exception {
		Resource resource = Resource.getResource(stylesheetURL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		Map<String,String> configMap = transformerPool.getConfigMap();

		assertEquals("{version=2.0, output-method=xml, output-indent=yes, output-omit-xml-declaration=yes, disable-output-escaping=no}", configMap.toString());
		assertFalse(transformerPool.getDisableOutputEscaping());
		assertTrue(transformerPool.getIndent());
	}

	@Test
	public void testGetConfigMapWithTransform() throws Exception {
		Resource resource = Resource.getResource(transformURL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		Map<String,String> configMap = transformerPool.getConfigMap();

		assertEquals("{version=2.0, disable-output-escaping=no}", configMap.toString());
		assertFalse(transformerPool.getDisableOutputEscaping());
		assertNull(transformerPool.getIndent());
	}
}
