package nl.nn.adapterframework.parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class SimpleParameter extends Parameter {

	public SimpleParameter(String value) throws ConfigurationException {
		this(value, null);
	}

	public SimpleParameter(String value, ParameterType type) {
		super();
		setValue(value);
		if(type != null) {
			setType(type);
		}
	}

	public static ParameterValueList getPVL(ParameterList params) throws ConfigurationException, ParameterException {
		params.configure();

		return params.getValues(Message.nullMessage(), new PipeLineSession());
	}

}
