package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

import java.io.IOException;
import java.util.Properties;

import org.hamcrest.core.StringContains;
import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;

public abstract class XsltTestBase<P extends StreamingPipe> extends StreamingPipeTestBase<P> {
	
	public static final String IDENTITY_STYLESHEET="/Xslt/identity.xslt";

	protected abstract void setXpathExpression(String xpathExpression);
	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setStyleSheetNameSessionKey(String styleSheetNameSessionKey);
	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);
	protected abstract void setIndent(boolean indent);
	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);
	protected abstract void setRemoveNamespaces(boolean removeNamespaces);
	protected abstract void setXslt2(boolean xslt2);
	protected abstract void setOutputType(String outputType);
 
	
	@Override
	public void setup() throws ConfigurationException {
		session = new PipeLineSessionBase();
		super.setup();
	}

	protected void assertResultsAreCorrect(String expected, String actual, IPipeLineSession session) {
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
		if (xslt2!=null) {
			setXslt2(xslt2);
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

	/*
	 * Beware, this test could fail when run multi threaded
	 */
	@Test
	public void testConfigWarnings() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		ConfigurationWarnings warnings = ConfigurationWarnings.getInstance();
		warnings.clear(); 
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		setStyleSheetName(styleSheetName);
		setXslt2(false);
		pipe.configure();
		for (int i=0;i<warnings.size();i++) {
			System.out.println(i+" "+warnings.get(i));
		}
		assertTrue("Expected at least one config warnings",warnings.size()>0);
		int nextPos=0;//warnings.size()>4?warnings.size()-2:1;
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
	public void testSkipEmptyTagsNoOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a></root>",false,false);
	}

	@Test
	public void testSkipEmptyTagsNoOmitIndent() throws Exception {
//		String lineSeparator=System.getProperty("line.separator");
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"\t<a>a</a>"+lineSeparator+"</root>",false,true);
	}
	@Test
	public void testSkipEmptyTagsOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root><a>a</a></root>",true,false);
	}
	@Test
	public void testSkipEmptyTagsOmitIndent() throws Exception {
//		String lineSeparator=System.getProperty("line.separator");
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

	//	@Test
//	public void testRemoveNamespacesOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
//		testRemoveNamespaces("<root><a>a</a><b></b><c/></root>","<root><a>a</a><b/><c/></root>",true,false);
//	}
//	@Test
//	public void testRemoveNamespacesOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
//		String lineSeparator=System.getProperty("line.separator");
//		testRemoveNamespaces("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",true,true);
//	}
	
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
		setXslt2(false);
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
		setXslt2(false);
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
		setXslt2(false);
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
		setXslt2(true);
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
		setXslt2(false);
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
		setXslt2(false);
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

		Parameter inputParameter = new Parameter();
		inputParameter.setName("source");
		inputParameter.setValue(input);
		inputParameter.setType("domdoc");
		pipe.addParameter(inputParameter);
		setXpathExpression("$source/request/b");

		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, "<dummy name=\"input\"/>", session);
		String result = Message.asMessage(prr.getResult()).asString();

		assertResultsAreCorrect("b", result, session);
	}

	public void xpathNodeText() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "Euro € single quote ' double quote \"";

		setXpathExpression("request/g");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asMessage(prr.getResult()).asString();
		
		assertResultsAreCorrect(expected, result, session);
	}

	@Test
	public void xpathAttrText() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "Euro € single quote ' double quote escaped \" ";

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
		String expected = "<g attr=\"Euro € single quote ' double quote escaped &quot; \">Euro € single quote ' double quote \"</g>";

		setXpathExpression("request/g");
		setOutputType("xml");
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
		setXslt2(false);
		setSkipEmptyTags(true);
		setOmitXmlDeclaration(true);
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
		setXslt2(true);
		setSkipEmptyTags(true);
		setOmitXmlDeclaration(true);
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
