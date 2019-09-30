package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.hamcrest.core.StringContains;
import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;

public abstract class XsltTestBase<P extends StreamingPipe> extends StreamingPipeTestBase<P> {
	
	public static final String IDENTITY_STYLESHEET="/Xslt/identity.xslt";

	protected IPipeLineSession session;

	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);
	protected abstract void setIndent(boolean indent);
	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);
	protected abstract void setRemoveNamespaces(boolean removeNamespaces);
	protected abstract void setXslt2(boolean xslt2);
 
	
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
		String result = prr.getResult().toString();
		assertResultsAreCorrect(expected,result.trim(),session);
		
	}
	
	@Test
	public void basic() throws Exception {
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		String input   =TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration=null;
		Boolean indent=null;
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
		assertTrue("Expected some config warnings",warnings.size()>1);
		for (int i=0;i<warnings.size();i++) {
			System.out.println(i+" "+warnings.get(i));
		}
		assertThat(warnings.get(0), StringContains.containsString("the attribute 'xslt2' has been deprecated. Its value is now auto detected. If necessary, replace with a setting of xsltVersion"));
		int nextPos=warnings.size()>4?warnings.size()-2:1;
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
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"</root>",false,true);
	}
	@Test
	public void testSkipEmptyTagsOmitNoIndent() throws Exception {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root><a>a</a></root>",true,false);
	}
	@Test
	public void testSkipEmptyTagsOmitIndent() throws Exception {
//		String lineSeparator=System.getProperty("line.separator");
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"</root>",true,true);
	}
	
	@Test
	public void testRemoveNamespacesNoOmitNoIndent() throws Exception {
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}
	
	@Test
	@Ignore("Indent appears not to work in combination with Streaming and RemoveNamespaces. Ignore the test for now...")	
	public void testRemoveNamespacesNoOmitIndent() throws Exception {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveNamespaces("<ns:root xmlns:ns=\"urn:fakenamespace\"><ns:a>a</ns:a><ns:b></ns:b><c/></ns:root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"   <b/>"+lineSeparator+"   <c/>"+lineSeparator+"</root>",false,true);
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

}
