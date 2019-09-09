package nl.nn.adapterframework.senders;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.ParameterResolutionContext;
import nl.nn.adapterframework.testutil.TestFileUtils;

import org.junit.Test;

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
		String input=getFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=getFile("/Xslt/duplicateImport/out.xml");

		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String result = sender.sendMessage(null, input, prc);

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}

	@Test
	public void basicXslt2() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setStyleSheetName("/Xslt/duplicateImport/root.xsl");
		sender.configure();
		sender.open();
		String input=getFile("/Xslt/duplicateImport/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected=getFile("/Xslt/duplicateImport/out.xml");

		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String result = sender.sendMessage(null, input, prc);

		assertEquals(expected.replaceAll("\\s",""), result.replaceAll("\\s",""));
	}

	@Test
	public void testSimpleXslt1Xpath() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("result");
		sender.setXsltVersion(1);
		sender.configure();
		sender.open();

		String input = "<result>dummy</result>";
		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String result = sender.sendMessage(null, input, prc);

		assertEquals("dummy", result);
	}

	@Test
	public void testSimpleXslt2Xpath() throws SenderException, TimeOutException, ConfigurationException, IOException {
		sender.setXpathExpression("result");
		sender.configure();
		sender.open();

		String input = "<result>dummy</result>";
		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String result = sender.sendMessage(null, input, prc);

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

		String input = "<results><result name='test1'>dummy1</result><result name='test2'>dummy2</result></results>";
		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		String result = sender.sendMessage(null, input, prc);

		assertEquals("2", result);
	}

	@Test
	public void testDynamicStylesheet() throws ConfigurationException, IOException, PipeRunException, PipeStartException, SenderException {
		sender.setStyleSheetName("/Xslt/dynamicStylesheet/wrongDummy.xsl");
		sender.configure();
		sender.open();

		session = new PipeLineSessionBase();
		session.put("stylesheetName", "/Xslt/dynamicStylesheet/correctDummy.xsl");
		sender.setStyleSheetNameSessionKey("stylesheetName");

		String input = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/in.xml");
		log.debug("inputfile ["+input+"]");
		String expected = TestFileUtils.getTestFile("/Xslt/dynamicStylesheet/out.txt");

		ParameterResolutionContext prc = new ParameterResolutionContext(input, session);
		assertEquals(expected, sender.sendMessage(null, input, prc));
	}
}
