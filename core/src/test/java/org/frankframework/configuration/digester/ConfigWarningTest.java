package org.frankframework.configuration.digester;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.frankframework.core.Resource;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

public class ConfigWarningTest {

	@Test
	public void testConfigWarning() throws Exception {
		// Arrange
		String configXmlFile = "Digester/ConfigurationWithWarnings/Configuration.xml";
		TestConfiguration configuration = new TestConfiguration();

		Resource resource = Resource.getResource(configXmlFile);
		ConfigurationDigester digester = SpringUtils.createBean(configuration);

		// Act
		digester.digestConfiguration(configuration, resource);

		// Assert
		List<String> warnings = configuration.getConfigurationWarnings().getWarnings();

		assertThat(warnings, hasItems(containsString("C1 Warning"), containsString("A1 Warning"), containsString("JL2 Warning")));
		assertThat(warnings, not(hasItems(containsString("R1 Warning"))));
	}
}
