package nl.nn.adapterframework.parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.parameters.Parameter.ParameterType;

public class SimpleParameter extends ParameterValue {

	private SimpleParameter(Parameter type, Object value) {
		super(type, value);
	}

	public SimpleParameter(String parameterName, ParameterType type, Object value) throws ConfigurationException {
		this(new Parameter(), value);
		Parameter param = getDefinition();
		param.setName(parameterName);
		param.setType(type);
		param.configure();
	}

}
