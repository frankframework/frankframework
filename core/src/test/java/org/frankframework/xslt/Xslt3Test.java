package org.frankframework.xslt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.XsltPipe;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.TransformerPool.OutputType;

public class Xslt3Test extends XsltErrorTestBase<XsltPipe> {

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
	}

	protected void assertResultsAreCorrect(String expected, String actual) {
		assertEquals(expected, actual);
	}

	protected void testXslt(String styleSheetName, String input, String expected) throws Exception {
		pipe.setStyleSheetName(styleSheetName);
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = prr.getResult().asString();
		assertResultsAreCorrect(expected, result.trim());
	}

	@Test
	void testBasicWrappedJsonConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/jsonToXmlConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/wrappedOriginalJson.json");
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/expectedXml.xml");
		testXslt(styleSheetName, input, expected);
	}

	@Test
	void testComplexWrappedJsonConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/jsonToXmlConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/complexOriginalJson.json");
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/complexExpectedXml.xml");
		testXslt(styleSheetName, input, expected);
	}

	@Test
	void testBasicReturnXmlConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/xmlToJsonConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/returnOriginalXml.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/expectedJson.json");
		testXslt(styleSheetName, input, expected);
	}

	@Test
	void testComplexReturnXmlConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/xmlToJsonConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/returnComplexOriginalXml.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/complexExpectedJson.json");
		testXslt(styleSheetName, input, expected);
	}

	@Test
	void testParameterizedJsonConversion() throws Exception {
		String styleSheetName = "/Xslt3/conversion/unwrappedJsontoXmlConversion.xsl";
		String input = TestFileUtils.getTestFile("/Xslt3/conversion/returnComplexOriginalXml.xml");
		String expected = TestFileUtils.getTestFile("/Xslt3/conversion/outputXml.xml");
		testXslt(styleSheetName, input, expected);
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
	protected void setXsltVersion(int xsltVersion) {
		pipe.setXsltVersion(xsltVersion);
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
