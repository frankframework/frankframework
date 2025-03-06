package org.frankframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ComponentLoaderTest {

	@Test
	public void testIfWeCanFindOurSelves() {
		List<Module> modules = ComponentLoader.findAllModules();
		assertNotNull(modules);
		long foundCoreModule = modules.stream().filter(module -> {
			try {
				return "frankframework-core".equals(module.getModuleInformation().getArtifactId());
			} catch (IOException e) {
				log.warn("unable to find manifest file in test", e);
				return false;
			}
		}).count();

		assertEquals(1L, foundCoreModule);
	}
}
