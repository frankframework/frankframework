package org.frankframework.testutil;

import org.jspecify.annotations.NonNull;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A minimal Spring ApplicationContext which contains no beans and no configuration.
 * Enables the use of
 * @SpringJUnitConfig(initializers = {SpringRootInitializer.class})
 * And
 * @WithMockUser(...)
 *
 */
public class SpringRootInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(@NonNull ConfigurableApplicationContext applicationContext) {
		// Empty method, nothing to initialize
	}

}
