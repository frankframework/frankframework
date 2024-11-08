package org.frankframework.xslt;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.util.Properties;

import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.configuration.ConfigurationWarnings;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterType;
import org.frankframework.pipes.FixedForwardPipe;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.XmlParameterBuilder;
import org.frankframework.util.TransformerPool.OutputType;

public abstract class XsltTestBase<P extends FixedForwardPipe> extends PipeTestBase<P> {

	public static final String IDENTITY_STYLESHEET = "/Xslt/identity.xslt";

	protected abstract void setXpathExpression(String xpathExpression);

	protected abstract void setStyleSheetName(String styleSheetName);

	protected abstract void setStyleSheetNameSessionKey(String styleSheetNameSessionKey);

	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);

	protected abstract void setIndent(boolean indent);

	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);

	protected abstract void setRemoveNamespaces(boolean removeNamespaces);

	protected abstract void setXsltVersion(int xsltVersion);

	protected abstract void setOutputType(OutputType outputType);

	protected abstract void setHandleLexicalEvents(boolean handleLexicalEvents);


	@BeforeEach
	@Override
	public void setUp() throws Exception {
		session = new PipeLineSession();
		super.setUp();
	}

	protected void assertResultsAreCorrect(String expected, String actual, PipeLineSession session) {
		assertEquals(expected, actual);
	}

	protected void testXslt(String styleSheetName, String input, String expected, Boolean omitXmlDeclaration, Boolean indent, Boolean skipEmptyTags, Boolean removeNamespaces, Boolean xslt2) throws Exception {
		setStyleSheetName(styleSheetName);
		if (omitXmlDeclaration != null) {
			setOmitXmlDeclaration(omitXmlDeclaration);
		}
		if (indent != null) {
			setIndent(indent);
		}
		if (skipEmptyTags != null) {
			setSkipEmptyTags(skipEmptyTags);
		}
		if (removeNamespaces != null) {
			setRemoveNamespaces(removeNamespaces);
		}
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertResultsAreCorrect(expected, result.trim(), session);
	}

	@Test
	void basic() throws Exception {
		String styleSheetName = "/Xslt3/orgchart.xslt";
		String input = TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration = null;
		Boolean indent = null; // follows indent of stylesheet
		Boolean skipEmptyTags = null;
		Boolean removeNamespaces = null;
		Boolean xslt2 = true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	@Test
	void basicIndent() throws Exception {
		String styleSheetName = "/Xslt3/orgchart.xslt";
		String input = TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration = null;
		Boolean indent = true;
		Boolean skipEmptyTags = null;
		Boolean removeNamespaces = null;
		Boolean xslt2 = true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	@Test
	void basicNoIndent() throws Exception {
		String styleSheetName = "/Xslt3/orgchart.xslt";
		String input = TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/orgchart-noindent.xml");
		Boolean omitXmlDeclaration = null;
		Boolean indent = false;
		Boolean skipEmptyTags = null;
		Boolean removeNamespaces = null;
		Boolean xslt2 = true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	/*
	 * Beware, this test could fail when run multithreaded
	 */
	@Test
	void testConfigWarnings() throws ConfigurationException {
		ConfigurationWarnings warnings = getConfigurationWarnings();
		String styleSheetName = "/Xslt3/orgchart.xslt";
		setStyleSheetName(styleSheetName);
		setXsltVersion(1);
		pipe.configure();
		for (int i = 0; i < warnings.size(); i++) {
			System.out.println(i + " " + warnings.get(i));
		}
		assertFalse(warnings.isEmpty(), "Expected at least one config warnings");
		int nextPos = 0;//warnings.size()>4?warnings.size()-2:1;
		assertThat(warnings.get(nextPos), StringContains.containsString("configured xsltVersion [1] does not match xslt version [2] declared in stylesheet"));
		assertThat(warnings.get(nextPos), StringContains.containsString(styleSheetName));
	}

	public void testSkipEmptyTags(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws Exception {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, true, null, null);
	}

	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws Exception {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, null, true, null);
	}

	@Test
	void testSkipEmptyTagsNoOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a></root>", false, false);
	}

	@Test
	void testSkipEmptyTagsNoOmitIndent() throws Exception {
		String lineSeparator = "\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + lineSeparator + "<root>" + lineSeparator + "\t<a>a</a>" + lineSeparator + "</root>", false, true);
	}

	@Test
	void testSkipEmptyTagsOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>", "<root><a>a</a></root>", true, false);
	}

	@Test
	void testSkipEmptyTagsOmitIndent() throws Exception {
		String lineSeparator = "\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>", "<root>" + lineSeparator + "\t<a>a</a>" + lineSeparator + "</root>", true, true);
	}

	@Test
	void testRemoveNamespacesNoOmitNoIndent() throws Exception {
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>", false, false);
	}

	@Test
	@Disabled("Indent appears not to work in combination with Streaming and RemoveNamespaces. Ignore the test for now...")
	void testRemoveNamespacesNoOmitIndent() throws Exception {
		String lineSeparator = System.getProperty("line.separator");
		testRemoveNamespaces("<ns:root xmlns:ns=\"urn:fakenamespace\"><ns:a>a</ns:a><ns:b></ns:b><c/></ns:root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>" + lineSeparator + "\t<a>a</a>" + lineSeparator + "\t<b/>" + lineSeparator + "\t<c/>" + lineSeparator + "</root>", false, true);
	}

	public void testBasic(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws Exception {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, false, null, null);
	}

	@Test
	void testBasicNoOmitNoIndent() throws Exception {
		testBasic("<root><a>a</a><b></b><c/></root>", "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>", false, false);
	}

	@Test
	void documentIncludedInSourceRelativeXslt1() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupRelative1.xsl");
		runPipeAndValidate(1);
	}

	@Test
	void documentIncludedInSourceRelativeXslt2() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupRelative2.xsl");
		runPipeAndValidate(1);
	}

	@Test
	void documentIncludedInSourceRelativeWithDynamicStylesheetXslt1() throws Exception {
		String stylesheetName = "/Xslt/importDocument/importLookupRelative1.xsl";
		session.put("Stylesheet", stylesheetName);
		setStyleSheetNameSessionKey("Stylesheet");
		runPipeAndValidate(1);
	}

	@Test
	void documentIncludedInSourceRelativeWithDynamicStylesheetXslt2() throws Exception {
		String stylesheetname = "/Xslt/importDocument/importLookupRelative1.xsl";
		session.put("Stylesheet", stylesheetname);
		setStyleSheetNameSessionKey("Stylesheet");
		runPipeAndValidate(2);
	}

	@Test
	void documentIncludedInSourceAbsoluteXslt1() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupAbsolute1.xsl");
		runPipeAndValidate(1);
	}

	void runPipeAndValidate(final int xsltVersion) throws Exception {
		setXsltVersion(xsltVersion);
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void documentIncludedInSourceAbsoluteXslt2() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupAbsolute2.xsl");
		setXsltVersion(1);
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void xPathFromParameter() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");

		pipe.addParameter(XmlParameterBuilder.create("source", input).withType(ParameterType.DOMDOC));
		setXpathExpression("$source/request/b");

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "<dummy name=\"input\"/>", session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect("b", result, session);
	}

	@Test
	public void xpathNodeText() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "Euro € single quote ' double quote \"";

		setXpathExpression("request/g");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void xpathAttrText() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "Euro € single quote ' double quote escaped \" newline escaped \n";

		setXpathExpression("request/g/@attr");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void xpathNodeXml() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "<g attr=\"Euro € single quote ' double quote escaped &quot; newline escaped &#10;\">Euro € single quote ' double quote \"</g>";

		setXpathExpression("request/g");
		setOutputType(OutputType.XML);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void anyXmlBasic() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/Escaped2.xml");

		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void anyXmlNoMethodConfigured() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/Escaped.xml");

		setStyleSheetName("/Xslt/AnyXml/CopyNoMethodConfigured.xsl");
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void anyXmlIndent() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrintedEscaped.xml");

		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		setIndent(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void anyXmlAsText() throws Exception {
		Properties prop = System.getProperties();
		String vendor = prop.getProperty("java.vendor");
		System.out.println("JVM Vendor : " + vendor);
		assumeFalse("IBM Corporation".equals(vendor)); // comments are not properly processed in the IBM JDK

		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/AsText.txt");

		setStyleSheetName("/Xslt/AnyXml/CopyAsText.xsl");
		setHandleLexicalEvents(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected.trim(), result.trim(), session); // trim is necessary on IBM JDK
	}

	@Test
	void anyXmlDisableOutputEscaping() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/OutputEscapingDisabled.xml");

		setStyleSheetName("/Xslt/AnyXml/DisableOutputEscaping.xsl");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected.replaceAll("\\s", ""), result.replaceAll("\\s", ""), session);
	}

	@Test
	void skipEmptyTagsXslt1() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/SkipEmptyTagsIndent.xml");

		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setXsltVersion(1);
		setSkipEmptyTags(true);
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		setIndent(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	void skipEmptyTagsXslt2() throws Exception {
		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setXsltVersion(2);
		setSkipEmptyTags(true);
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/SkipEmptyTagsIndent.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();

		assertResultsAreCorrect(expected, result, session);
	}

}
