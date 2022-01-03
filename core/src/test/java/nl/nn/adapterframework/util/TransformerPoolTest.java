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

}
