package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.parameters.ParameterType;
import org.frankframework.senders.JavascriptSender.JavaScriptEngines;
import org.frankframework.stream.Message;
import org.frankframework.testutil.NumberParameterBuilder;
import org.frankframework.testutil.ParameterBuilder;
import org.junit.jupiter.api.Test;

import lombok.Getter;

public class JavascriptSenderCallbackTest extends SenderTestBase<JavascriptSender> {

	private final JavaScriptEngines engine = JavaScriptEngines.J2V8;

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

		sender.addParameter(NumberParameterBuilder.create("x", 3));
		sender.addParameter(NumberParameterBuilder.create("y", 4));

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

		sender.addParameter(NumberParameterBuilder.create("x", 3));
		sender.addParameter(NumberParameterBuilder.create("y", 4));

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
		assertFalse(Message.isEmpty(promiseResult), "promise not called");
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
