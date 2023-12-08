package nl.nn.adapterframework.pipes;

import java.io.File;
import java.net.URL;

import nl.nn.adapterframework.core.PipeRunResult;

import org.junit.jupiter.api.Test;
import nl.nn.adapterframework.senders.XsltSender;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.TestAssertions;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class XmlFileElementIteratorPipeTest extends PipeTestBase<XmlFileElementIteratorPipe> {

	@Override
	public XmlFileElementIteratorPipe createPipe() {
		return new XmlFileElementIteratorPipe();
	}

	@Test
	void testElementName() throws Exception {
		XsltSender sender = new XsltSender();
		sender.setXpathExpression("concat(Person/PersonName/Id,'_',Person/Demographics/Gender)");
		pipe.setSender(sender);
		pipe.setElementName("Person");
		pipe.configure();
		pipe.start();

		URL input = TestFileUtils.getTestFileURL("/XmlFileElementIteratorPipe/input.xml");
		File file = new File(input.toURI());
		String expected = TestFileUtils.getTestFile("/XmlFileElementIteratorPipe/ElementNameOutput.xml");
		PipeRunResult prr = doPipe(pipe, file.toString(), session);
		String result = Message.asString(prr.getResult());
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}

	@Test
	void testElementChain() throws Exception {
		XsltSender sender = new XsltSender();
		sender.setXpathExpression("concat(Person/PersonName/Id,'_',Person/Demographics/Gender)");
		pipe.setSender(sender);
		pipe.setElementChain("GetPartiesOnAgreementRLY;PartyAgreementRole;PartyInternalAgreementRole;Party;Person");
		pipe.configure();
		pipe.start();

		URL input = TestFileUtils.getTestFileURL("/XmlFileElementIteratorPipe/input.xml");
		File file = new File(input.toURI());
		String expected = TestFileUtils.getTestFile("/XmlFileElementIteratorPipe/ElementChainOutput.xml");
		PipeRunResult prr = doPipe(pipe, file.toString(), session);
		String result = Message.asString(prr.getResult());
		TestAssertions.assertEqualsIgnoreCRLF(expected, result);
	}
}
