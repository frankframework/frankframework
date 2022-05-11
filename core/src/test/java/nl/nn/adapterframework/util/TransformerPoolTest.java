package nl.nn.adapterframework.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.xml.transform.Source;

import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.xml.sax.ContentHandler;

import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.xml.XmlWriter;

public class TransformerPoolTest {
	protected Logger log = LogUtil.getLogger(this);

	private String xml = "<root><message active=\"false\">hello</message></root>";
	private String xpath = "root/message";
	private String expectedXpath = "hello";

	private String stylesheetURL = "xml/xsl/active.xsl";
	private String expectedStylesheetURL = "<root/>";

	public static String NAMESPACED_INPUT_MESSAGE="<root><sub xmlns=\"http://dummy\">+</sub><sub>-</sub></root>";
	public static String NAMESPACELESS_XPATH="/root/sub";
	public static String NAMESPACELESS_STYLESHEET="/Util/TransformerPool/NamespacelessStylesheet.xsl";
	public static String NAMESPACE_INSENSITIVE_RESULT= "+ -";
	public static String NAMESPACE_COMPLIANT_RESULT= "-";
	public static String NAMESPACE_INSENSITIVE_FIRST_RESULT= "+";

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

	public void testNamespaceInsensitiveTransformation(String xpath, String stylesheet, int xsltVersion, boolean namespaceAware, String expectedResult) throws Exception {
		TransformerPool tp = TransformerPool.configureTransformer0("transformerpool test", null, null, xpath, stylesheet, OutputType.TEXT, false, null, xsltVersion);

		assertEquals(expectedResult, tp.transform(NAMESPACED_INPUT_MESSAGE, null, namespaceAware));
		assertEquals(expectedResult, tp.transform(new Message(NAMESPACED_INPUT_MESSAGE), null, namespaceAware));

		XmlWriter writer = new XmlWriter();
		writer.setTextMode(true);
		ContentHandler handler = tp.getTransformerFilter(null, writer, !namespaceAware);
		XmlUtils.parseXml(NAMESPACED_INPUT_MESSAGE, handler);

		assertEquals(expectedResult, writer.toString());
	}

	public void testNamespaceInsensitiveStylesheetTransformation(int xsltVersion, boolean namespaceAware, String expectedResult) throws Exception {
		testNamespaceInsensitiveTransformation(null, NAMESPACELESS_STYLESHEET, xsltVersion, namespaceAware, expectedResult);
	}

	public void testNamespaceInsensitiveXPathTransformation(int xsltVersion, boolean namespaceAware, String expectedResult) throws Exception {
		testNamespaceInsensitiveTransformation(NAMESPACELESS_XPATH, null, xsltVersion, namespaceAware, expectedResult);
	}


	@Test
	public void testNamespaceInsensitiveXPathXslt2AndAuto() throws Exception {
		testNamespaceInsensitiveXPathTransformation(0, false, NAMESPACE_INSENSITIVE_RESULT);
		testNamespaceInsensitiveXPathTransformation(2, false, NAMESPACE_INSENSITIVE_RESULT);
		testNamespaceInsensitiveXPathTransformation(0, true, NAMESPACE_INSENSITIVE_RESULT);
		testNamespaceInsensitiveXPathTransformation(2, true, NAMESPACE_INSENSITIVE_RESULT);
	}

	@Test
	public void testNamespaceInsensitiveXPathXslt1NamespaceAware() throws Exception {
		testNamespaceInsensitiveXPathTransformation(1, true, NAMESPACE_COMPLIANT_RESULT); // for XSLT 1 and namespaceAware (explicitly) put on, the transformation is not namespace insensitive
	}

	@Test
	public void testNamespaceInsensitiveXPathXslt1NamespaceUnaware() throws Exception {
		testNamespaceInsensitiveXPathTransformation(1, false, "+"); // under XPath 1.0, only the value of the first node in the matched nodeset is returned
	}



	@Test
	public void testNamespaceInsensitiveStyleSheetXslt2AndAutoNamespaceAware() throws Exception {
		testNamespaceInsensitiveStylesheetTransformation(0, true, NAMESPACE_COMPLIANT_RESULT); // if namespaceaware is set, then transformation is expected to follow namespace rules
		testNamespaceInsensitiveStylesheetTransformation(2, true, NAMESPACE_COMPLIANT_RESULT); // if namespaceaware is set, then transformation is expected to follow namespace rules
	}

	@Test
	public void testNamespaceInsensitiveStyleSheetXslt1NamespaceAware() throws Exception {
		testNamespaceInsensitiveStylesheetTransformation(1, true, NAMESPACE_COMPLIANT_RESULT); // for XSLT 1 and namespaceAware (explicitly) put on, the transformation is not namespace insensitive
	}

	@Test
	public void testNamespaceInsensitiveStyleSheetXslt2AndAutoNamespaceUnaware() throws Exception {
		testNamespaceInsensitiveStylesheetTransformation(0, false, NAMESPACE_INSENSITIVE_FIRST_RESULT); // under XPath 1.0, only the first match is returned
		//testNamespaceInsensitiveStylesheetTransformation(2, false, NAMESPACE_INSENSITIVE_FIRST_RESULT); // should not set XsltVersion=2 explicitly if you want a namespace unaware XSLT 1.0 stylesheet transformation.
	}

	@Test
	public void testNamespaceInsensitiveStyleSheetXslt1NamespaceUnaware() throws Exception {
		testNamespaceInsensitiveStylesheetTransformation(1, false, NAMESPACE_INSENSITIVE_FIRST_RESULT); // under XPath 1.0, only the value of the first node in the matched nodeset is returned
	}

}
