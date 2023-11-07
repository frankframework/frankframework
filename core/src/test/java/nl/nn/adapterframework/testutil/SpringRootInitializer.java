package nl.nn.adapterframework.testutil;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * A minimal Spring ApplicationContext which contains no beans and no configuration.
 * Enables the use of
 * <pre>
 * @RunWith(SpringJUnit4ClassRunner.class)
 * @ContextConfiguration(initializers = {SpringRootInitializer.class})
 * </pre>
 * And
 * <pre>
 * @WithMockUser(...)
 * </pre>
 *
 */
public class SpringRootInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		// Empty method, nothing to initialize
	}

}
