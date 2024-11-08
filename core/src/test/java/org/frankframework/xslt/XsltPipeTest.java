package org.frankframework.xslt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.ParameterType;
import org.frankframework.pipes.XsltPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.testutil.XmlParameterBuilder;
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

		pipe.setSessionKey("sessionKey");
		setXpathExpression("/request/g/@attr");
		pipe.configure();
		pipe.start();

		PipeRunResult prr = doPipe(pipe, input, session);
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
}
