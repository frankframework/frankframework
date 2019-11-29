package nl.nn.adapterframework.scheduler;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class DatabaseJobDef extends JobDef {

	public void configure() throws ConfigurationException {
		try {
			setFunction(JobDefFunctions.SEND_MESSAGE.getName());
		} catch (ConfigurationException e) {}

		super.configure(null);
	}
}
