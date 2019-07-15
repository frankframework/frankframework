package nl.nn.adapterframework.util;

import java.io.IOException;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XpathTest extends FunctionalTransformerPoolTestBase {

	private String inputMessageWithNs="<root xmlns=\"urn:rootnamespace/\"><body xmlns=\"urn:bodynamespace/\"><item>1</item><item>2</item></body></root>";
	private String inputMessageWithoutNs="<root><body><item>1</item><item>2</item></body></root>";
	private String inputMessageMultipleChildren=
			"<root>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>aa</directoryUrl>" + 
			"		<orgUnitId>ab</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>ba</directoryUrl>" + 
			"		<orgUnitId>bb</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>ca</directoryUrl>" + 
			"		<orgUnitId>cb</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>da</directoryUrl>" + 
			"		<orgUnitId>db</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>ea</directoryUrl>" + 
			"		<orgUnitId>eb</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>fa</directoryUrl>" + 
			"		<orgUnitId>fb</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>ga</directoryUrl>" + 
			"		<orgUnitId>gb</orgUnitId>" + 
			"	</subDirectory>" + 
			"	<subDirectory>" + 
			"		<directoryUrl>ha</directoryUrl>" + 
			"		<orgUnitId>hb</orgUnitId>" + 
			"	</subDirectory>" + 
			"</root>";
	
	public void xpathTest(String input, String xpath, String expected) throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		String namespaceDefs=null; 
		xpathTest(input, namespaceDefs, xpath, "text", expected);
	}
	public void xpathTest(String input, String namespaceDefs, String xpath, String expected) throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(input, namespaceDefs, xpath, "text", expected);
	}
	public void xpathTest(String input, String namespaceDefs, String xpath, String outputType, String expected)  throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		boolean includeXmlDeclaration=false;
		ParameterList formalParams=null;
		TransformerPool tp= XmlUtils.getXPathTransformerPool(namespaceDefs, xpath, outputType, includeXmlDeclaration, formalParams);
		testTransformerPool(tp, input, expected, false, "viaString");
		Source source = XmlUtils.stringToSource(input,false);
		testTransformerPool(tp, source, expected, false, "viaSource");
	}
	
	
	@Test
	public void testXpathNoNamespaceInput() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithoutNs,"name(/*)","root");
	}
	@Test
	public void testXpathNamespacedInput() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithNs,"name(/*)","root");
	}
	
	@Test
	public void testXpathNoNamespaceInputXpath1() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithoutNs,"sum(root/*/item)","3");
	}
	@Test
	public void testXpathNamespacedInputXpath1() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithNs,"sum(root/*/item)","3");
	}

	@Test
	public void testXpathNoNamespaceInputXpath2() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithoutNs,"avg(root/*/item)","1.5");
	}
	@Test
	public void testXpathNamespacedInputXpath2() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithNs,"avg(root/*/item)","1.5");
	}
	
	@Test
	public void testNamespacedXpathNamespacedInput1() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithNs,"r=urn:rootnamespace/,b=urn:bodynamespace/","sum(r:root/b:body/b:item)","3");
	}
	@Test
	public void testNamespacedXpathNamespacedInput2() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithNs,"r=urn:rootnamespace,b=urn:bodynamespace","sum(r:root/r:body/r:item)","0");
	}
	@Test
	public void testNamespacedXpathNamespacedInput3() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageWithNs,"r=urn:rootnamespace,b=urn:bodynamespace","sum(root/body/item)","0");
	}

	@Test
	public void testXpathXmlSwitchCase1() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		String input=TestFileUtils.getTestFile("/XmlSwitch/in.xml");
		String expression="name(/Envelope/Body/*[name()!='MessageHeader'])";
		String expected="SetRequest";
		xpathTest(input,expression,expected);
	}

	@Test
	public void testXpathWithXmlSpecialChars() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
		xpathTest(inputMessageMultipleChildren, "root/subDirectory[position()>1 and position()<6]", "babb cacb dadb eaeb");
	}
//	@Test
//	public void testXpathWithXmlSpecialCharsEscaped() throws ConfigurationException, DomBuilderException, TransformerException, IOException {
//		xpathTest(inputMessageMultipleChildren, "root/subDirectory[position()&gt;1 and position()&lt;6]", "babb cacb dadb eaeb");
//	}
}
