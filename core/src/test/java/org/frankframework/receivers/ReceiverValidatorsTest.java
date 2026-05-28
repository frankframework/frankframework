package org.frankframework.receivers;

import static org.frankframework.testutil.mock.WaitUtils.waitWhileInState;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IListener;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.stream.Message;
import org.frankframework.testdummies.TestDummyErrorMessageFormatter;
import org.frankframework.testdummies.TestDummyValidator;
import org.frankframework.testdummies.TestDummyWrapper;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.RunState;
import org.frankframework.util.SpringUtils;

class ReceiverValidatorsTest {

	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver<String> receiver;
	private JavaListener<String> listener;
	private PipeLineSession session;

	@BeforeEach
	void setUp() {
		configuration = TransactionManagerType.DATASOURCE.create(false);
		adapter = createAdapter(configuration);
		listener = createListener();
		receiver = createReceiver(adapter, listener);

		session = new PipeLineSession();
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session, adapter, configuration);
	}

	private static Adapter createAdapter(TestConfiguration configuration) {
		Adapter adapter = configuration.createBean();
		adapter.setName("TEST");
		adapter.setErrorMessageFormatter(new TestDummyErrorMessageFormatter());
		configuration.addAdapter(adapter);
		return adapter;
	}

	private static Receiver<String> createReceiver(Adapter adapter, IListener<String> listener) {
		Receiver<String> receiver = SpringUtils.createBean(adapter);
		receiver.setName("TEST");
		receiver.setListener(listener);
		adapter.addReceiver(receiver);
		return receiver;
	}

	private static @NonNull JavaListener<String> createListener() {
		JavaListener<String> listener = new JavaListener<>();
		listener.setName("TEST");
		return listener;
	}

	private static void addValidators(Receiver<?> receiver, boolean addFailureForwards, boolean dualModeValidation) {
		TestDummyValidator inputValidator;
		if (dualModeValidation) {
			inputValidator = new TestDummyValidator(true, "fail-validator-input", "InputWrapper[fail-validator-output]");
		} else {
			inputValidator = new TestDummyValidator("fail-validator-input");
		}
		TestDummyWrapper inputWrapper = new TestDummyWrapper("fail-wrap-input");
		TestDummyWrapper outputWrapper = new TestDummyWrapper("fail-wrap-output");
		TestDummyValidator outputValidator = new TestDummyValidator("fail-validator-output");

		if (addFailureForwards) {
			inputValidator.addForward(new PipeForward("failure", "error"));
			inputWrapper.addForward(new PipeForward("failure", "error"));
			outputWrapper.addForward(new PipeForward("failure", "error"));
			outputValidator.addForward(new PipeForward("failure", "error"));
		}

		receiver.setInputWrapper(inputWrapper);
		receiver.setOutputWrapper(outputWrapper);
		receiver.setInputValidator(inputValidator);
		if (!dualModeValidation) {
			receiver.setOutputValidator(outputValidator);
		}
	}

	private static PipeLine createPipeLine(Adapter adapter, boolean expectEmptyResult, boolean skipWrappingValidation) throws ConfigurationException {
		PipeLine pl = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pl);

		EchoPipe echoPipe = new EchoPipe();
		echoPipe.setName("EchoPipe");
		pl.addPipe(echoPipe);
		pl.setFirstPipe(echoPipe.getName());

		PipeLineExit success = new PipeLineExit();
		success.setName("success");
		success.setState(PipeLine.ExitState.SUCCESS);
		success.setCode(200);
		success.setEmpty(expectEmptyResult);
		success.setSkipValidation(skipWrappingValidation);
		success.setSkipWrapping(skipWrappingValidation);
		pl.addPipeLineExit(success);

		PipeLineExit error = new PipeLineExit();
		error.setName("error");
		error.setState(PipeLine.ExitState.ERROR);
		error.setCode(400);
		error.setEmpty(expectEmptyResult);
		error.setSkipValidation(skipWrappingValidation);
		error.setSkipWrapping(skipWrappingValidation);
		pl.addPipeLineExit(error);

		return pl;
	}

	private static void startConfiguration(TestConfiguration configuration, Adapter adapter) {
		assertDoesNotThrow(configuration::configure);
		assertDoesNotThrow(configuration::start);

		waitWhileInState(adapter, RunState.STOPPED);
		waitWhileInState(adapter, RunState.STARTING);
	}

	@Test
	void testDoNotForwardToPipe() throws ConfigurationException {
		// Arrange
		PipeLine pipeLine = createPipeLine(adapter, false, false);

		addValidators(receiver, false, false);

		receiver.getInputValidator().addForward(new PipeForward(PipeForward.FAILURE_FORWARD_NAME, pipeLine.getFirstPipe()));

		// Act / Assert
		ConfigurationException configurationException = assertThrows(ConfigurationException.class, receiver::configure);
		assertEquals("Receiver TEST - InputValidator can only forward errors directly to a Pipeline Exit", configurationException.getMessage());
	}

	@Test
	void testAutoAddDefaultErrorForward() throws ConfigurationException {
		// Arrange
		receiver = createReceiver(adapter, createListener());
		createPipeLine(adapter, false, false);
		addValidators(receiver, false, false);

		// Act / Assert
		assertDoesNotThrow(receiver::configure);

		validateDefaultPipeForwards(receiver.getInputValidator());
		validateDefaultPipeForwards(receiver.getOutputValidator());
		validateDefaultPipeForwards(receiver.getInputWrapper());
		validateDefaultPipeForwards(receiver.getOutputWrapper());
	}

	private static void validateDefaultPipeForwards(IPipe pipe) {
		PipeForward successForward = pipe.findForward(PipeForward.SUCCESS_FORWARD_NAME);
		assertNotNull(successForward);

		PipeForward failureForward = pipe.findForward(PipeForward.FAILURE_FORWARD_NAME);
		assertNotNull(failureForward);
		assertEquals("error", failureForward.getPath());
	}

	@Test
	void testReceiverNoWrappersOrValidators() throws Exception {
		// Arrange
		createPipeLine(adapter, false, false);

		startConfiguration(configuration, adapter);

		Message inputMessage = new Message("input");
		MessageWrapper<String> messageWrapper = new MessageWrapper<>(inputMessage, "my-mid", "my-cid");

		// Act
		Message result = assertDoesNotThrow(() -> receiver.processRequest(listener, messageWrapper, session));

		// Assert
		assertNotNull(result);
		assertEquals("input", result.asString());
		assertEquals("SUCCESS", session.getString(PipeLineSession.EXIT_STATE_CONTEXT_KEY));
		assertEquals(200, session.getInteger(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
	}

	@Test
	void testReceiverWrappersOrValidatorsSuccess() throws Exception {
		// Arrange
		createPipeLine(adapter, false, false);
		addValidators(receiver, true, false);
		startConfiguration(configuration, adapter);

		Message inputMessage = new Message("input");
		MessageWrapper<String> messageWrapper = new MessageWrapper<>(inputMessage, "my-mid", "my-cid");

		// Act
		Message result = assertDoesNotThrow(() -> receiver.processRequest(listener, messageWrapper, session));

		// Assert
		assertNotNull(result);
		assertEquals("wrapping-successReceiver TEST - OutputWrapper[wrapping-successReceiver TEST - InputWrapper[input]]", result.asString());
		assertEquals("SUCCESS", session.getString(PipeLineSession.EXIT_STATE_CONTEXT_KEY));
		assertEquals(200, session.getInteger(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
	}

	@ParameterizedTest
	@CsvSource({
			"false, fail-validator-input, 'Error in [Receiver TEST - InputValidator]: Forward to: name=failure, path=error [fail-validator-input]'",
			"true, fail-validator-input, 'Error in [Receiver TEST - InputValidator]: Forward to: name=failure, path=error [fail-validator-input]'",
			"false, fail-wrap-input, 'Error in [Receiver TEST - InputWrapper]: Forward to: name=failure, path=error [wrapping-failedReceiver TEST - InputWrapper[fail-wrap-input]]'",
			"false, fail-wrap-output, 'Error in [Receiver TEST - OutputWrapper]: Forward to: name=failure, path=error [wrapping-failedReceiver TEST - OutputWrapper[wrapping-successReceiver TEST - InputWrapper[fail-wrap-output]]]'",
			"false, fail-validator-output, 'Error in [Receiver TEST - OutputValidator]: Forward to: name=failure, path=error [wrapping-successReceiver TEST - OutputWrapper[wrapping-successReceiver TEST - InputWrapper[fail-validator-output]]]'",
			"true, fail-validator-output, 'Error in [Receiver TEST - OutputValidator]: Forward to: name=failure, path=error [wrapping-successReceiver TEST - OutputWrapper[wrapping-successReceiver TEST - InputWrapper[fail-validator-output]]]'",
	})
	void testReceiverWithWrappersAndValidatorsFailures(boolean dualModeValidator, String input, String expectedMessage) throws Exception {
		// Arrange
		createPipeLine(adapter, false, false);
		addValidators(receiver, true, dualModeValidator);
		startConfiguration(configuration, adapter);

		Message inputMessage = new Message(input);
		MessageWrapper<String> messageWrapper = new MessageWrapper<>(inputMessage, "my-mid", "my-cid");

		// Act
		Message result = assertDoesNotThrow(() -> receiver.processRequest(listener, messageWrapper, session));

		// Assert
		assertNotNull(result);
		assertEquals(expectedMessage, result.asString());
		assertEquals("ERROR", session.getString(PipeLineSession.EXIT_STATE_CONTEXT_KEY));
		assertEquals(400, session.getInteger(PipeLineSession.EXIT_CODE_CONTEXT_KEY));
	}
}
