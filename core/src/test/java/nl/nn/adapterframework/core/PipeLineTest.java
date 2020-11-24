package nl.nn.adapterframework.core;

import static org.junit.Assert.assertThat;

import org.hamcrest.core.StringEndsWith;
import org.junit.Test;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.configuration.ConfigurationWarnings;

public class PipeLineTest {

	@Test
	public void testDuplicateExits() throws ConfigurationException {
		Adapter adapter = new Adapter();
		PipeLine pipeline = new PipeLine();
		PipeLineExit exit = new PipeLineExit();
		exit.setPath("success");
		exit.setState("SUCCESS");
		pipeline.registerPipeLineExit(exit);
		pipeline.registerPipeLineExit(exit);
		adapter.setPipeLine(pipeline);
		
		String lastWarning = ConfigurationWarnings.getInstance().getLast();
		assertThat(lastWarning,StringEndsWith.endsWith("PipeLine exit named [success] already exists"));
	}
}
