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
import nl.nn.adapterframework.core.SenderResult;
import nl.nn.adapterframework.core.TimeoutException;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;
import nl.nn.adapterframework.senders.EchoSender;
import nl.nn.adapterframework.senders.JavascriptSender;
import nl.nn.adapterframework.senders.JavascriptSender.JavaScriptEngines;
import nl.nn.adapterframework.senders.SenderTestBase;
import nl.nn.adapterframework.senders.SenderWithParametersBase;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.testutil.ParameterBuilder;

@RunWith(Parameterized.class)
public class JavascriptSenderCallbackTest extends SenderTestBase<JavascriptSender> {

	@Parameterized.Parameter(0)
	public JavaScriptEngines engine;

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		return Arrays.asList(new Object[][] {{JavaScriptEngines.J2V8}, {JavaScriptEngines.NASHORN}});
	}

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@Test
	public void simpleParameterizedSenderNoCallbacks() throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create("x", "3").withType(ParameterType.INTEGER));

		sender.addParameter(ParameterBuilder.create("y", "4").withType(ParameterType.INTEGER));

		sender.configure();
		sender.open();

		assertEquals("7", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	//An EchoSender will be called in the javascript code.
	@Test
	public void javaScriptSenderWithNestedEchoSender() throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f4");
		sender.setEngineName(engine);

		sender.addParameter(ParameterBuilder.create("x", "3").withType(ParameterType.INTEGER));

		sender.addParameter(ParameterBuilder.create("y", "4").withType(ParameterType.INTEGER));

		EchoSender log = new EchoSender();
		log.setName("myFunction");
		sender.registerSender(log);

		sender.configure();
		sender.open();

		// See function 4, validates if input to the nested sender is the same as the output of the nested sender
		assertEquals("true", sender.sendMessageOrThrow(dummyInput,session).asString());
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

		Message senderResult = sender.sendMessageOrThrow(dummyInput, session);
		assertTrue(Message.isEmpty(senderResult));
		Message promiseResult = promise.getPromiseResult();
		assertFalse("promise not called", Message.isEmpty(promiseResult));
		assertEquals("success", promiseResult.asString());
	}

	private static class PromiseResultSender extends SenderWithParametersBase {
		private @Getter Message promiseResult = null;

		@Override
		public SenderResult sendMessage(Message message, PipeLineSession session) throws SenderException, TimeoutException {
			promiseResult = message;
			return new SenderResult(Message.nullMessage());
		}
	};
}