package org.frankframework.senders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import jakarta.annotation.Nonnull;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.SenderException;
import org.frankframework.core.SenderResult;
import org.frankframework.core.TimeoutException;
import org.frankframework.senders.JavascriptSender.JavaScriptEngines;
import org.frankframework.stream.Message;
import org.frankframework.testutil.NumberParameterBuilder;

class JavascriptSenderCallbackTest extends SenderTestBase<JavascriptSender> {

	@Override
	public JavascriptSender createSender() {
		return new JavascriptSender();
	}

	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void simpleParameterizedSenderNoCallbacks(JavaScriptEngines engine) throws Exception {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f2");
		sender.setEngineName(engine);

		sender.addParameter(NumberParameterBuilder.create("x", 3));
		sender.addParameter(NumberParameterBuilder.create("y", 4));

		sender.configure();
		sender.start();

		assertEquals("7", sender.sendMessageOrThrow(dummyInput, session).asString());
	}

	//An EchoSender will be called in the javascript code.
	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void javaScriptSenderWithNestedEchoSender(JavaScriptEngines engine) throws ConfigurationException, SenderException, TimeoutException, IOException {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("f4");
		sender.setEngineName(engine);

		sender.addParameter(NumberParameterBuilder.create("x", 3));
		sender.addParameter(NumberParameterBuilder.create("y", 4));

		EchoSender log = new EchoSender();
		log.setName("myFunction");
		sender.addSender(log);

		sender.configure();
		sender.start();

		// See function 4, validates if input to the nested sender is the same as the output of the nested sender
		assertEquals("true", sender.sendMessageOrThrow(dummyInput,session).asString());
	}

	@ParameterizedTest
	@EnumSource(JavaScriptEngines.class)
	void promise(JavaScriptEngines engine) throws Exception {
		Message dummyInput = new Message("dummyinput");
		sender.setJsFileName("Javascript/JavascriptTest.js");
		sender.setJsFunctionName("promise");
		sender.setEngineName(engine);

		PromiseResultSender promise = new PromiseResultSender();
		promise.setName("result");
		sender.addSender(promise);

		sender.configure();
		sender.start();

		Message senderResult = sender.sendMessageOrThrow(dummyInput, session);
		assertTrue(Message.isEmpty(senderResult));
		Message promiseResult = promise.getPromiseResult();
		assertFalse(Message.isEmpty(promiseResult), "promise not called");
		assertEquals("success", promiseResult.asString());
	}

	private static class PromiseResultSender extends AbstractSenderWithParameters {
		private @Getter Message promiseResult = null;

		@Override
		public @Nonnull SenderResult sendMessage(@Nonnull Message message, @Nonnull PipeLineSession session) {
			promiseResult = message;
			return new SenderResult(Message.nullMessage());
		}
	};
}
