package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XsltSenderTest extends SenderTestBase<XsltSender> {

	@Override
	public XsltSender createSender() {
		return new XsltSender();
	}

	@Test
	public void basicXslt1() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		String result = sender.sendMessage(input, session).asString();

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}

	@Test
	public void basicXslt2() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/duplicateImport/out.xml");

		String result = sender.sendMessage(input, session).asString();

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}

	@Test
	public void testSimpleXslt1Xpath() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("result");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		Message input = new Message("<result>dummy</result>");
		String result = sender.sendMessage(input, session).asString();

		assertEquals("dummy", result);
	}

	@Test
	public void testSimpleXslt2Xpath() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("result");
		sender.configure();
		sender.open();

		Message input = new Message("<result>dummy</result>");
		String result = sender.sendMessage(input, session).asString();

		assertEquals("dummy", result);
	}

	//This xPath only runs on xslt version 1
	@Test
	public void testComplexXslt1Xpath() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))");
		sender.setOutputType("txt");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		Message input = new Message("<results><result name='test1'>dummy1</result><result name='test2'>dummy2</result></results>");
		String result = sender.sendMessage(input, session).asString();

		assertEquals("2", result);
	}

	@Test
	public void testXpathWithNamespaceXslt1() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("//x:directoryUrl");
		sender.setNamespaceDefs("x=http://studieData.nl/schema/edudex/directory");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/XPathWithNamespace/input.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/XPathWithNamespace/expected-xslt1.txt");
		String result = sender.sendMessage(input, session).asString();

		assertEquals(expected.trim(), result.trim());
	}

	@Test
	public void testXpathWithNamespaceXslt2() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("//directoryUrl");
		sender.configure();
		sender.open();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/XPathWithNamespace/input.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/XPathWithNamespace/expected-xslt2.txt");
		String result = sender.sendMessage(input, session).asString();

		assertEquals(expected.trim(), result.trim());
	}

	@Test
	public void testDynamicStylesheet() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		sender.setStyleSheetName("/Xslt/dynamicStylesheet/wrongDummy.xsl");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessage(input, session).asString());
	}
	
	@Test
	public void testDynamicStylesheetWithoutDefault() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessage(input, session).asString());
	}
	
	@Test
	public void useDefaultStylesheetWithEmptySessionKey() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		sender.setStyleSheetName("/Xslt/dynamicStylesheet/correctDummy.xsl");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessage(input, session).asString());
	}
	
	@Test
	public void noStylesheetOrXpathOrSessionKeyGiven() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		exception.expectMessage("one of xpathExpression, styleSheetName or styleSheetNameSessionKey must be specified");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessage(input, session).asString());
	}
	
	@Test
	public void stylesheetSessionKeyAndXpathGiven() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		sender.setXpathExpression("result");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessage(input, session).asString());
	}
	
	@Test
	public void useDefaultXpathWithEmptySessionKey() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		sender.setXpathExpression("result");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();

		Message input = new Message("<result>dummy</result>");
		String result = sender.sendMessage(input, session).asString();

		assertEquals("dummy", result);
	}
	
	@Test
	public void nonexistingStyleSheet() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException, TimeOutException {
		exception.expectMessage("cannot find [/Xslt/dynamicStylesheet/nonexistingDummy.xsl]");
		sender.setXpathExpression("number(count(/results/result[contains(@name , 'test')]))");
		sender.setStyleSheetNameSessionKey("stylesheetName");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/nonexistingDummy.xsl");

		Message input = TestFileUtils.getTestFileMessage("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		assertEquals(expected, sender.sendMessage(input, session).asString());
	}
	
	@Ignore("First have to fix this")
	@Test
	public void testNamespaceUnaware() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/NamespaceUnaware/FileInfoNamespaceUnAware.xsl");
		sender.setRemoveNamespaces(true);
		sender.configure();
		sender.open();
		Message input=TestFileUtils.getTestFileMessage("/Xslt/NamespaceUnaware/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=TestFileUtils.getTestFile("/Xslt/NamespaceUnaware/out.xml");

		String actual = sender.sendMessage(input, session).asString();

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

		String actual = sender.sendMessage(input, session).asString();

		assertEquals(expected, actual);
//		Diff diff = XMLUnit.compareXML(expected, actual);
//		assertTrue(diff.toString(), diff.similar());
	}

}
