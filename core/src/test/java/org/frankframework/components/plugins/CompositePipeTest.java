package org.frankframework.components.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import lombok.Getter;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.TestFileUtils;

public class CompositePipeTest extends PipeTestBase<CompositePipe> {
	private PluginContextEventListener listener;
	private PluginLoader loader;
	private PipeLine pipeline;

	@Override
	public CompositePipe createPipe() throws ConfigurationException {
		return new CompositePipe(loader, pipeline);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		loader = createPluginLoader();
		pipeline = mock();

		super.setUp();

		loader.start();
		listener = new PluginContextEventListener();
		getConfiguration().addApplicationListener(listener);
	}

	private PluginLoader createPluginLoader() {
		try {
			URL url = TestFileUtils.getTestFileURL("/Plugins");
			assertNotNull(url);
			File directory = new File(url.toURI());

			PluginLoader pluginLoader = new PluginLoader(directory.getCanonicalPath());
			autowireByType(pluginLoader);
			return pluginLoader;
		} catch (Exception e) {
			return fail("unable to create PluginLoader", e);
		}
	}

	@Override
	@AfterEach
	public void tearDown() {
		if (listener != null) {
			getConfiguration().removeApplicationListener(listener);
		}

		loader.stop();
		super.tearDown();
	}

	@Test
	public void initializeAndLoadPlugin() throws Exception {
		pipe.setPlugin("demo-plugin");
		pipe.setRef("demo-test-part.xml");

		assertEquals(0, listener.getEvents().size());

		configureAdapter();

		assertEquals(0, listener.getEvents().size());

		verify(pipeline, times(1)).configure();
		doAnswer(i -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setResult(i.getArgument(1));
			plr.setState(ExitState.SUCCESS);
			return plr;
		}).when(pipeline).process(anyString(), any(Message.class), any(PipeLineSession.class));

		Message ignored = Message.nullMessage();
		PipeRunResult result = doPipe(ignored);

		assertTrue(Message.isNull(result.getResult()));
	}

	@Test
	public void initializeAndLoadPluginDefaultRef() throws Exception {
		pipe.setPlugin("demo-plugin");

		assertEquals(0, listener.getEvents().size());

		// For now we just verify there are no ConfigurationExceptions
		configureAdapter();
	}

	private static class PluginContextEventListener implements ApplicationListener<ApplicationContextEvent> {
		@Getter
		private final Deque<String> events = new ArrayDeque<>();

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
			if (!(event.getApplicationContext() instanceof Adapter)) {
				events.push(event.getClass().getSimpleName());
			}
		}
	}
}
