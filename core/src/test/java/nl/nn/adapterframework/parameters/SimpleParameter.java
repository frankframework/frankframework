package nl.nn.adapterframework.parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;

public class SimpleParameter extends ParameterValue {

	private SimpleParameter(Parameter type, Object value) {
		super(type, value);
	}

	public SimpleParameter(String parameterName, String type, Object value) throws ConfigurationException {
		this(new Parameter(), value);
		Parameter param = getDefinition();
		param.setName(parameterName);
		param.setType(type);
		param.configure();
	}

}
