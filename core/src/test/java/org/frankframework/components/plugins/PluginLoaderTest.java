package org.frankframework.components.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginState;
import org.pf4j.PluginWrapper;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;

import lombok.Getter;

import org.frankframework.testutil.TestConfiguration;
import org.frankframework.testutil.TestFileUtils;
import org.frankframework.util.SpringUtils;

public class PluginLoaderTest {

	private PluginLoader createPluginLoader() {
		try {
			URL url = TestFileUtils.getTestFileURL("/Plugins");
			assertNotNull(url);
			File directory = new File(url.toURI());

			return new PluginLoader(directory.getCanonicalPath());
		} catch (Exception e) {
			return fail("unable to create PluginLoader", e);
		}
	}

	@Test
	public void initializeAndLoadPlugins() {
		PluginLoader loader = createPluginLoader();
		loader.afterPropertiesSet();

		PluginWrapper plugin = loader.findPlugin("test-plugin");
		assertNotNull(plugin, "was not able to find test plugin");

		PluginInfo descriptor = assertInstanceOf(PluginInfo.class, plugin.getDescriptor());
		assertEquals("org.frankframework:plugin-template:20250919-0917:0.0.1-SNAPSHOT", descriptor.getArtifact().toString());
	}

	@Test
	public void testEvents() throws Exception {
		TestConfiguration config = new TestConfiguration();
		PluginLoader loader = createPluginLoader();
		config.autowireByType(loader);
		SpringUtils.registerSingleton(config, "pluginLoader", loader);

		PluginContextEventListener listener = new PluginContextEventListener();
		config.addApplicationListener(listener);

		PluginWrapper plugin = loader.findPlugin("demo-plugin");
		assertNotNull(plugin, "was not able to find test plugin");

		PluginInfo descriptor = assertInstanceOf(PluginInfo.class, plugin.getDescriptor());
		assertEquals("org.frankframework:demo-plugin:20260306-0816:0.0.1-SNAPSHOT", descriptor.getArtifact().toString());
		assertEquals(PluginState.RESOLVED, plugin.getPluginState());
		assertEquals(0, listener.getEvents().size());

		loader.start();

		Deque<String> eventQueue = listener.getEvents();
		assertEquals(2, eventQueue.size());
		assertEquals("ContextStartedEvent", eventQueue.pop());
		assertEquals("ContextRefreshedEvent", eventQueue.pop());

		loader.stop();

		assertEquals(1, listener.getEvents().size());
		assertEquals("ContextStoppedEvent", eventQueue.pop());
	}

	private static class PluginContextEventListener implements ApplicationListener<ApplicationContextEvent> {
		@Getter
		private final Deque<String> events = new ArrayDeque<>();

		@Override
		public void onApplicationEvent(ApplicationContextEvent event) {
			events.push(event.getClass().getSimpleName());
		}
	}
}
