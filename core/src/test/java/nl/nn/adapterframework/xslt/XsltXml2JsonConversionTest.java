package nl.nn.adapterframework.xslt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;

import javax.xml.transform.*;
import javax.xml.transform.stream.*;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.PipeTestBase;
import nl.nn.adapterframework.pipes.XsltPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XsltXml2JsonConversionTest extends PipeTestBase<XsltPipe>{

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
		assertResultsAreCorrect(expected,result.trim(),session);
	}
	
	@Test
	public void testBasicWrappedJsonConversion() throws Exception {
		String styleSheetName=  "/Xslt3/conversion/jsonToXmlConversion.xsl";
		String input   =TestFileUtils.getTestFile("/Xslt3/conversion/wrappedOriginalJson.json");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/expectedXml.xml");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}
	
	@Test
	public void testComplexWrappedJsonConversion() throws Exception {
		String styleSheetName=  "/Xslt3/conversion/jsonToXmlConversion.xsl";
		String input   =TestFileUtils.getTestFile("/Xslt3/conversion/complexOriginalJson.json");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/complexExpectedXml.xml");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}

	@Test
	public void testBasicReturnXmlConversion() throws Exception {
		String styleSheetName=  "/Xslt3/conversion/xmlToJsonConversion.xsl";
		String input   =TestFileUtils.getTestFile("/Xslt3/conversion/returnOriginalXml.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/expectedJson.json");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}
	
	@Test
	public void testComplexReturnXmlConversion() throws Exception {
		String styleSheetName=  "/Xslt3/conversion/xmlToJsonConversion.xsl";
		String input   =TestFileUtils.getTestFile("/Xslt3/conversion/returnComplexOriginalXml.xml");
		String expected=TestFileUtils.getTestFile("/Xslt3/conversion/complexExpectedJson.json");
		Boolean omitXmlDeclaration=null;
		Boolean skipEmptyTags=null;
		Boolean removeNamespaces=null;
		testXslt(styleSheetName, input, expected, omitXmlDeclaration, skipEmptyTags, removeNamespaces);
	}
	
	@Test
	public void testParameterizedJsonConversion() throws Exception {
		Source template = (new StreamSource(new File("C:\\Users\\HP\\FrankWorkspace\\Frank Framework\\iaf\\core\\src\\test\\resources\\Xslt3\\conversion\\unwrappedJsontoXmlConversion.xsl")));
		
		StringWriter writer = new StringWriter();
		Result output = new StreamResult(writer);
		
		try {
			Transformer transformer = TransformerFactory.newInstance().newTransformer(template);
			transformer.transform(new StreamSource(new StringReader("<empty/>")), output);
			
		} catch(TransformerException e){
			e.printStackTrace();
		}
		
		
		String result = writer.toString();
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/expectedXml.xml");
		
		assertEquals(expected, result.trim());
		
	}
}
