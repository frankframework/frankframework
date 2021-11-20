package nl.nn.adapterframework.parameters;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

public class SimpleParameter extends Parameter {

	public SimpleParameter(String value) {
		this(null, value, null);
	}

	public SimpleParameter(String name, String value) {
		this(name, value, null);
	}

	public SimpleParameter(String value, ParameterType type) {
		this(null, value, type);
	}
	public SimpleParameter(String name, String value, ParameterType type) {
		super();
		setName(name);
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
