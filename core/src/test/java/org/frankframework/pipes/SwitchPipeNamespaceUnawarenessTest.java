package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeRunResult;
import org.frankframework.util.TransformerPoolNamespaceUnawarenessTest;

public class SwitchPipeNamespaceUnawarenessTest extends PipeTestBase<SwitchPipe> {

	public static final String NAMESPACE_UNAWARE_XSLT1_RESULT_7_5678= "1";
	public static final String XSLT2_XPATH_RESULT_7_0 = "NF";
	public static final String XSLT2_XPATH_RESULT_7_5678 = "1";

	@Override
	public SwitchPipe createPipe() {
		return new SwitchPipe();

	}

	public void testSwitch(String input, String expectedForwardName) throws Exception {
		log.debug("inputfile [{}]", input);
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
		pipe.addForward(new PipeForward(XSLT2_XPATH_RESULT_7_5678,"FixedResult1"));
		pipe.addForward(new PipeForward(XSLT2_XPATH_RESULT_7_0,"FixedResultNF"));
		pipe.setStyleSheetName(TransformerPoolNamespaceUnawarenessTest.NAMESPACELESS_STYLESHEET);
		pipe.setNotFoundForwardName(XSLT2_XPATH_RESULT_7_0);
		pipe.setXsltVersion(xsltVersion);
		pipe.setNamespaceAware(namespaceAware);
		String input= "<root xmlns=\"http://dummy\"><sub>1</sub></root>";
		testSwitch(input,expectedForwardName);
	}

	public void testNamespaceAwarenessWithXpath(int xsltVersion, boolean namespaceAware, String expectedForwardName) throws Exception {
		pipe.addForward(new PipeForward(XSLT2_XPATH_RESULT_7_5678,"FixedResult1"));
		pipe.addForward(new PipeForward(XSLT2_XPATH_RESULT_7_0,"FixedResultNF"));
		pipe.setXpathExpression("/root/sub");
		pipe.setNotFoundForwardName(XSLT2_XPATH_RESULT_7_0);
		pipe.setXsltVersion(xsltVersion);
		pipe.setNamespaceAware(namespaceAware);
		String input= "<root xmlns=\"http://dummy\"><sub>1</sub></root>";
		testSwitch(input,expectedForwardName);
	}

	@Test
	void testNamespaceAwareWithStylesheetXslt2() throws Exception {
		testNamespaceAwarenessWithStylesheet(2, true, XSLT2_XPATH_RESULT_7_0);
	}

	@Test
	void testNamespaceAwareWithXPathXslt2() throws Exception {
		testNamespaceAwarenessWithXpath(2, true, XSLT2_XPATH_RESULT_7_5678); // Will return 1, as Xslt 2.0 stylesheet generated from XPath will ignore namespaces in input, as no namespaceDefs were specified
	}

	@Test
	void testNamespaceUnawareWithStylesheetXslt2() throws Exception {
		testNamespaceAwarenessWithStylesheet(2, false, XSLT2_XPATH_RESULT_7_0); // should not set XsltVersion=2 explicitly if you want a namespace unaware XSLT 1.0 stylesheet transformation.
	}

	@Test
	void testNamespaceUnawareWithXPathXslt2() throws Exception {
		testNamespaceAwarenessWithXpath(2, false, XSLT2_XPATH_RESULT_7_5678);
	}

	@Test
	void testNamespaceAwareWithStylesheetXslt1() throws Exception {
		testNamespaceAwarenessWithStylesheet(1, true, XSLT2_XPATH_RESULT_7_0);
	}

	@Test
	void testNamespaceAwareWithXPathXslt1() throws Exception {
		testNamespaceAwarenessWithXpath(1, true, XSLT2_XPATH_RESULT_7_0);
	}

	@Test
	void testNamespaceUnawareWithStylesheetXslt1() throws Exception {
		testNamespaceAwarenessWithStylesheet(1, false, NAMESPACE_UNAWARE_XSLT1_RESULT_7_5678);
	}

	@Test
	void testNamespaceUnawareWithXPathXslt1() throws Exception {
		testNamespaceAwarenessWithXpath(1, false, NAMESPACE_UNAWARE_XSLT1_RESULT_7_5678);
	}

	@Test
	void testNamespaceAwareWithStylesheetXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithStylesheet(0, true, XSLT2_XPATH_RESULT_7_0);
	}

	@Test
	void testNamespaceAwareWithXPathXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithXpath(0, true, XSLT2_XPATH_RESULT_7_5678);  // Will return 1, as Xslt 2.0 stylesheet generated from XPath will ignore namespaces in input, as no namespaceDefs were specified
	}

	@Test
	void testNamespaceUnawareWithStylesheetXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithStylesheet(0, false, NAMESPACE_UNAWARE_XSLT1_RESULT_7_5678);
	}

	@Test
	void testNamespaceUnawareWithXPathXsltVersionAutoDetect() throws Exception {
		testNamespaceAwarenessWithXpath(0, false, XSLT2_XPATH_RESULT_7_5678);
	}

}
