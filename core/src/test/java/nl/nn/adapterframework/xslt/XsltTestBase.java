package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;

import javax.xml.transform.TransformerException;

import org.hamcrest.core.StringContains;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.IPipeLineSession;
import nl.nn.adapterframework.core.PipeLineSessionBase;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.stream.IOutputStreamingSupport;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.MessageOutputStreamCap;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.StreamingPipe;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.DomBuilderException;

@RunWith(Parameterized.class)
public abstract class XsltTestBase<P extends IPipe> extends PipeTestBase<P> {
	
	public static final String IDENTITY_STYLESHEET="/Xslt/identity.xslt";

	protected IPipeLineSession session;

	protected abstract void setStyleSheetName(String styleSheetName);
	protected abstract void setOmitXmlDeclaration(boolean omitXmlDeclaration);
	protected abstract void setIndent(boolean indent);
	protected abstract void setSkipEmptyTags(boolean skipEmptyTags);
	protected abstract void setRemoveNamespaces(boolean removeNamespaces);
	protected abstract void setXslt2(boolean xslt2);
 
	@Parameters(name = "{index}: {0}: provide [{2}] stream out [{3}]")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                 { "classic", 			false, false, false }, 
                 { "new, no stream", 	 true, false, false }, 
                 { "output to stream", 	 true, false, true  }, 
                 { "consume stream", 	 true, true,  false }, 
                 { "stream through",  	 true, true,  true  }
           });
    }
	
	@Parameter(0)
	public String  description=null;
	@Parameter(1)
	public boolean classic=true;
	@Parameter(2)
	public boolean provideStreamForInput=false;
	@Parameter(3)
	public boolean writeOutputToStream=false;
	
	@Override
	public void setup() throws ConfigurationException {
		session = new PipeLineSessionBase();
		super.setup();
	}

	protected void assertResultsAreCorrect(String expected, String actual, IPipeLineSession session) {
		assertEquals(expected,actual);	
	}
	
	protected void testXslt(String styleSheetName, String input, String expected, Boolean omitXmlDeclaration, Boolean indent, Boolean skipEmptyTags, Boolean removeNamespaces, Boolean xslt2) throws ConfigurationException, PipeStartException, IOException, PipeRunException, StreamingException {
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

		String result;
		if (provideStreamForInput) {
			MessageOutputStream target;
			if (writeOutputToStream) {
				target = ((IOutputStreamingSupport)pipe).provideOutputStream(null, session, new MessageOutputStreamCap());
			} else {
				target = ((IOutputStreamingSupport)pipe).provideOutputStream(null, session, null);
			}
			
			try (Writer writer = target.asWriter()) {
				writer.write(input);
			}
			result=target.getResponseAsString();
		} else {
			PipeRunResult prr;
			if (classic) {
				prr = ((StreamingPipe)pipe).doPipe(input,session);
			} else {
				if (writeOutputToStream) {
					prr = ((StreamingPipe)pipe).doPipe(input,session,new MessageOutputStreamCap());
				} else {
					prr = ((StreamingPipe)pipe).doPipe(input,session,null);
				}
			}		
			result=(String)prr.getResult();
		}
		assertResultsAreCorrect(expected,result.trim(),session);
		
	}
	
	@Test
	public void basic() throws ConfigurationException, PipeStartException, IOException, PipeRunException, StreamingException {
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

	public void testSkipEmptyTags(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, true, null, null);
	}
	
	public void testRemoveNamespaces(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, null, true, null);
	}
	
	@Test
	public void testSkipEmptyTagsNoOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a></root>",false,false);
	}

	@Test
	public void testSkipEmptyTagsNoOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
//		String lineSeparator=System.getProperty("line.separator");
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+lineSeparator+"<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"</root>",false,true);
	}
	@Test
	public void testSkipEmptyTagsOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root><a>a</a></root>",true,false);
	}
	@Test
	public void testSkipEmptyTagsOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
//		String lineSeparator=System.getProperty("line.separator");
		String lineSeparator="\n";
		testSkipEmptyTags("<root><a>a</a><b></b><c/></root>","<root>"+lineSeparator+"   <a>a</a>"+lineSeparator+"</root>",true,true);
	}
	
	@Test
	public void testRemoveNamespacesNoOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testRemoveNamespaces("<root xmlns=\"urn:fakenamespace\"><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}
	@Test
	public void testRemoveNamespacesNoOmitIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
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
	
	public void testBasic(String input, String expected, boolean omitXmlDeclaration, boolean indent) throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testXslt(IDENTITY_STYLESHEET, input, expected, omitXmlDeclaration, indent, false, null, null);
	}
	@Test
	public void testBasicNoOmitNoIndent() throws DomBuilderException, TransformerException, IOException, ConfigurationException, PipeStartException, PipeRunException, StreamingException {
		testBasic("<root><a>a</a><b></b><c/></root>","<?xml version=\"1.0\" encoding=\"UTF-8\"?><root><a>a</a><b/><c/></root>",false,false);
	}

}
