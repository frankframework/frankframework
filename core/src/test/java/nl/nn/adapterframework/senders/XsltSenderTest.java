package nl.nn.adapterframework.senders;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;
import nl.nn.adapterframework.util.TransformerPoolNamespaceUnawarenessTest;

public class XsltSenderTest extends SenderTestBase<XsltSender> {

	private String NAMESPACE_UNAWARENESS_STYLESHEET = TransformerPoolNamespaceUnawarenessTest.NAMESPACELESS_STYLESHEET;
	private String NAMESPACE_UNAWARENESS_XPATH = TransformerPoolNamespaceUnawarenessTest.NAMESPACELESS_XPATH;
	private String NAMESPACE_UNAWARENESS_INPUT = TransformerPoolNamespaceUnawarenessTest.NAMESPACED_INPUT_MESSAGE;
	private String NAMESPACE_UNAWARE_EXPECTED_RESULT = TransformerPoolNamespaceUnawarenessTest.NAMESPACE_INSENSITIVE_RESULT;
	private String NAMESPACE_COMPLIANT_RESULT = TransformerPoolNamespaceUnawarenessTest.NAMESPACE_COMPLIANT_RESULT;
	private String NAMESPACE_UNAWARE_EXPECTED_FIRST_RESULT = TransformerPoolNamespaceUnawarenessTest.NAMESPACE_INSENSITIVE_FIRST_RESULT;


	@Override
	public XsltSender createSender() {
		return new XsltSender();
	}

	@Test
	public void duplicateParameters() {
		sender.setXpathExpression("*");
		sender.addParameter(new Parameter("a","value1"));
		sender.addParameter(new Parameter("a","value2"));
		sender.addParameter(new Parameter("b","value1"));
		sender.addParameter(new Parameter("b","value2"));

		ConfigurationException e = assertThrows(ConfigurationException.class, sender::configure);

		assertThat(e.getMessage(), containsString("Duplicate parameter names [a, b]"));
	}

	@Test
	public void basicXslt1() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}

	@Test
	public void basicXslt2() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}

	@Test
	public void testSimpleXslt1Xpath() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setXpathExpression("result");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		Message input = new Message("<result>dummy</result>");
		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals("dummy", result);
	}

	@Test
	public void testSimpleXslt2Xpath() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setXpathExpression("result");
		sender.configure();
		sender.open();

		Message input = new Message("<result>dummy</result>");
		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals("dummy", result);
	}

	//This xPath only runs on xslt version 1
	@Test
	public void testComplexXslt1Xpath() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))");
		sender.setOutputType(OutputType.TEXT);
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		Message input = new Message("<results><result name='test1'>dummy1</result><result name='test2'>dummy2</result></results>");
		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals("2", result);
	}

	@Test
	public void testXpathWithNamespaceXslt1() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setXpathExpression("//x:directoryUrl");
		sender.setNamespaceDefs("x=http://studieData.nl/schema/edudex/directory");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/XPathWithNamespace/input.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/XPathWithNamespace/expected-xslt1.txt");
		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected.trim(), result.trim());
	}

	@Test
	public void testXpathWithNamespaceXslt2() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setXpathExpression("//directoryUrl");
		sender.configure();
		sender.open();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/XPathWithNamespace/input.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/XPathWithNamespace/expected-xslt2.txt");
		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected.trim(), result.trim());
	}

	@Test
	public void testDynamicStylesheet() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		sender.setStyleSheetName("/Xslt/dynamicStylesheet/wrongDummy.xsl");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSession();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessageOrThrow(input, session).asString());
	}

	@Test
	public void testDynamicStylesheetWithoutDefault() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSession();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessageOrThrow(input, session).asString());
	}

	@Test
	public void useDefaultStylesheetWithEmptySessionKey() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		sender.setStyleSheetName("/Xslt/dynamicStylesheet/correctDummy.xsl");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSession();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessageOrThrow(input, session).asString());
	}

	@Test
	public void noStylesheetOrXpathOrSessionKeyGiven() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		exception.expectMessage("one of xpathExpression, styleSheetName or styleSheetNameSessionKey must be specified");
		sender.configure();
		sender.open();

		session = new PipeLineSession();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessageOrThrow(input, session).asString());
	}

	@Test
	public void stylesheetSessionKeyAndXpathGiven() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		sender.setXpathExpression("result");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSession();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessageOrThrow(input, session).asString());
	}

	@Test
	public void useDefaultXpathWithEmptySessionKey() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		sender.setXpathExpression("result");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSession();

		Message input = new Message("<result>dummy</result>");
		String result = sender.sendMessageOrThrow(input, session).asString();

		assertEquals("dummy", result);
	}

	@Test
	public void nonexistingStyleSheet() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeoutException {
		exception.expectMessage("cannot find [/Xslt/dynamicStylesheet/nonexistingDummy.xsl]");
		sender.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSession();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/nonexistingDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessageOrThrow(input, session).asString());
	}

	@Ignore("First have to fix this")
	@Test
	public void testNamespaceUnaware() throws SenderException, TimeoutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/NamespaceUnaware/FileInfoNamespaceUnAware.xsl");
		sender.setRemoveNamespaces(true);
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/NamespaceUnaware/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/NamespaceUnaware/out.xml");

		String actual = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected, actual);
	}

	@Ignore("First have to fix this")
	@Test
	public void testNamespaceAware() throws Exception {
		sender.setStyleSheetName("/Xslt/NamespaceUnaware/FileInfoNamespaceAware.xsl");
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/NamespaceUnaware/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/NamespaceUnaware/out.xml");

		String actual = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected, actual);
//		Diff diff = XMLUnit.compareXML(expected, actual);
//		assertTrue(diff.toString(), diff.similar());
	}

	@Test
	public void testNamespaceAwareWithXpath() throws Exception {
		testNamespaceUnawareness(true, NAMESPACE_UNAWARE_EXPECTED_RESULT, true);
	}

	@Test
	public void testNotNamespaceAwareWithXpath() throws Exception {
		// I would like to keep the namespaces in the result xml, but that is not possible anymore.
		testNamespaceUnawareness(false, NAMESPACE_UNAWARE_EXPECTED_RESULT, true);
	}

	@Test
	public void testNamespaceAwareWithStyleSheet() throws Exception {
		testNamespaceUnawareness(true, NAMESPACE_COMPLIANT_RESULT, false);
	}

	@Test
	public void testNotNamespaceAwareWithStyleSheet() throws Exception {
		testNamespaceUnawareness(false, NAMESPACE_UNAWARE_EXPECTED_FIRST_RESULT, false);
	}

	public void testNamespaceUnawareness(boolean namespaceAware, String expected, boolean usingXpath) throws Exception {
		if(usingXpath) {
			sender.setXpathExpression(NAMESPACE_UNAWARENESS_XPATH);
		} else {
			sender.setStyleSheetName(NAMESPACE_UNAWARENESS_STYLESHEET);
		}
		sender.setIndentXml(true);
		sender.setOutputType(OutputType.TEXT);
		sender.setOmitXmlDeclaration(true);
		sender.setNamespaceAware(namespaceAware);
		sender.configure();
		sender.open();
		Message input=new Message(NAMESPACE_UNAWARENESS_INPUT);

		String actual = sender.sendMessageOrThrow(input, session).asString();

		assertEquals(expected, actual);
	}

}
