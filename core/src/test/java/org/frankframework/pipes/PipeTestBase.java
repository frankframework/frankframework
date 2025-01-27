package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ConfiguredTestBase;
import org.frankframework.core.IPipe;
import org.frankframework.core.PipeForward;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunException;
import org.frankframework.core.PipeRunResult;
import org.frankframework.stream.Message;
import org.frankframework.stream.UrlMessage;
import org.frankframework.testutil.ThrowingAfterCloseInputStream;
import org.frankframework.util.SpringUtils;

public abstract class PipeTestBase<P extends IPipe> extends ConfiguredTestBase {

	protected P pipe;

	public abstract P createPipe() throws ConfigurationException;

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		super.setUp();
		pipe = createPipe();
		SpringUtils.autowireByType(adapter, pipe);
		pipe.addForward(new PipeForward("success", "READY"));
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipeline.addPipe(pipe);
	}

	@Override
	@AfterEach
	public void tearDown() {
		getConfigurationWarnings().destroy();
		getConfigurationWarnings().afterPropertiesSet();
		pipe = null;
		super.tearDown();
	}

	/**
	 * Configure the pipe
	 */
	protected void configurePipe() throws ConfigurationException {
		configurePipeline();
	}

	/**
	 * Configure and start the pipe
	 */
	protected void configureAndStartPipe() throws ConfigurationException {
		configurePipeline();
		pipe.start();
	}

	/*
	 * use these methods to execute pipe, instead of calling pipe.doPipe directly. This allows for
	 * integrated testing of streaming.
	 */
	protected PipeRunResult doPipe(String input) throws PipeRunException {
		return doPipe(pipe, new Message(input), session);
	}
	protected PipeRunResult doPipe(Message input) throws PipeRunException {
		return doPipe(pipe, input, session);
	}

	protected PipeRunResult doPipe(P pipe, Object input, PipeLineSession session) throws PipeRunException {
		return doPipe(pipe, Message.asMessage(input), session);
	}

	protected PipeRunResult doPipe(final P pipe, final Message input, final PipeLineSession session) throws PipeRunException {
		if (input != null && input.isRequestOfType(InputStream.class)) {
			// Wrap input-stream in a stream that forces IOExceptions after it is closed; close the session
			// (and thus any messages attached) after running the pipe so that reading the result message
			// will verify the original input-stream of the input-message is not used beyond due-date.
			// Do not close session when input message did not have a stream, due to some tests depending on
			// an open session after running the pipe.
			try (PipeLineSession ignored = session) {
				input.unscheduleFromCloseOnExitOf(session);
				Message wrappedInput;
				try {
					wrappedInput = new Message(new ThrowingAfterCloseInputStream(input.asInputStream()));
				} catch (IOException e) {
					throw new PipeRunException(pipe, "Error getting inputStream of input message", e);
				}
				wrappedInput.closeOnCloseOf(session);
				session.putIfAbsent(PipeLineSession.ORIGINAL_MESSAGE_KEY, wrappedInput);
				PipeRunResult result = pipe.doPipe(wrappedInput, session);
				session.unscheduleCloseOnSessionExit(result.getResult());
				return result;
			}
		}
		session.computeIfAbsent(PipeLineSession.ORIGINAL_MESSAGE_KEY, k -> input);
		return pipe.doPipe(input, session);
	}

	/**
	 * Retrieves a file from the test-classpath, with the pipe's classname as basepath.
	 */
	protected Message getResource(String resource) {
		String base = pipe.getClass().getSimpleName();
		if(StringUtils.isEmpty(base)) {
			Class<?> superClass = pipe.getClass().getSuperclass();
			if(superClass != null) {
				base = superClass.getSimpleName();
			}
		}
		assertTrue(StringUtils.isNotEmpty(base), "unable to determine ["+pipe+"] name");
		String relativeUrl = FilenameUtils.normalize("/Pipes/" + base + "/" + resource, true);

		URL url = PipeTestBase.class.getResource(relativeUrl);
		assertNotNull(url, "unable to find resource ["+resource+"] in path ["+relativeUrl+"]");
		return new UrlMessage(url);
	}
}
