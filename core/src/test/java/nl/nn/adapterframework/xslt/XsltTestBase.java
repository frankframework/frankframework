package nl.nn.adapterframework.xslt;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeThat;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.pipes.FixedForwardPipe;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

public abstract class XsltTestBase<P extends FixedForwardPipe> extends PipeTestBase<P> {

	public static final String IDENTITY_STYLESHEET="/Xslt/identity.xslt";

	protected abstract void setXpathExpression(String xpathExpression);
	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setStyleSheetNameSessionKey(String styleSheetNameSessionKey);
	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);
	protected abstract void setIndent(boolean indent);
	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);
	protected abstract void setRemoveNamespaces(boolean removeNamespaces);
	protected abstract void setOutputType(OutputType outputType);
	protected abstract void setHandleLexicalEvents(boolean handleLexicalEvents);


	@Override
	public void setUp() throws Exception {
		session = new PipeLineSession();
		super.setUp();
	}

	protected void assertResultsAreCorrect(String expected, String actual, PipeLineSession session) {
		assertEquals(expected,actual);
	}

	protected void testXslt(String styleSheetName, String input, String expected, Boolean omitXmlDeclaration, Boolean indent, Boolean skipEmptyTags, Boolean removeNamespaces, Boolean xslt2) throws Exception {
		setStyleSheetName(styleSheetName);
		if (omitXmlDeclaration!=null) {
			setOmitXmlDeclaration(omitXmlDeclaration);
		}
		if (indent!=null) {
			setIndent(indent);
		}
		if (skipEmptyTags!=null) {
			setSkipEmptyTags(skipEmptyTags);
		}
		if (removeNamespaces!=null) {
			setRemoveNamespaces(removeNamespaces);
		}
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());
		assertResultsAreCorrect(expected,result.trim(),session);

	}

	@Test
	public void basic() throws Exception {
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		String input   =TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration=null;
		Boolean indent=null; // follows indent of stylesheet
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		Boolean xslt2=true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	@Test
	public void basicIndent() throws Exception {
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		String input   =TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration=null;
		Boolean indent=true;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		Boolean xslt2=true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	@Test
	public void basicNoIndent() throws Exception {
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		String input   =TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/orgchart-noindent.xml");
		Boolean omitXmlDeclaration=null;
		Boolean indent=false;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		Boolean xslt2=true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	public void testSkipEmptyTags(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws Exception {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, true, null, null);
	}

	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws Exception {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, null, true, null);
	}

	@Test
	public void testSkipEmptyTagsNoOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a></root>",false,false);
	}

	@Test
	public void testSkipEmptyTagsNoOmitIndent() throws Exception {
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"\t<a>a</a>"+lineSeparator+"</root>",false,true);
	}

	@Test
	public void testSkipEmptyTagsOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root><a>a</a></root>",true,false);
	}

	@Test
	public void testSkipEmptyTagsOmitIndent() throws Exception {
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"\t<a>a</a>"+lineSeparator+"</root>",true,true);
	}

	@Test
	public void testRemoveNamespacesNoOmitNoIndent() throws Exception {
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}

	@Test
	@Ignore("Indent appears not to work in combination with Streaming and RemoveNamespaces. Ignore the test for now...")
	public void testRemoveNamespacesNoOmitIndent() throws Exception {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveNamespaces("<ns:root xmlns:ns=\"urn:fakenamespace\"><ns:a>a</ns:a><ns:b></ns:b><c/></ns:root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"\t<a>a</a>"+lineSeparator+"\t<b/>"+lineSeparator+"\t<c/>"+lineSeparator+"</root>",false,true);
	}

	public void testBasic(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws Exception {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, false, null, null);
	}

	@Test
	public void testBasicNoOmitNoIndent() throws Exception {
		testBasic("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}

	@Test
	public void documentIncludedInSourceRelativeXslt1() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupRelative1.xsl");
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void documentIncludedInSourceRelativeXslt2() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupRelative2.xsl");
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void documentIncludedInSourceRelativeWithDynamicStylesheetXslt1() throws Exception {
		String stylesheetname="/Xslt/importDocument/importLookupRelative1.xsl";
		session.put("Stylesheet", stylesheetname);
		setStyleSheetNameSessionKey("Stylesheet");
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void documentIncludedInSourceRelativeWithDynamicStylesheetXslt2() throws Exception {
		String stylesheetname="/Xslt/importDocument/importLookupRelative1.xsl";
		session.put("Stylesheet", stylesheetname);
		setStyleSheetNameSessionKey("Stylesheet");
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void documentIncludedInSourceAbsoluteXslt1() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupAbsolute1.xsl");
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");
		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void documentIncludedInSourceAbsoluteXslt2() throws Exception {
		setStyleSheetName("/Xslt/importDocument/importLookupAbsolute2.xsl");
		setRemoveNamespaces(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/importDocument/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/importDocument/out.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void xPathFromParameter() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");

		pipe.addParameter(ParameterBuilder.create("source", input).withType(ParameterType.DOMDOC));
		setXpathExpression("$source/request/b");

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "<dummy name=\"input\"/>", session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect("b", result, session);
	}

	@Test
	public void xpathAttrText() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "Euro € single quote ' double quote escaped \" newline escaped \n";

		setXpathExpression("request/g/@attr");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void xpathNodeXml() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "<g attr=\"Euro € single quote ' double quote escaped &quot; newline escaped &#10;\">Euro € single quote ' double quote \"</g>";

		setXpathExpression("request/g");
		setOutputType(OutputType.XML);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void anyXmlBasic() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/Escaped2.xml");

		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void anyXmlNoMethodConfigured() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/Escaped.xml");

		setStyleSheetName("/Xslt/AnyXml/CopyNoMethodConfigured.xsl");
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void anyXmlIndent() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/PrettyPrintedEscaped.xml");

		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		setIndent(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void anyXmlAsText() throws Exception {
		Properties prop = System.getProperties();
		String vendor = prop.getProperty("java.vendor");
		System.out.println("JVM Vendor : " + vendor);
		assumeThat(vendor, not(equalTo("IBM Corporation"))); // comments are not properly processed in the IBM JDK

		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/AsText.txt");

		setStyleSheetName("/Xslt/AnyXml/CopyAsText.xsl");
		setHandleLexicalEvents(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect(expected.trim(), result.trim(), session); // trim is necessary on IBM JDK
	}

	@Test
	public void anyXmlDisableOutputEscaping() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/OutputEscapingDisabled.xml");

		setStyleSheetName("/Xslt/AnyXml/DisableOutputEscaping.xsl");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected.replaceAll("\\s", ""), result.replaceAll("\\s", ""), session);
	}

	@Test
	public void skipEmptyTagsXslt1() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/SkipEmptyTagsIndent.xml");

		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setSkipEmptyTags(true);
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		setIndent(true);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void skipEmptyTagsXslt2() throws Exception {
		setStyleSheetName("/Xslt/AnyXml/Copy.xsl");
		setSkipEmptyTags(true);
		setOmitXmlDeclaration(true);
		setHandleLexicalEvents(true);
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/AnyXml/SkipEmptyTagsIndent.xml");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect(expected, result, session);
	}

}
