package org.frankframework.receivers;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.IWrapperPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineSession;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.testdummies.TestDummyValidator;
import org.frankframework.testdummies.TestDummyWrapper;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.SpringUtils;

class ReceiverValidatorsTest {

	private TestConfiguration configuration;
	private Adapter adapter;
	private Receiver<?> receiver;
	private PipeLineSession session;

	@BeforeEach
	void setUp() {
		configuration = TransactionManagerType.DATASOURCE.create(false);
		adapter = createAdapter(configuration);
		session = new PipeLineSession();
	}

	@AfterEach
	void tearDown() {
		CloseUtils.closeSilently(session, adapter, configuration);
	}

	private static Adapter createAdapter(TestConfiguration configuration) {
		Adapter adapter = configuration.createBean();
		adapter.setName("TEST");
		configuration.addAdapter(adapter);
		return adapter;
	}

	private static Receiver<?> createReceiver(Adapter adapter) {
		Receiver<String> receiver = SpringUtils.createBean(adapter);
		receiver.setName("TEST");
		JavaListener<String> listener = new JavaListener<>();
		receiver.setListener(listener);
		
		return receiver;
	}

	private static void addValidators(Receiver<?> receiver, boolean addFailureForwards) throws ConfigurationException {
		IValidator inputValidator = new TestDummyValidator("fail-validator-input");
		IWrapperPipe inputWrapper = new TestDummyWrapper("fail-wrap-input");
		IWrapperPipe outputWrapper = new TestDummyWrapper("fail-wrap-output");
		IValidator outputValidator = new TestDummyValidator("fail-validator-output");

		if (addFailureForwards) {
			inputValidator.addForward(new PipeForward("failure", "error"));
			inputWrapper.addForward(new PipeForward("failure", "error"));
			outputWrapper.addForward(new PipeForward("failure", "error"));
			outputValidator.addForward(new PipeForward("failure", "error"));
		}

		receiver.setInputWrapper(inputWrapper);
		receiver.setOutputWrapper(outputWrapper);
		receiver.setInputValidator(inputValidator);
		receiver.setOutputValidator(outputValidator);

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
		success.setEmpty(expectEmptyResult);
		success.setSkipValidation(skipWrappingValidation);
		success.setSkipWrapping(skipWrappingValidation);
		pl.addPipeLineExit(success);

		PipeLineExit error = new PipeLineExit();
		error.setName("error");
		error.setState(PipeLine.ExitState.ERROR);
		error.setEmpty(expectEmptyResult);
		error.setSkipValidation(skipWrappingValidation);
		error.setSkipWrapping(skipWrappingValidation);
		pl.addPipeLineExit(error);

		return pl;
	}

	@Test
	void testDoNotForwardToPipe() throws ConfigurationException {
		// Arrange
		receiver = createReceiver(adapter);
		PipeLine pipeLine = createPipeLine(adapter, false, false);

		addValidators(receiver, false);

		receiver.getInputValidator().addForward(new PipeForward(PipeForward.FAILURE_FORWARD_NAME, pipeLine.getFirstPipe()));

		// Act / Assert
		ConfigurationException configurationException = assertThrows(ConfigurationException.class, receiver::configure);
		assertEquals("Receiver TEST - InputValidator can only forward errors directly to a Pipeline Exit", configurationException.getMessage());
	}

	@Test
	void testAutoAddDefaultErrorForward() throws ConfigurationException {
		// Arrange
		receiver = createReceiver(adapter);
		PipeLine pipeLine = createPipeLine(adapter, false, false);
		addValidators(receiver, false);

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
}
