package org.frankframework.configuration.digester;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import org.frankframework.core.Adapter;
import org.frankframework.core.PipeLine;
import org.frankframework.core.Resource;
import org.frankframework.testutil.TestConfiguration;
import org.frankframework.util.SpringUtils;

public class IncludePipelinePartTest {

	@Test
	public void testConfigWarning() throws Exception {
		// Arrange
		String configXmlFile = "Digester/ConfigWithPipelinePartIncludes/Configuration.xml";
		TestConfiguration configuration = new TestConfiguration();

		Resource resource = Resource.getResource(configXmlFile);
		ConfigurationDigester digester = SpringUtils.createBean(configuration);

		// Act
		digester.digestConfiguration(configuration, resource);

		// Assert
		Adapter adapter = configuration.getRegisteredAdapter("Adapt1");
		PipeLine pipeLine = adapter.getPipeLine();

		assertNotNull(pipeLine.getPipe("ping"));
		assertNotNull(pipeLine.getPipe("pong"));
		assertNotNull(pipeLine.getPipe("fr1"));
		assertNotNull(pipeLine.getPipe("e2"));
	}
}
