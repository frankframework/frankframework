package org.frankframework.processors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.IPipe;
import org.frankframework.core.IValidator;
import org.frankframework.core.IWrapperPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLineExit;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.AbstractPipe;
import org.frankframework.pipes.AbstractValidator;
import org.frankframework.pipes.EchoPipe;
import org.frankframework.pipes.PutInSessionPipe;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TransactionManagerType;
import org.frankframework.util.CloseUtils;
import org.frankframework.util.SpringUtils;

class CorePipeLineProcessorTest {

	private TestConfiguration configuration;
	private Adapter adapter;
	private CorePipeLineProcessor processor;
	private PipeLineSession session;

	@BeforeEach
	void setUp() {
		configuration = TransactionManagerType.DATASOURCE.create(false);
		adapter = createAdapter(configuration);
		processor = configuration.createBean();
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

	/**
	 * PipeLine design:
	 * <ul>
	 *     <li>P1 -> P2 -> Success</li>
	 *     <li>V-inp -> Err1 -> Error</li>
	 *     <li>W-inp -> Err2 -> Error</li>
	 *     <li>W-out -> Err3 -> Error</li>
	 *     <li>V-out -> Err4 -> Error</li>
	 * </ul>
	 */
	private static IPipe[] createPipes() {
		PutInSessionPipe p1 = new PutInSessionPipe();
		p1.setName("p1");
		p1.addForward(new PipeForward("success", "p2"));
		p1.setSessionKey("s1");
		p1.setValue("1");

		PutInSessionPipe p2 = new PutInSessionPipe();
		p2.setName("p2");
		p2.addForward(new PipeForward("success", "success"));
		p2.setSessionKey("s2");
		p2.setValue("2");

		EchoPipe err1 = new EchoPipe();
		err1.setName("err1");
		err1.addForward(new PipeForward("success", "error"));
		err1.setGetInputFromFixedValue("err1");

		EchoPipe err2 = new EchoPipe();
		err2.setName("err2");
		err2.addForward(new PipeForward("success", "error"));
		err2.setGetInputFromFixedValue("err2");

		EchoPipe err3 = new EchoPipe();
		err3.setName("err3");
		err3.addForward(new PipeForward("success", "error"));
		err3.setGetInputFromFixedValue("err3");

		EchoPipe err4 = new EchoPipe();
		err4.setName("err4");
		err4.addForward(new PipeForward("success", "error"));
		err4.setGetInputFromFixedValue("err4");

		return new IPipe[]{p1, p2, err1, err2, err3, err4};
	}

	private static PipeLine createPipeLine(Adapter adapter, IPipe... testPipes) throws ConfigurationException {
		PipeLine pl = SpringUtils.createBean(adapter);
		adapter.setPipeLine(pl);
		for (IPipe pipe : testPipes) {
			pl.addPipe(pipe);
		}
		if (testPipes.length > 0) {
			IPipe firstPipe =  testPipes[0];
			pl.setFirstPipe(firstPipe.getName());
		}

		PipeLineExit success = new PipeLineExit();
		success.setName("success");
		success.setState(PipeLine.ExitState.SUCCESS);
		pl.addPipeLineExit(success);
		PipeLineExit error = new PipeLineExit();
		error.setName("error");
		error.setState(PipeLine.ExitState.ERROR);
		pl.addPipeLineExit(error);
		return pl;
	}

	private void addValidators(PipeLine pipeLine, boolean outputValidationFailsAll) throws ConfigurationException {
		IValidator inputValidator = new DummyValidator("fail-validator-input");
		inputValidator.addForward(new PipeForward("failure", "err1"));
		IWrapperPipe inputWrapper = new DummyWrapper("fail-wrap-input");
		inputWrapper.addForward(new PipeForward("failure", "err2"));
		IWrapperPipe outputWrapper = new DummyWrapper("fail-wrap-output");
		outputWrapper.addForward(new PipeForward("failure", "err3")); // This forward is currently ignored
		IValidator outputValidator;
		if (outputValidationFailsAll) {
			outputValidator = new DummyValidator("fail-validator-output", "err4");
		} else {
			outputValidator = new DummyValidator("fail-validator-output");
		}
		outputValidator.addForward(new PipeForward("failure", "err4"));

		pipeLine.setInputWrapper(inputWrapper);
		pipeLine.setOutputWrapper(outputWrapper);
		pipeLine.setInputValidator(inputValidator);
		pipeLine.setOutputValidator(outputValidator);
	}

	@Test
	void processPipeLineNoWrappersOrValidators() throws Exception {
		// Arrange
		IPipe[] pipes = createPipes();
		PipeLine pipeLine = createPipeLine(adapter, pipes);
		pipeLine.setStoreOriginalMessageWithoutNamespaces(true);

		configuration.configure();
		configuration.start();

		// Act
		PipeLineResult pipeLineResult = processor.processPipeLine(pipeLine, "id", new Message("<ns1:input xmlns:ns1=\"xyx\"/>"), session, "p1");

		// Assert
		assertTrue(pipeLineResult.isSuccessful(), "Expected successful Pipe result");
		assertEquals("<ns1:input xmlns:ns1=\"xyx\"/>", pipeLineResult.getResult().asString());
		assertEquals("<input/>", session.getString("originalMessageWithoutNamespaces"));
		assertEquals("1", session.getString("s1"));
		assertEquals("2", session.getString("s2"));
	}

	@Test
	void processPipeLineWithWrappersAndValidatorsSuccess() throws Exception {
		// Arrange
		IPipe[] pipes = createPipes();
		PipeLine pipeLine = createPipeLine(adapter, pipes);
		pipeLine.setStoreOriginalMessageWithoutNamespaces(true);

		addValidators(pipeLine, false);

		configuration.configure();
		configuration.start();

		// Act
		PipeLineResult pipeLineResult = processor.processPipeLine(pipeLine, "id", new Message("<ns1:input/>"), session, "p1");

		// Assert
		assertTrue(pipeLineResult.isSuccessful(), "Expected successful Pipe result");
		assertEquals("wrapping-success[wrapping-success[<ns1:input/>]]", pipeLineResult.getResult().asString());
		assertEquals("wrapping-success[<ns1:input/>]", session.getString("originalMessageWithoutNamespaces")); // Not wellformed XML so namespace not stripped
		assertEquals("1", session.getString("s1"));
		assertEquals("2", session.getString("s2"));
	}

	@ParameterizedTest
	@CsvSource({
		"fail-wrap-input, wrapping-success[err2]",
		"fail-validator-input, wrapping-success[err1]"
	})
	void processPipeLineWithWrappersAndValidatorsFailureOnInput(String input, String expected) throws Exception {
		// Arrange
		IPipe[] pipes = createPipes();
		PipeLine pipeLine = createPipeLine(adapter, pipes);
		pipeLine.setStoreOriginalMessageWithoutNamespaces(false);

		addValidators(pipeLine, false);

		configuration.configure();
		configuration.start();

		// Act
		PipeLineResult pipeLineResult = processor.processPipeLine(pipeLine, "id", new Message(input), session, "p1");

		// Assert
		assertFalse(pipeLineResult.isSuccessful(), "Expected failure Pipe result");
		assertEquals(expected, pipeLineResult.getResult().asString());
		assertFalse(session.containsKey("s1"), "Did not expect pipe 1 to be executed");
		assertFalse(session.containsKey("s2"), "Did not expect pipe 2 to be executed");
	}

	@ParameterizedTest
	@CsvSource({
		"fail-wrap-output, wrapping-success[fail-wrap-output], true, false",
		"fail-validator-output, wrapping-success[err4], false, false",
		"fail-validator-output, wrapping-success[err4], false, true"
	})
	void processPipeLineWithWrappersAndValidatorsFailureOnOutput(String input, String expected, boolean expectSuccess, boolean failErrorValidationToo) throws Exception {
		// Arrange
		IPipe[] pipes = createPipes();
		PipeLine pipeLine = createPipeLine(adapter, pipes);
		pipeLine.setStoreOriginalMessageWithoutNamespaces(false);

		addValidators(pipeLine, failErrorValidationToo);

		configuration.configure();
		configuration.start();

		// Act
		PipeLineResult pipeLineResult = processor.processPipeLine(pipeLine, "id", new Message(input), session, "p1");

		// Assert
		assertEquals(expectSuccess, pipeLineResult.isSuccessful(), "Expected " + (expectSuccess ? "success" : "failure") + " Pipe result");
		assertEquals(expected, pipeLineResult.getResult().asString());
		assertEquals("1", session.getString("s1"));
		assertEquals("2", session.getString("s2"));
	}

	private class DummyWrapper extends AbstractPipe implements IWrapperPipe {

		private final String failOnValue;

		private DummyWrapper(@NonNull String failOnValue) {
			this.failOnValue = failOnValue;
		}

		@Override
		public @NonNull PipeRunResult doPipe(@NonNull Message message, @NonNull PipeLineSession session) throws PipeRunException {
			try {
				String data = message.asString();
				if (data != null && data.contains(failOnValue)) {
					Message result = new Message("wrapping-failed[" + data + "]");
					return new PipeRunResult(findForward("failure"), result);
				}
				Message result = new Message("wrapping-success[" + data + "]");
				return new PipeRunResult(findForward("success"), result);
			} catch (IOException e) {
				throw new PipeRunException(this, "Failure to get data from message", e);
			}
		}
	}

	private class DummyValidator extends AbstractValidator {

		private final String[] failOnValue;

		private DummyValidator(String... failOnValue) {
			this.failOnValue = failOnValue;
		}

		@Override
		protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
			try {
				String data = messageToValidate.asString();
				for (String value : failOnValue) {
					if (StringUtils.isNotEmpty(data) && StringUtils.isNotEmpty(value) && data.contains(value)) {
						return findForward("failure");
					}
				}
				return findForward("success");
			} catch (IOException e) {
				throw new PipeRunException(this, "Failure to get data from message", e);
			}
		}
	}
}
