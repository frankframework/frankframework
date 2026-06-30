package org.frankframework.xslt;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterType;
import org.frankframework.pipes.XsltPipe;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.processors.InputOutputPipeProcessor;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.XmlParameterBuilder;
import org.frankframework.util.ClassLoaderUtils;
import org.frankframework.util.TransformerPool.OutputType;

public class XsltPipeTest extends XsltErrorTestBase<XsltPipe> {

	@Override
	public XsltPipe createPipe() {
		return new XsltPipe();
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
	protected void setXpathExpression(String xpathExpression) {
		pipe.setXpathExpression(xpathExpression);
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
	protected void setOutputType(OutputType outputType) {
		pipe.setOutputType(outputType);
	}

	@Override
	protected void setRemoveNamespaces(boolean removeNamespaces) {
		pipe.setRemoveNamespaces(removeNamespaces);
	}


	@Override
	protected void setHandleLexicalEvents(boolean handleLexicalEvents) {
		pipe.setHandleLexicalEvents(handleLexicalEvents);
	}

	@Override
	protected void setXsltVersion(int xsltVersion) {
		pipe.setXsltVersion(xsltVersion);
	}

	@Test
	void testSessionKey() throws Exception {
		String input = TestFileUtils.getTestFile("/Xslt/AnyXml/in.xml");
		String expected = "Euro € single quote ' double quote escaped \" newline escaped \n";

		// Use both setPreserveInput and setStoreResultInSessionKey to get the result in the sessionKey
		pipe.setPreserveInput(true);
		pipe.setStoreResultInSessionKey("sessionKey");
		setXpathExpression("/request/g/@attr");

		// Configure input/output pipe processor to enable storeResultInSessionKey
		InputOutputPipeProcessor ioProcessor = new InputOutputPipeProcessor();
		CorePipeProcessor coreProcessor = new CorePipeProcessor();
		ioProcessor.setPipeProcessor(coreProcessor);

		pipe.configure();

		PipeRunResult prr = ioProcessor.processPipe(pipeline, pipe, Message.asMessage(input), session);

		Message sessionKey = session.getMessage("sessionKey");
		assertEquals(expected, sessionKey.asString());
		String result = prr.getResult().asString();
		assertEquals(result, input);
	}

	/**
	 * @see <a href="https://github.com/frankframework/frankframework/issues/3934">for the issue describing this problem</a>
	 * @throws Exception
	 */
	@Test
	@DisplayName("Assert that we get a PipeRunException when using type = NODE and an xpathExpression on the XmlParameter")
	void test3934WithXpathExpressionParameter() throws Exception {
		String parameterContents = TestFileUtils.getTestFile("/Xslt/3934/param.xml");

		session.put("keyXmlParameter", parameterContents);

		@SuppressWarnings("removal")
		XmlParameterBuilder parameter = XmlParameterBuilder.create()
				.withName("parNode")
				.withType(ParameterType.NODE);
		parameter.setSessionKey("keyXmlParameter");
		parameter.setXpathExpression("xmlRoot/xmlChild");
		parameter.setXsltVersion(1);

		pipe.addParameter(parameter);
		pipe.setXsltVersion(1);
		pipe.setIndentXml(true);
		pipe.setStyleSheetName("/Xslt/3934/template_with_xpath_parameter.xsl");
		pipe.configure();
		pipe.start();

		assertThrows(PipeRunException.class, () -> doPipe(pipe, "<test/>", session));
	}

	@Test
	@DisplayName("Assert that we get a PipeRunException with type = NODE and no xpathExpression")
	void test3934WithParameter() throws Exception {
		String parameterContents = TestFileUtils.getTestFile("/Xslt/3934/param.xml");

		session.put("keyXmlParameter", parameterContents);

		@SuppressWarnings("removal")
		XmlParameterBuilder parameter = XmlParameterBuilder.create()
				.withName("parNode")
				.withType(ParameterType.NODE);
		parameter.setSessionKey("keyXmlParameter");
		parameter.setXsltVersion(1);

		pipe.addParameter(parameter);
		pipe.setXsltVersion(1);
		pipe.setIndentXml(true);
		pipe.setStyleSheetName("/Xslt/3934/template_without_xpath_parameter_node.xsl");
		pipe.configure();
		pipe.start();

		PipeRunResult result = doPipe(pipe, "<test/>", session);
		assertNotNull(result);

		String expected = TestFileUtils.getTestFile("/Xslt/3934/expected.xml");
		assertEquals(expected, result.getResult().asString());
	}

	@Test
	@DisplayName("Assert that we don't get a PipeRunException when using DOMDOC and an XSL without xpath parameter")
	void test3934WithDomDoc() throws Exception {
		String paramContents = TestFileUtils.getTestFile("/Xslt/3934/param.xml");

		session.put("keyXmlParameter", paramContents);

		XmlParameterBuilder parameter = XmlParameterBuilder.create()
				.withName("parNode")
				.withType(ParameterType.DOMDOC);
		parameter.setSessionKey("keyXmlParameter");
		parameter.setXsltVersion(1);

		pipe.addParameter(parameter);
		pipe.setXsltVersion(1);
		pipe.setIndentXml(true);
		pipe.setStyleSheetName("/Xslt/3934/template_without_xpath_parameter.xsl");
		pipe.configure();
		pipe.start();

		PipeRunResult result = doPipe(pipe, "<test/>", session);
		assertNotNull(result);

		String expected = TestFileUtils.getTestFile("/Xslt/3934/expected.xml");
		assertEquals(expected, result.getResult().asString());
	}

	// When reading the result the new charset needs to be known, else it will use UTF-8 and thus muck up the characters.
	@Test
	public void testOutputEncodingISO8859_1Input() throws Exception {
		pipe.setStyleSheetName("/Xslt/ISO-8859-1/output-encoding.xsl");
		pipe.configure();
		pipe.start();
		URL url = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/iso-8859-1.xml");
		assertNotNull(url);
		Message inputMessage = new UrlMessage(url, StandardCharsets.ISO_8859_1);

		URL resultUrl = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/utf-8.xml");
		assertNotNull(resultUrl);
		String expected = new UrlMessage(resultUrl).asString();

		// Act
		PipeRunResult result = doPipe(inputMessage);
		assertNotNull(result);

		// Assert
		String resultString = result.getResult().asString();
		assertNotNull(resultString);
		assertTrue(resultString.contains("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"));
		assertXmlEquals(expected, resultString);

		assertEquals("ISO-8859-1", result.getResult().getCharset());
	}

	@Test
	public void testOutputEncodingUTF8Input() throws Exception {
		pipe.setStyleSheetName("/Xslt/ISO-8859-1/output-encoding.xsl");
		pipe.configure();
		pipe.start();

		URL url = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/utf-8.xml");
		assertNotNull(url);
		Message inputMessage = new UrlMessage(url, StandardCharsets.UTF_8);

		URL resultUrl = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/iso-8859-1.xml");
		assertNotNull(resultUrl);
		UrlMessage expectedMessage = new UrlMessage(resultUrl, StandardCharsets.ISO_8859_1);
		byte[] expectedBytes = expectedMessage.asByteArray();
		String expected = expectedMessage.asString();

		// Act
		PipeRunResult result = doPipe(inputMessage);
		assertNotNull(result);

		// Assert
		byte[] resultBytes = result.getResult().asByteArray();
		String resultString = result.getResult().asString();

		assertNotNull(resultBytes);
		assertNotNull(resultString);

		assertTrue(resultString.contains("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>"));
		assertXmlEquals(expected, resultString);

		assertEquals("ISO-8859-1", result.getResult().getCharset());

		// Check actual bytes produced to be really sure the output is in the desired charset
		assertArrayEquals(expectedBytes, resultBytes);
	}

	@Test
	public void testOutputEncodingWithTextOutput_Iso8859_1_Input() throws Exception {
		pipe.setStyleSheetName("/Xslt/ISO-8859-1/output-encoding-with-text-output.xsl");
		pipe.configure();
		pipe.start();
		URL url = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/iso-8859-1.xml");
		assertNotNull(url);
		Message inputMessage = new UrlMessage(url, StandardCharsets.ISO_8859_1);

		URL expectedUrl = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/iso-8859-1.txt");
		assertNotNull(expectedUrl);
		UrlMessage expectedMessage = new UrlMessage(expectedUrl, StandardCharsets.ISO_8859_1);
		byte[] expectedBytes = expectedMessage.asByteArray();
		String expected = expectedMessage.asString();

		// Act
		PipeRunResult result = doPipe(inputMessage);
		assertNotNull(result);

		// Assert

		// Retrieve bytes before getting string, to be absolutely sure we get the raw output bytes from the XSLT conversion
		byte[] resultBytes = result.getResult().asByteArray();

		String resultString = result.getResult().asString();
		assertEquals(expected, resultString);

		// Check actual bytes produced to be really sure the output is in the desired charset
		assertArrayEquals(expectedBytes, resultBytes);

		assertEquals("ISO-8859-1", result.getResult().getCharset());
	}

	@Test
	public void testOutputEncodingWithTextOutput_UTF8_Input() throws Exception {
		pipe.setStyleSheetName("/Xslt/ISO-8859-1/output-encoding-with-text-output.xsl");
		pipe.configure();
		pipe.start();
		URL url = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/utf-8.xml");
		assertNotNull(url);
		Message inputMessage = new UrlMessage(url, StandardCharsets.UTF_8);

		URL expectedUrl = ClassLoaderUtils.getResourceURL("/Xslt/ISO-8859-1/iso-8859-1.txt");
		assertNotNull(expectedUrl);
		UrlMessage expectedMessage = new UrlMessage(expectedUrl, StandardCharsets.ISO_8859_1);
		byte[] expectedBytes = expectedMessage.asByteArray();
		String expected = expectedMessage.asString();

		// Act
		PipeRunResult result = doPipe(inputMessage);
		assertNotNull(result);

		// Assert

		// Retrieve bytes before getting string, to be absolutely sure we get the raw output bytes from the XSLT conversion
		byte[] resultBytes = result.getResult().asByteArray();

		String resultString = result.getResult().asString();
		assertEquals(expected, resultString);

		// Check actual bytes produced to be really sure the output is in the desired charset
		assertArrayEquals(expectedBytes, resultBytes);

		assertEquals("ISO-8859-1", result.getResult().getCharset());
	}
}
