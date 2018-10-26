package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.junit.Test;
import org.mockito.Mock;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.util.DomBuilderException;

public abstract class XsltTestBase<P extends IPipe> extends PipeTestBase<P> {
	
	public static final String IDENTITY_STYLESHEET="/Xslt/identity.xslt";

	private IPipeLineSession session;

	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);
	protected abstract void setIndent(boolean indent);
	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);
	protected abstract void setRemoveNamespaces(boolean removeNamespaces);
	protected abstract void setXslt2(boolean xslt2);
	
	@Override
	public void setup() throws ConfigurationException {
		super.setup();
		session = new PipeLineSessionBase();
	}

	protected void assertResultsAreCorrect(String expected, String actual, IPipeLineSession session) {
		assertEquals(expected,actual);	
	}
	
	protected void testXslt(String styleSheetName, String input, String expected, Boolean omitXmlDeclaration, Boolean indent, Boolean skipEmptyTags, Boolean removeNamespaces, Boolean xslt2) throws ConfigurationException, PipeStartException, IOException, PipeRunException {
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
		PipeRunResult prr = pipe.doPipe(input,session);
		String xmlOut=(String)prr.getResult();
		assertResultsAreCorrect(expected,xmlOut.trim(),session);
	}
	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException {
		String styleSheetName=  "/Xslt3/orgchart.xslt";
		String input   =getFile("/Xslt3/employees.xml");
		String expected=getFile("/Xslt3/orgchart.xml");
		Boolean omitXmlDeclaration=null;
		Boolean indent=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		Boolean xslt2=true;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, indent, skipEmptyTags, removeNamespaces, xslt2);
	}

	public void testSkipEmptyTags(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, true, null, null);
	}
	
	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, null, true, null);
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
		testRemoveNamespaces("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}
	@Test
	public void testRemoveNamespacesNoOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException {
		String lineSeparator=System.getProperty("line.separator");
		testRemoveNamespaces("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>"+lineSeparator+"<a>a</a>"+lineSeparator+"<b/>"+lineSeparator+"<c/>"+lineSeparator+"</root>",false,true);
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
}
