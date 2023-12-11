package nl.nn.adapterframework.xslt;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.pipes.SenderPipe;
import nl.nn.adapterframework.senders.XsltSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestFileUtils;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

public class XsltSenderTest extends XsltErrorTestBase<SenderPipe> {

	protected XsltSender sender;

	@Override
	public SenderPipe createPipe() {
		SenderPipe pipe=new SenderPipe();
		sender = new XsltSender();
		autowireByType(sender);
		pipe.setSender(sender);
		return pipe;
	}

	@Override
	protected void setXsltVersion(int xsltVersion) {
		sender.setXsltVersion(xsltVersion);
	}

	/*
	 * Test with output-method=xml, but yielding a text file.
	 * It should not render namespace definitions multiple times
	 */
	@Test
	public void multiNamespace() throws Exception {
		setStyleSheetName("/Xslt/MultiNamespace/toText.xsl");
		setIndent(true);
		pipe.configure();
		pipe.start();
		String input = TestFileUtils.getTestFile("/Xslt/MultiNamespace/in.xml");
		String expected = TestFileUtils.getTestFile("/Xslt/MultiNamespace/out.txt");

		PipeRunResult prr = doPipe(pipe, input, session);
		String result = Message.asString(prr.getResult());

		assertResultsAreCorrect(expected, result, session);
	}



	@Override
	protected void setStyleSheetName(String styleSheetName) {
		sender.setStyleSheetName(styleSheetName);
	}

	@Override
	protected void setStyleSheetNameSessionKey(String styleSheetNameSessionKey) {
		sender.setStyleSheetNameSessionKey(styleSheetNameSessionKey);
	}


	@Override
	protected void setXpathExpression(String xpathExpression) {
		sender.setXpathExpression(xpathExpression);
	}

	@Override
	protected void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
		sender.setOmitXmlDeclaration(omitXmlDeclaration);
	}

	@Override
	protected void setIndent(boolean indent) {
		sender.setIndentXml(indent);
	}

	@Override
	protected void setSkipEmptyTags(boolean skipEmptyTags) {
		sender.setSkipEmptyTags(skipEmptyTags);
	}

	@Override
	protected void setOutputType(OutputType outputType) {
		sender.setOutputType(outputType);
	}

	@Override
	protected void setRemoveNamespaces(boolean removeNamespaces) {
		sender.setRemoveNamespaces(removeNamespaces);
	}

	@Override
	protected void setHandleLexicalEvents(boolean handleLexicalEvents) {
		sender.setHandleLexicalEvents(handleLexicalEvents);
	}

}
