package org.frankframework.util;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.frankframework.testutil.TestFileUtils;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.parameters.ParameterList;
import org.frankframework.util.TransformerPool.OutputType;

public class XpathTest extends FunctionalTransformerPoolTestBase {

	private final String inputMessageWithNs = "<root xmlns=\"urn:rootnamespace/\"><body xmlns=\"urn:bodynamespace/\"><item>1</item><item>2</item></body></root>";
	private final String inputMessageWithoutNs = "<root><body><item>1</item><item>2</item></body></root>";
	private final String inputMessageMultipleChildren =
		"<root>" +
			"<subDirectory>" +
			"<directoryUrl>aa</directoryUrl>" +
			"<orgUnitId>ab</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>ba</directoryUrl>" +
			"<orgUnitId>bb</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>ca</directoryUrl>" +
			"<orgUnitId>cb</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>da</directoryUrl>" +
			"<orgUnitId>db</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>ea</directoryUrl>" +
			"<orgUnitId>eb</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>fa</directoryUrl>" +
			"<orgUnitId>fb</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>ga</directoryUrl>" +
			"<orgUnitId>gb</orgUnitId>" +
			"</subDirectory>" +
			"<subDirectory>" +
			"<directoryUrl>ha</directoryUrl>" +
			"<orgUnitId>hb</orgUnitId>" +
			"</subDirectory>" +
			"</root>";

	public void xpathTest(String input, String xpath, String expected) throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		String namespaceDefs=null;
		xpathTest(input, namespaceDefs, xpath, OutputType.TEXT, expected);
	}
	public void xpathTest(String input, String namespaceDefs, String xpath, String expected) throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(input, namespaceDefs, xpath, OutputType.TEXT, expected);
	}
	public void xpathTest(String input, String namespaceDefs, String xpath, OutputType outputType, String expected)  throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		boolean includeXmlDeclaration=false;
		boolean namespaceAware=true;
		ParameterList formalParams=null;
		TransformerPool tp= TransformerPool.getXPathTransformerPool(namespaceDefs, xpath, outputType, includeXmlDeclaration, formalParams);
		testTransformerPool(tp, input, expected, namespaceAware, "viaString");
		Source source = XmlUtils.stringToSource(input,namespaceAware);
		testTransformerPool(tp, source, expected, namespaceAware, "viaSource");
	}


	@Test
	public void testXpathNoNamespaceInput() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithoutNs,"name(/*)","root");
	}
	@Test
	public void testXpathNamespacedInput() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithNs,"name(/*)","root");
	}

	@Test
	public void testXpathNoNamespaceInputXpath1() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithoutNs,"sum(root/*/item)","3");
	}
	@Test
	public void testXpathNamespacedInputXpath1() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithNs,"sum(root/*/item)","3");
	}

	@Test
	public void testXpathNoNamespaceInputXpath2() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithoutNs,"avg(root/*/item)","1.5");
	}
	@Test
	public void testXpathNamespacedInputXpath2() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithNs,"avg(root/*/item)","1.5");
	}

	@Test
	public void testNamespacedXpathNamespacedInput1() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithNs,"r=urn:rootnamespace/,b=urn:bodynamespace/","sum(r:root/b:body/b:item)","3");
	}
	@Test
	public void testNamespacedXpathNamespacedInput2() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithNs,"r=urn:rootnamespace,b=urn:bodynamespace","sum(r:root/r:body/r:item)","0");
	}
	@Test
	public void testNamespacedXpathNamespacedInput3() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageWithNs,"r=urn:rootnamespace,b=urn:bodynamespace","sum(root/body/item)","0");
	}

	@Test
	public void testXpathXmlSwitchCase1() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		String input= TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		String expression="name(/Envelope/Body/*[name()!='MessageHeader'])";
		String expected="SetRequest";
		xpathTest(input,expression,expected);
	}

	@Test
	public void testXpathWithXmlSpecialChars() throws ConfigurationException, DomBuilderException, TransformerException, IOException, SAXException {
		xpathTest(inputMessageMultipleChildren, "root/subDirectory[position()>1 and position()<6]", "babb cacb dadb eaeb");
	}
//	@Test
//	public void testXpathWithXmlSpecialCharsEscaped() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
//		xpathTest(inputMessageMultipleChildren, "root/subDirectory[position()&gt;1 and position()&lt;6]", "babb cacb dadb eaeb");
//	}
}
