package nl.nn.adapterframework.pipes;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ConfiguredTestBase;
import nl.nn.adapterframework.core.IPipe;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.stream.UrlMessage;
import nl.nn.adapterframework.util.FilenameUtils;

public abstract class PipeTestBase<P extends IPipe> extends ConfiguredTestBase {

	protected P pipe;

	public abstract P createPipe() throws ConfigurationException;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		pipe = createPipe();
		autowireByType(pipe);
		pipe.registerForward(new PipeForward("success", "READY"));
		pipe.setName(pipe.getClass().getSimpleName()+" under test");
		pipeline.addPipe(pipe);
	}

	@Override
	public void tearDown() throws Exception {
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
	protected void configureAndStartPipe() throws ConfigurationException, PipeStartException {
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
	protected PipeRunResult doPipe(P pipe, Message input, PipeLineSession session) throws PipeRunException {
		session.computeIfAbsent(PipeLineSession.originalMessageKey, k -> input);
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
		assertTrue("unable to determine ["+pipe+"] name", StringUtils.isNotEmpty(base));
		String relativeUrl = FilenameUtils.normalize("/Pipes/" + base + "/" + resource, true);

		URL url = PipeTestBase.class.getResource(relativeUrl);
		assertNotNull("unable to find resource ["+resource+"] in path ["+relativeUrl+"]", url);
		return new UrlMessage(url);
	}
}
