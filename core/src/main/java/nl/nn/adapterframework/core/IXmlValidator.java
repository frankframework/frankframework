package nl.nn.adapterframework.core;

import nl.nn.adapterframework.configuration.ConfigurationException;

public interface IXmlValidator extends IPipe {

	public ConfigurationException getConfigurationException();
//	public String getMessageRoot();
//	public String getEnvelopeRoot();
	
}
