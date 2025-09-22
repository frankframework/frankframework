package org.frankframework.components.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URL;

import org.junit.jupiter.api.Test;
import org.pf4j.PluginWrapper;

import org.frankframework.testutil.TestFileUtils;

public class PluginLoaderTest {

	@Test
	public void initializeAndLoadPlugins() throws Exception {
		URL url = TestFileUtils.getTestFileURL("/Plugins");
		assertNotNull(url);
		File directory = new File(url.toURI());

		PluginLoader loader = new PluginLoader(directory.getCanonicalPath());
		loader.afterPropertiesSet();

		PluginWrapper plugin = loader.findPlugin("test-plugin");
		assertNotNull(plugin, "was not able to find test plugin");

		PluginInfo descriptor = assertInstanceOf(PluginInfo.class, plugin.getDescriptor());
		assertEquals("org.frankframework:plugin-template:20250919-0917:0.0.1-SNAPSHOT", descriptor.getArtifact().toString());
	}
}
