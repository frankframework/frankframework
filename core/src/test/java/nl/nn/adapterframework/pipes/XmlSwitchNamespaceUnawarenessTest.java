package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.util.TransformerPoolNamespaceUnawarenessTest;

public class XmlSwitchNamespaceUnawarenessTest extends PipeTestBase<XmlSwitch> {

	public String NAMESPACE_UNAWARE_XSLT1_RESULT_7_0= "1";
	public String NAMESPACE_UNAWARE_XSLT1_RESULT_7_5678= "1";

	private final String namespaceUnaware_Xslt1_result = NAMESPACE_UNAWARE_XSLT1_RESULT_7_5678;

	public String XSLT2_XPATH_RESULT_7_0= "NF";
	public String XSLT2_XPATH_RESULT_7_5678= "1";

	private final String xslt2_XPath_result = XSLT2_XPATH_RESULT_7_5678;

	@Override
	public XmlSwitch createPipe() {
		return new XmlSwitch();

	}

	public void testSwitch(String input, String expectedForwardName) throws Exception {
		log.debug("inputfile ["+input+"]");
		pipe.configure();
		pipe.start();
		PipeRunResult prr = doPipe(input);

		//assertEquals(input,prr.getResult());

		PipeForward forward=prr.getPipeForward();
		assertNotNull(forward);

		String actualForwardName=forward.getName();
		assertEquals(expectedForwardName,actualForwardName);
	}


	public void testNamespaceAwarenessWithStylesheet(int xsltVersion, boolean namespaceAware, String expectedForwardName) throws Exception {
		pipe.registerForward(new PipeForward("1","FixedResult1"));
		pipe.registerForward(new PipeForward("NF","FixedResultNF"));
		pipe.setServiceSelectionStylesheetFilename(TransformerPoolNamespaceUnawarenessTest.NAMESPACELESS_STYLESHEET);
		pipe.setNotFoundForwardName("NF");
		pipe.setXsltVersion(xsltVersion);
		pipe.setNamespaceAware(namespaceAware);
		String input= "<root xmlns=\"http://dummy\"><sub>1</sub></root>";
		testSwitch(input,expectedForwardName);
	}

	public void testNamespaceAwarenessWithXpath(int xsltVersion, boolean namespaceAware, String expectedForwardName) throws Exception {
		pipe.registerForward(new PipeForward("1","FixedResult1"));
		pipe.registerForward(new PipeForward("NF","FixedResultNF"));
		pipe.setXpathExpression("/root/sub");
		pipe.setNotFoundForwardName("NF");
		pipe.setXsltVersion(xsltVersion);
		pipe.setNamespaceAware(namespaceAware);
		String input= "<root xmlns=\"http://dummy\"><sub>1</sub></root>";
		testSwitch(input,expectedForwardName);
	}

	@Test
	public void testNamespaceAwareWithStylesheetXslt2() throws Exception {
		testNamespaceAwarenessWithStylesheet(2, true, "NF");
	}

	@Test
	public void testNamespaceAwareWithXPathXslt2() throws Exception {
		testNamespaceAwarenessWithXpath(2, true, xslt2_XPath_result); // Will return 1, as Xslt 2.0 stylesheet generated from XPath will ignore namespaces in input, as no namespaceDefs were specified
	}

	@Test
	public void testNamespaceUnawareWithStylesheetXslt2() throws Exception {
		testNamespaceAwarenessWithStylesheet(2, false, "NF"); // should not set XsltVersion=2 explicitly if you want a namespace unaware XSLT 1.0 stylesheet transformation.
	}

	@Test
	public void testNamespaceUnawareWithXPathXslt2() throws Exception {
		testNamespaceAwarenessWithXpath(2, false, xslt2_XPath_result);
	}

	@Test
	public void testNamespaceAwareWithStylesheetXslt1() throws Exception {
		testNamespaceAwarenessWithStylesheet(1, true, "NF");
	}

	@Test
	public void testNamespaceAwareWithXPathXslt1() throws Exception {
		testNamespaceAwarenessWithXpath(1, true, "NF");
	}

	@Test
	public void testNamespaceUnawareWithStylesheetXslt1() throws Exception {
		testNamespaceAwarenessWithStylesheet(1, false, namespaceUnaware_Xslt1_result);
	}

	@Test
	public void testNamespaceUnawareWithXPathXslt1() throws Exception {
		testNamespaceAwarenessWithXpath(1, false, namespaceUnaware_Xslt1_result);
	}

	@Test
	public void testNamespaceAwareWithStylesheetXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithStylesheet(0, true, "NF");
	}

	@Test
	public void testNamespaceAwareWithXPathXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithXpath(0, true, xslt2_XPath_result);  // Will return 1, as Xslt 2.0 stylesheet generated from XPath will ignore namespaces in input, as no namespaceDefs were specified
	}

	@Test
	public void testNamespaceUnawareWithStylesheetXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithStylesheet(0, false, namespaceUnaware_Xslt1_result);
	}

	@Test
	public void testNamespaceUnawareWithXPathXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithXpath(0, false, "1");
	}

}
