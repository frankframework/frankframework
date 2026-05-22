package org.frankframework.pipes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

		pipe.stop();
		pipe = null;

		super.tearDown();
	}

	/**
	 * Configure the pipe
	 */
	protected void configurePipe() throws ConfigurationException {
		configureAdapter();
	}

	/**
	 * Configure and start the pipe
	 */
	protected void configureAndStartPipe() throws ConfigurationException {
		configureAdapter();

		// Start Pipeline because async start in Adapter may take too long.
		pipeline.start();
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
		Message message = input == null ? Message.nullMessage() : input;
		session.putIfAbsent(PipeLineSession.ORIGINAL_MESSAGE_KEY, message);
		return pipe.doPipe(message, session);
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
