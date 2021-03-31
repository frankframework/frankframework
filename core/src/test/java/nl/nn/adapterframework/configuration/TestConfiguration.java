package nl.nn.adapterframework.configuration;

public class TestConfiguration extends Configuration {

	//Configures a standalone configuration.
	//TODO create a spring configuration with a dummy scheduler
	public TestConfiguration() {
		super();
		setConfigured(true);
		refresh();
	}
}
