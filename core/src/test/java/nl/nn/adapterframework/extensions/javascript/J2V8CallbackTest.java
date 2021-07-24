package nl.nn.adapterframework.extensions.javascript;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.senders.JavascriptSender;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.stream.Message;

public class J2V8CallbackTest extends SenderTestBase<JavascriptSender> {

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@Test
	public void simpleSenderNoCallbacks() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f2");
		sender.setEngineName("J2V8");

		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("3");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("4");
		sender.addParameter(param2);

		sender.configure();
		sender.open();

		assertEquals("7", sender.sendMessage(dummyInput, session).asString());
	}

	//An EchoSender will be called in the javascript code.
	@Test
	public void javaScriptSenderWithNestedEchoSender() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("f4");
		sender.setEngineName("J2V8");

		Parameter param = new Parameter();
		param.setName("x");
		param.setType("integer");
		param.setValue("3");
		sender.addParameter(param);

		Parameter param2 = new Parameter();
		param2.setName("y");
		param2.setType("integer");
		param2.setValue("4");
		sender.addParameter(param2);

		EchoSender log = new EchoSender();
		log.setName("myFunction");
		sender.setSender(log);

		sender.configure();
		sender.open();

		// See function 4, validates if input to the nested sender is the same as the output of the nested sender
		assertEquals("true", sender.sendMessage(dummyInput,session).asString());
	}
}