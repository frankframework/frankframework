package org.frankframework.pipes;

import static org.frankframework.testutil.MatchUtils.assertXmlEquals;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.documentbuilder.DocumentFormat;
import org.frankframework.processors.CorePipeProcessor;
import org.frankframework.senders.EchoSender;
import org.frankframework.soap.SoapWrapperPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.MessageTestUtils;
import org.frankframework.testutil.TestFileUtils;

public class MessageSendingPipeTest extends PipeTestBase<MessageSendingPipe> {

	@Override
	public MessageSendingPipe createPipe() {
		MessageSendingPipe result = new MessageSendingPipe();
		result.setSender(new EchoSender() {

			@Override
			public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) throws SenderException, TimeoutException {
				try {
					return new SenderResult("{ \"input\": \""+message.asString()+"\"}");
				} catch (IOException e) {
					throw new SenderException(e);
				}
			}
		});
		return result;
	}

	@Test
	public void testConfigure() throws Exception {
		pipe.configure();
	}

	@Test
	public void testConfigureAndStart() throws Exception {
		configureAndStartPipe();
	}

	@Test
	public void testBasic() throws Exception {
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testMessage");
		Assertions.assertEquals("success", prr.getPipeForward().getName());
		Assertions.assertEquals("{ \"input\": \"testMessage\"}", prr.getResult().asString());
	}

	@Test
	public void testInputWrapped() throws Exception {
		pipe.setInputWrapper(new SoapWrapperPipe());
		pipe.setPipeProcessor(new CorePipeProcessor());
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testMessage");
		Assertions.assertEquals("success", prr.getPipeForward().getName());
		String expected = "{ \"input\": \"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>testMessage</soapenv:Body></soapenv:Envelope>\"}";
		Assertions.assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testOutputWrapped() throws Exception {
		pipe.setOutputWrapper(new SoapWrapperPipe());
		pipe.setPipeProcessor(new CorePipeProcessor());
		configureAndStartPipe();

		PipeRunResult prr = doPipe("testMessage");
		Assertions.assertEquals("success", prr.getPipeForward().getName());
		String expected = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>{ \"input\": \"testMessage\"}</soapenv:Body></soapenv:Envelope>";
		Assertions.assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testInputValidated() throws Exception {
		Json2XmlValidator validator = new Json2XmlValidator();
		validator.setNoNamespaceSchemaLocation("/Align/Abc/abc.xsd");
		validator.setOutputFormat(DocumentFormat.JSON);
		validator.setThrowException(true);
		pipe.setInputValidator(validator);
		pipe.setPipeProcessor(new CorePipeProcessor());
		configureAndStartPipe();

		Message input = MessageTestUtils.getMessage("/Align/Abc/abc.xml");
		String expected =  "{ \"input\": \""+ TestFileUtils.getTestFile("/Align/Abc/abc-compact.json")+"\"}";
		PipeRunResult prr = doPipe(input);
		Assertions.assertEquals("success", prr.getPipeForward().getName());
		Assertions.assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testOutputValidated() throws Exception {
		Json2XmlValidator validator = new Json2XmlValidator();
		validator.setNoNamespaceSchemaLocation("/Align/Abc/abc.xsd");
		validator.setRoot("a");
		validator.setOutputFormat(DocumentFormat.XML);
		validator.setThrowException(true);
		pipe.setOutputValidator(validator);
		pipe.setSender(new EchoSender());
		pipe.setPipeProcessor(new CorePipeProcessor());
		configureAndStartPipe();

		Message input = MessageTestUtils.getMessage("/Align/Abc/abc-compact.json");
		String expected =  TestFileUtils.getTestFile("/Align/Abc/abc.xml");
		PipeRunResult prr = doPipe(input);
		Assertions.assertEquals("success", prr.getPipeForward().getName());
		assertXmlEquals("response converted", expected, prr.getResult().asString(),true);
	}
}
