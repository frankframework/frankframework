package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Ignore;
import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.XsltPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

public class XsltXml2JsonConversionTest extends XsltErrorTestBase<XsltPipe>{

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
	}
	
	protected void assertResultsAreCorrect(String expected, String actual, PipeLineSession session) {
		assertEquals(expected,actual);
	}
	
	protected void testXslt(String styleSheetName, String input, String expected, Boolean omitXmlDeclaration, Boolean skipEmptyTags, Boolean removeNamespaces) throws Exception {
		pipe.setStyleSheetName(styleSheetName);
		if (omitXmlDeclaration!=null) {
			pipe.setOmitXmlDeclaration(omitXmlDeclaration);
		}
		if (skipEmptyTags!=null) {
			pipe.setSkipEmptyTags(skipEmptyTags);
		}
		if (removeNamespaces!=null) {
			pipe.setRemoveNamespaces(removeNamespaces);
		}
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());
		assertResultsAreCorrect(expected, result.trim(), session);
	}
	
	@Test
	public void testBasicWrappedJsonConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/jsonToXmlConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/wrappedOriginalJson.json");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/expectedXml.xml");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}
	
	@Test
	public void testComplexWrappedJsonConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/jsonToXmlConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/complexOriginalJson.json");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/complexExpectedXml.xml");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}

	@Test
	public void testBasicReturnXmlConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/xmlToJsonConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/returnOriginalXml.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/expectedJson.json");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}
	
	@Test
	public void testComplexReturnXmlConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/xmlToJsonConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/returnComplexOriginalXml.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/complexExpectedJson.json");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}
	
	@Test
	public void testParameterizedJsonConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/unwrappedJsontoXmlConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/returnComplexOriginalXml.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/outputXml.xml");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}

	@Override
	protected void setXpathExpression(String xpathExpression) {
		pipe.setXpathExpression(xpathExpression);
	}

	@Override
	protected void setStyleSheetName(String styleSheetName) {
		pipe.setStyleSheetName(styleSheetName);
	}

	@Override
	protected void setStyleSheetNameSessionKey(String styleSheetNameSessionKey) {
		pipe.setStyleSheetNameSessionKey(styleSheetNameSessionKey);
	}

	@Override
	protected void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
		pipe.setOmitXmlDeclaration(omitXmlDeclaration);
	}

	@Override
	protected void setIndent(boolean indent) {
		pipe.setIndentXml(indent);
	}

	@Override
	protected void setSkipEmptyTags(boolean skipEmptyTags) {
		pipe.setSkipEmptyTags(skipEmptyTags);
	}

	@Override
	protected void setRemoveNamespaces(boolean removeNamespaces) {
		pipe.setRemoveNamespaces(removeNamespaces);
	}

	@Override
	protected void setXslt2(boolean xslt2) {
		pipe.setXslt2(xslt2);
	}

	@Override
	protected void setOutputType(OutputType outputType) {
		pipe.setOutputType(outputType);
	}

	@Override
	protected void setHandleLexicalEvents(boolean handleLexicalEvents) {
		pipe.setHandleLexicalEvents(handleLexicalEvents);
	}
}
