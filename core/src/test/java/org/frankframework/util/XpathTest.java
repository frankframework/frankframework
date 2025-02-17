package org.frankframework.util;

import javax.xml.transform.Source;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.parameters.ParameterList;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.TransformerPool.OutputType;

public class XpathTest extends FunctionalTransformerPoolTestBase {

	private final String inputMessageWithNs = "<root xmlns=\"urn:rootnamespace/\"><body xmlns=\"urn:bodynamespace/\"><item>1</item><item>2</item></body></root>";
	private final String inputMessageWithoutNs = "<root><body><item>1</item><item>2</item></body></root>";
	private final String inputMessageMultipleChildren =
		"""
		<root>\
		<subDirectory>\
		<directoryUrl>aa</directoryUrl>\
		<orgUnitId>ab</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>ba</directoryUrl>\
		<orgUnitId>bb</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>ca</directoryUrl>\
		<orgUnitId>cb</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>da</directoryUrl>\
		<orgUnitId>db</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>ea</directoryUrl>\
		<orgUnitId>eb</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>fa</directoryUrl>\
		<orgUnitId>fb</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>ga</directoryUrl>\
		<orgUnitId>gb</orgUnitId>\
		</subDirectory>\
		<subDirectory>\
		<directoryUrl>ha</directoryUrl>\
		<orgUnitId>hb</orgUnitId>\
		</subDirectory>\
		</root>\
		""";

	public void xpathTest(String input, String xpath, String expected) throws Exception {
		xpathTest(input, null, xpath, expected);
	}
	public void xpathTest(String input, String namespaceDefs, String xpath, String expected) throws Exception {
		xpathTest(input, namespaceDefs, xpath, OutputType.TEXT, expected);
	}
	public void xpathTest(String input, String namespaceDefs, String xpath, OutputType outputType, String expected)  throws Exception {
		boolean includeXmlDeclaration=false;
		ParameterList formalParams=null;
		TransformerPool tp= TransformerPool.getXPathTransformerPool(namespaceDefs, xpath, outputType, includeXmlDeclaration, formalParams);
		testTransformerPool(tp, input, expected, "viaString");
		Source source = XmlUtils.stringToSource(input, true);
		testTransformerPool(tp, source, expected, "viaSource");
	}


	@Test
	public void testGetElementName() throws Exception {
		xpathTest(inputMessageWithoutNs, "name(/*)", "root");
	}

	@Test
	public void testGetSubElementName() throws Exception {
		xpathTest(inputMessageWithoutNs, "name(/root/*)", "body");
	}

	@Test
	public void testGetElementContentText() throws Exception {
		xpathTest(inputMessageWithoutNs, null, "/root/body/item[1]", OutputType.TEXT, "1");
	}

	@Test
	public void testGetElementContentXml() throws Exception {
		xpathTest(inputMessageWithoutNs, null, "/root/body/item[1]", OutputType.XML, "<item>1</item>");
	}

	@Test
	public void testGetElementContentXmlValue() throws Exception {
		xpathTest(inputMessageWithoutNs, null, "/root/body/item[1]/text()", OutputType.TEXT, "1");
		xpathTest(inputMessageWithoutNs, null, "/root/body/item[1]/text()", OutputType.XML, "1");
	}

	@Test
	public void testXpathNamespacedInput() throws Exception {
		xpathTest(inputMessageWithNs, "name(/*)", "root");
	}

	@Test
	public void testXpathNoNamespaceInputXpath1() throws Exception {
		xpathTest(inputMessageWithoutNs, "sum(root/*/item)", "3");
	}

	@Test
	public void testXpathNamespacedInputXpath1() throws Exception {
		xpathTest(inputMessageWithNs, "sum(root/*/item)", "3");
	}

	@Test
	public void testXpathNoNamespaceInputXpath2() throws Exception {
		xpathTest(inputMessageWithoutNs, "avg(root/*/item)", "1.5");
	}

	@Test
	public void testXpathNamespacedInputXpath2() throws Exception {
		xpathTest(inputMessageWithNs, "avg(root/*/item)", "1.5");
	}

	@Test
	public void testNamespacedXpathNamespacedInput1() throws Exception {
		xpathTest(inputMessageWithNs, "r=urn:rootnamespace/,b=urn:bodynamespace/", "sum(r:root/b:body/b:item)", "3");
	}

	@Test
	public void testNamespacedXpathNamespacedInput2() throws Exception {
		xpathTest(inputMessageWithNs, "r=urn:rootnamespace,b=urn:bodynamespace", "sum(r:root/r:body/r:item)", "0");
	}

	@Test
	public void testNamespacedXpathNamespacedInput3() throws Exception {
		xpathTest(inputMessageWithNs, "r=urn:rootnamespace,b=urn:bodynamespace", "sum(root/body/item)", "0");
	}

	@Test
	public void testXpathXmlSwitchCase1() throws Exception {
		String input= TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		String expression="name(/Envelope/Body/*[name()!='MessageHeader'])";
		String expected="SetRequest";
		xpathTest(input,expression,expected);
	}

	@Test
	@DisplayName("use xPath to convert a larger xml, omit the xml elements, and just retrieve text")
	public void testXpathWithXml() throws Exception {
		xpathTest(inputMessageMultipleChildren, "root/subDirectory[position()>1 and position()<6]", "babb cacb dadb eaeb");
	}
}
