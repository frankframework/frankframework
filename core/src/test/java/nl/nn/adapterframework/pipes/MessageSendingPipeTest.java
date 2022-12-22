package nl.nn.adapterframework.pipes;

import static nl.nn.adapterframework.testutil.MatchUtils.assertXmlEquals;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.processors.CorePipeProcessor;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.soap.SoapWrapperPipe;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.StreamingPipeTestBase;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.testutil.TestFileUtils;

public class MessageSendingPipeTest extends StreamingPipeTestBase<MessageSendingPipe> {

	@Override
	public MessageSendingPipe createPipe() {
		MessageSendingPipe result = new MessageSendingPipe();
		result.setSender(new EchoSender() {

			@Override
			public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
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
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals("{ \"input\": \"testMessage\"}", prr.getResult().asString());
	}
	
	@Test
	public void testInputWrapped() throws Exception {
		pipe.setInputWrapper(new SoapWrapperPipe());
		pipe.setPipeProcessor(new CorePipeProcessor());
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe("testMessage");
		assertEquals("success", prr.getPipeForward().getName());
		String expected = "{ \"input\": \"<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>testMessage</soapenv:Body></soapenv:Envelope>\"}";
		assertEquals(expected, prr.getResult().asString());
	}

	@Test
	public void testOutputWrapped() throws Exception {
		pipe.setOutputWrapper(new SoapWrapperPipe());
		pipe.setPipeProcessor(new CorePipeProcessor());
		configureAndStartPipe();
		
		PipeRunResult prr = doPipe("testMessage");
		assertEquals("success", prr.getPipeForward().getName());
		String expected = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"><soapenv:Body>{ \"input\": \"testMessage\"}</soapenv:Body></soapenv:Envelope>";
		assertEquals(expected, prr.getResult().asString());
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
		
		Message input = TestFileUtils.getTestFileMessage("/Align/Abc/abc.xml");
		String expected =  "{ \"input\": \""+ TestFileUtils.getTestFile("/Align/Abc/abc-compact.json")+"\"}";
		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());
		assertEquals(expected, prr.getResult().asString());
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
		
		Message input = TestFileUtils.getTestFileMessage("/Align/Abc/abc-compact.json");
		String expected =  TestFileUtils.getTestFile("/Align/Abc/abc.xml");
		PipeRunResult prr = doPipe(input);
		assertEquals("success", prr.getPipeForward().getName());
		assertXmlEquals("response converted", expected, prr.getResult().asString(),true);
	}
}
