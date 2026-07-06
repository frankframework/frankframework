package org.frankframework.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.xml.transform.Source;

import org.junit.jupiter.api.Test;

import org.frankframework.core.Resource;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;

public class TransformerPoolTest {

	private static final String XML = "<root><message authAliasTest=\"false\">hello</message></root>";
	private static final String XPATH = "root/message";
	private static final String EXPECTED_XPATH = "hello";

	private static final String STYLESHEET_URL = "xml/xsl/authAlias.xsl";

	private static final String TRANSFORM_URL = "Xslt/xslWithTransformRootTag.xslt";

	@Test
	public void plainXPath() throws Exception {
		TransformerPool transformerPool = TransformerPool.getXPathTransformerPool(XPATH, XmlUtils.DEFAULT_XSLT_VERSION);
		String result = transformerPool.transformToString(XML);
		assertEquals(EXPECTED_XPATH, result);
	}

	@Test
	public void plainXPathUsingMultiUseSource() throws Exception {
		TransformerPool transformerPool = TransformerPool.getXPathTransformerPool(XPATH, XmlUtils.DEFAULT_XSLT_VERSION);
		Source source = XmlUtils.stringToSource(XML);
		String result = transformerPool.transformToString(source);
		assertEquals(EXPECTED_XPATH, result);
	}

	@Test
	public void plainViaUrl() throws Exception {
		Resource resource = Resource.getResource(STYLESHEET_URL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		String result = transformerPool.transformToString(XML);
		result=result.replaceAll("[\n\r]", "");
		assertEquals("<authEntries>   <entry alias=\"false\"/></authEntries>", result);
	}

	@Test
	public void testGetConfigMapWithStylesheet() throws Exception {
		Resource resource = Resource.getResource(STYLESHEET_URL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		Map<String,String> configMap = transformerPool.getConfigMap();

		assertEquals("{version=2.0, output-method=xml, output-indent=yes, output-omit-xml-declaration=yes, disable-output-escaping=no}", configMap.toString());
		assertFalse(transformerPool.getDisableOutputEscaping());
		assertTrue(transformerPool.getIndent());
		assertNull(transformerPool.getOutputEncoding());
	}

	@Test
	public void testGetConfigMapWithCustomOutputEncoding() throws Exception {
		Resource resource = Resource.getResource("/xml/xsl/xslt-with-output-charset.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		Map<String,String> configMap = transformerPool.getConfigMap();

		assertEquals("{version=2.0, output-encoding=ISO-8859-1, output-method=xml, output-indent=yes, output-omit-xml-declaration=yes, disable-output-escaping=no}", configMap.toString());
		assertFalse(transformerPool.getDisableOutputEscaping());
		assertTrue(transformerPool.getIndent());
		assertEquals(StandardCharsets.ISO_8859_1, transformerPool.getOutputEncoding());
	}

	@Test
	public void testGetConfigMapWithTransform() throws Exception {
		Resource resource = Resource.getResource(TRANSFORM_URL);
		TransformerPool transformerPool = TransformerPool.getInstance(resource);
		Map<String,String> configMap = transformerPool.getConfigMap();

		assertEquals("{version=2.0, disable-output-escaping=no}", configMap.toString());
		assertFalse(transformerPool.getDisableOutputEscaping());
		assertNull(transformerPool.getIndent());
	}

	@Test
	public void testTransformWithOutputEncoding() throws Exception {

		// NB: This test is very similar to org.frankframework.xslt.XsltPipeTest.testOutputEncodingWithTextOutput_UTF8_Input and uses the same stylesheet, input and output

		// Arrange
		Resource resource = Resource.getResource("/Xslt/ISO-8859-1/output-encoding-with-text-output.xsl");
		TransformerPool transformerPool = TransformerPool.getInstance(resource);

		assertEquals(StandardCharsets.ISO_8859_1, transformerPool.getOutputEncoding());

		URL url = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/utf-8.xml");
		assertNotNull(url);
		Message inputMessage = new UrlMessage(url, StandardCharsets.UTF_8);

		URL expectedUrl = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/iso-8859-1.txt");
		assertNotNull(expectedUrl);
		UrlMessage expectedMessage = new UrlMessage(expectedUrl, StandardCharsets.ISO_8859_1);
		byte[] expectedBytes = expectedMessage.asByteArray();
		String expected = expectedMessage.asString();

		// Act
		Message result = transformerPool.transform(inputMessage);

		// Assert

		// Retrieve bytes before getting string, to be absolutely sure we get the raw output bytes from the XSLT conversion
		byte[] resultBytes = result.asByteArray();

		String resultString = result.asString();
		assertEquals(expected, resultString);

		// Check actual bytes produced to be really sure the output is in the desired charset
		assertArrayEquals(expectedBytes, resultBytes);

		assertEquals("ISO-8859-1", result.getCharset());
	}
}
