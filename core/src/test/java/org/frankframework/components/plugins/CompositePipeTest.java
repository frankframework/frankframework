package org.frankframework.components.plugins;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.net.URL;
import java.time.Duration;
import java.util.Deque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import org.frankframework.components.FrankPlugin;
import org.frankframework.core.PipeLine;
import org.frankframework.core.PipeLine.ExitState;
import org.frankframework.core.PipeLineResult;
import org.frankframework.core.PipeLineSession;
import org.frankframework.core.PipeRunResult;
import org.frankframework.parameters.IParameter;
import org.frankframework.pipes.PipeTestBase;
import org.frankframework.stream.Message;
import org.frankframework.testutil.ParameterBuilder;
import org.frankframework.testutil.TestFileUtils;

public class CompositePipeTest extends PipeTestBase<CompositePipe> {
	private PluginContextEventListener listener;
	private PluginLoader loader;
	private FrankPlugin frankPlugin;

	@Override
	public CompositePipe createPipe() {
		return new CompositePipe(frankPlugin);
	}

	@Override
	@BeforeEach
	public void setUp() throws Exception {
		loader = createPluginLoader();
		frankPlugin = spy();
		doReturn(loader).when(frankPlugin).getPluginLoader(any(ApplicationContext.class));

		super.setUp();
		session.put(PipeLineSession.MESSAGE_ID_KEY, testMessageId);
		session.put(PipeLineSession.CORRELATION_ID_KEY, testCorrelationId);

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

		configureAndStartPipe();

		await().pollInterval(1, TimeUnit.SECONDS)
			.atMost(Duration.ofSeconds(10))
			.until(() -> listener.getEvents().size() == 3);

		// Verify above method 'pipeline.configure()' has configured everything.
		assertEquals(3, listener.getEvents().size());
		assertTrue(listener.getEvents().remove("ContextRefreshedEvent"));
		assertTrue(listener.getEvents().remove("ContextStartedEvent"));
		assertTrue(listener.getEvents().remove("ContextStartedEvent"));

		verify(frankPlugin, times(1)).configure();
		verify(frankPlugin, times(1)).start();

		// Stub actual processing.
		doAnswer(i -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setResult(i.getArgument(1));
			PipeLineSession session = i.getArgument(2);
			assertNull(session.getMessageId());
			assertEquals(testCorrelationId, session.getCorrelationId());
			plr.setState(ExitState.SUCCESS);
			return plr;
		}).when(frankPlugin).process(anyString(), any(Message.class), any(PipeLineSession.class));

		// Process dummy message
		Message ignored = Message.nullMessage();
		PipeRunResult result = doPipe(ignored);

		assertTrue(Message.isNull(result.getResult()));

		pipeline.stop();

		verify(frankPlugin, times(1)).stop();
		assertEquals(2, listener.getEvents().size());
		assertTrue(listener.getEvents().remove("ContextStoppedEvent"));
		assertTrue(listener.getEvents().remove("ContextStoppedEvent"));
	}

	@Test
	public void initializeAndLoadPluginWithParameters() throws Exception {
		pipe.setPlugin("demo-plugin");
		pipe.setRef("demo-test-part.xml");
		IParameter parameter = spy(ParameterBuilder.create("test", "value"));
		pipe.addParameter(parameter);

		assertEquals(0, listener.getEvents().size());

		configureAndStartPipe();

		await().pollInterval(1, TimeUnit.SECONDS)
			.atMost(Duration.ofSeconds(10))
			.until(() -> listener.getEvents().size() == 3);

		// Verify above method 'pipeline.configure()' has configured everything.
		assertEquals(3, listener.getEvents().size());
		assertTrue(listener.getEvents().remove("ContextRefreshedEvent"));
		assertTrue(listener.getEvents().remove("ContextStartedEvent"));
		assertTrue(listener.getEvents().remove("ContextStartedEvent"));

		verify(frankPlugin, times(1)).configure();
		verify(parameter, times(1)).configure();

		// Stub actual processing.
		doAnswer(i -> {
			PipeLineResult plr = new PipeLineResult();
			plr.setResult(i.getArgument(1));
			PipeLineSession session = i.getArgument(2);
			assertNull(session.getMessageId());
			assertEquals(testCorrelationId, session.getCorrelationId());
			assertEquals("value", session.getString("test"));
			plr.setState(ExitState.SUCCESS);
			return plr;
		}).when(frankPlugin).process(anyString(), any(Message.class), any(PipeLineSession.class));

		// Process dummy message
		Message ignored = Message.nullMessage();
		PipeRunResult result = doPipe(ignored);

		assertTrue(Message.isNull(result.getResult()));

		pipeline.stop();

		verify(frankPlugin, times(1)).stop();
		assertEquals(2, listener.getEvents().size());
		assertTrue(listener.getEvents().remove("ContextStoppedEvent"));
		assertTrue(listener.getEvents().remove("ContextStoppedEvent"));
	}

	@Test
	public void initializeAndLoadPluginDefaultRef() throws Exception {
		pipe.setPlugin("demo-plugin");

		assertEquals(0, listener.getEvents().size());

		// For now we just verify there are no ConfigurationExceptions
		configureAdapter();
	}

	private static class PluginContextEventListener implements ApplicationListener<ApplicationContextEvent> {
		private final Deque<String> events = new LinkedBlockingDeque<>(16);

		public Deque<String> getEvents() {
			return events;
		}

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
			if (event.getApplicationContext() instanceof PipeLine) {
				events.push(event.getClass().getSimpleName());
			}
		}
	}
}
