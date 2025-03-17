package org.frankframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

import org.frankframework.util.Environment;
import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ComponentLoaderTest {

	@Test
	public void locateCommonsModule() {
		List<Module> modules = ComponentLoader.findAllModules();
		assertNotNull(modules);
		long foundCoreModule = modules.stream().filter(module -> {
			try {
				ModuleInformation info = module.getModuleInformation();
				log.debug("found module: {}", info);
				return "frankframework-commons".equals(info.getArtifactId());
			} catch (IOException e) {
				log.warn("unable to find manifest file in test", e);
				return false;
			}
		}).count();

		assertEquals(1L, foundCoreModule);
	}

	@Test
	public void testIfWeCanReadManifest() throws IOException {
		URL jarFileWithManifest = ComponentLoaderTest.class.getResource("/ClassLoader/zip/myConfig.zip");
		assertNotNull(jarFileWithManifest);
		Manifest manifest = Environment.getManifest(jarFileWithManifest);
		ModuleInformation info = new ModuleInformation(manifest);
		assertEquals("myConfig", info.getTitle());
	}
}
