package org.frankframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.jar.Manifest;

import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

import org.frankframework.testutil.TestAssertions;
import org.frankframework.util.Environment;

@Log4j2
public class ComponentLoaderTest {

	@Test
	public void locateCommonsModule() {
		List<Module> modules = ComponentLoader.findAllModules();

		// When running as a Maven build, this test should find modules.
		if (TestAssertions.isTestRunningWithSurefire()) {
			assertFalse(modules.isEmpty(), "no modules found, failing!");
		} else { // When running in an IDE (IntelliJ) this may be an empty list, if so skip the test.
			assumeFalse(modules.isEmpty(), "no modules found, skipping!");
		}

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
