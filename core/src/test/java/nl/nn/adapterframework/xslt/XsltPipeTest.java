package nl.nn.adapterframework.xslt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import nl.nn.adapterframework.core.PipeRunResult;

import org.junit.jupiter.api.Test;
import nl.nn.adapterframework.pipes.XsltPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

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
		String result = Message.asMessage(prr.getResult()).asString();
		assertEquals(result, input);
	}

}
