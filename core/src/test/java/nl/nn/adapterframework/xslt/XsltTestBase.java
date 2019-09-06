package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.xml.transform.TransformerException;

import org.hamcrest.core.StringContains;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.DomBuilderException;

public abstract class XsltTestBase<P extends IPipe> extends PipeTestBase<P> {
	
	public static final String IDENTITY_STYLESHEET="/Xslt/identity.xslt";

	protected IPipeLineSession session;

	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);
	protected abstract void setIndent(boolean indent);
	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);
	protected abstract void setRemoveNamespaces(boolean removeNamespaces);
	protected abstract void setXslt2(boolean xslt2);
	protected abstract void setStreamToSessionKey(String streamToSessionKey);
	
	@Override
	public void setup() throws ConfigurationException {
		session = new PipeLineSessionBase();
		super.setup();
	}

	protected void assertResultsAreCorrect(String expected, String actual, IPipeLineSession session) {
		assertEquals(expected,actual);	
	}
	
	protected void testXslt(String styleSheetName, String input, String expected, Boolean omitXmlDeclaration, Boolean indent, Boolean skipEmptyTags, Boolean removeNamespaces, Boolean xslt2, boolean viaOutputStream) throws ConfigurationException, PipeStartException, IOException, PipeRunException {
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
		if (viaOutputStream) {
			OutputStream out = new ByteArrayOutputStream();
			session.put("outputstream", out);
			setStreamToSessionKey("outputstream");
		}
		pipe.configure();
		pipe.start();
		PipeRunResult prr = pipe.doPipe(input,session);
		String result=(String)prr.getResult();
		if (viaOutputStream) {
			assertEquals(input, result);
			ByteArrayOutputStream out = (ByteArrayOutputStream)session.get("outputstream");
			result=new String(out.toByteArray(),"utf-8");
		}
		assertResultsAreCorrect(expected,result.trim(),session);
		
	}
	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		String input   =TestFileUtils.getTestFile("/Xslt3/employees.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration=null;
		Boolean indent=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		Boolean xslt2=true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2, false);
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
		assertThat(warnings.get(0), StringContains.containsString("the attribute 'xslt2' has been deprecated. Its value is now auto detected. If necessary, replace with a setting of xsltVersion"));
		assertThat(warnings.get(1), StringContains.containsString("configured xsltVersion [1] does not match xslt version [2] declared in stylesheet"));
		assertThat(warnings.get(1), StringContains.containsString(styleSheetName));
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
		assertThat(warnings.get(0), StringContains.containsString("the attribute 'xslt2' has been deprecated. Its value is now auto detected. If necessary, replace with a setting of xsltVersion"));
		assertThat(warnings.get(1), StringContains.containsString("configured xsltVersion [1] does not match xslt version [2] declared in stylesheet"));
		assertThat(warnings.get(1), StringContains.containsString(styleSheetName));
	}

	public void testSkipEmptyTags(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, true, null, null, false);
	}
	
	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, null, true, null, false);
	}
	
	@Test
	public void testSkipEmptyTagsNoOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a></root>",false,false);
	}

	@Test
	public void testSkipEmptyTagsNoOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
//		String lineSeparator=System.getProperty("line.separator");
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"</root>",false,true);
	}
	@Test
	public void testSkipEmptyTagsOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root><a>a</a></root>",true,false);
	}
	@Test
	public void testSkipEmptyTagsOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
//		String lineSeparator=System.getProperty("line.separator");
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"</root>",true,true);
	}
	
	@Test
	public void testRemoveNamespacesNoOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}
	@Test
	public void testRemoveNamespacesNoOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"   <b/>"+lineSeparator+"   <c/>"+lineSeparator+"</root>",false,true);
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
	
	public void testBasic(String input, String expected, boolean omitXmlDeclaration, boolean indent, boolean streaming) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, false, null, null, streaming);
	}
	@Test
	public void testBasicNoOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testBasic("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false,false);
	}
	@Test
	public void testBasicNoOmitNoIndentStreaming() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testBasic("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false,true);
	}


}
