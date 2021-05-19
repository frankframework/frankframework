package nl.nn.adapterframework.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.hamcrest.core.StringEndsWith;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.testutil.TestConfiguration;

public class PipeLineTest {

	@Test
	public void testDuplicateExits() throws ConfigurationException {
		TestConfiguration configuration = new TestConfiguration();
		Adapter adapter = new Adapter();
		PipeLine pipeline = new PipeLine();
		pipeline.setApplicationContext(configuration);
		PipeLineExit exit = new PipeLineExit();
		exit.setPath("success");
		exit.setState("SUCCESS");
		pipeline.registerPipeLineExit(exit);
		pipeline.registerPipeLineExit(exit);
		adapter.setPipeLine(pipeline);

		List<String> warnings = configuration.getConfigurationWarnings().getWarnings();
		assertEquals(warnings.size(), 1);
		String lastWarning = warnings.get(warnings.size()-1);
		assertThat(lastWarning,StringEndsWith.endsWith("PipeLine exit named [success] already exists"));
	}
}
