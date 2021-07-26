package nl.nn.adapterframework.extensions.javascript;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.core.TimeOutException;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.senders.JavascriptSender;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;

@RunWith(Parameterized.class)
public class JavascriptSenderCallbackTest extends SenderTestBase<JavascriptSender> {

	@Parameterized.Parameter(0)
	public String engine;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {{"J2V8"}, {"Nashorn"}});
	}

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@Test
	public void simpleParameterizedSenderNoCallbacks() throws ConfigurationException, SenderException, TimeOutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

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
		sender.setEngineName(engine);

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
		sender.registerSender(log);

		sender.configure();
		sender.open();

		// See function 4, validates if input to the nested sender is the same as the output of the nested sender
		assertEquals("true", sender.sendMessage(dummyInput,session).asString());
	}

	@Test
	public void promise() throws Exception {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js"); 
		sender.setJsFunctionName("promise");
		sender.setEngineName(engine);

		PromiseResultSender promise = new PromiseResultSender();
		promise.setName("result");
		sender.registerSender(promise);

		sender.configure();
		sender.open();

		Message senderResult = sender.sendMessage(dummyInput, session);
		assertTrue(Message.isEmpty(senderResult));
		Message promiseResult = promise.getPromiseResult();
		assertFalse("promise not called", Message.isEmpty(promiseResult));
		assertEquals("success", promiseResult.asString());
	}

	private static class PromiseResultSender extends SenderWithParametersBase {
		private @Getter Message promiseResult = null;

		@Override
		public Message sendMessage(Message message, PipeLineSession session) throws SenderException, TimeOutException {
			promiseResult = message;
			return Message.nullMessage();
		}
	};
}