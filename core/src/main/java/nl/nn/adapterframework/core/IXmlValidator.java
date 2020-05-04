package nl.nn.adapterframework.core;

import java.util.Set;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.validation.XSD;

public interface IXmlValidator extends IPipe {

	public ConfigurationException getConfigurationException();

	public String getMessageRoot();
	
	public String getSchema();
	public String getSchemaLocation();
	public Set<XSD> getXsds() throws ConfigurationException;

	/**
	 * Provide additional generic documentation on the validation of the
	 * subsequent processing. This documentation will be included in generated
	 * schema's like WSDL or OpenApi
	 */
	public String getDocumentation();
}
