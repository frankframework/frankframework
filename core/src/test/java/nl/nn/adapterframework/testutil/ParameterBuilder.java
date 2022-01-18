package nl.nn.adapterframework.testutil;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

public class ParameterBuilder extends Parameter {

	public ParameterBuilder() {
		super();
	}

	public ParameterBuilder withValue(String value) {
		setValue(value);
		return this;
	}

	public ParameterBuilder withType(ParameterType type) {
		setType(type);
		return this;
	}

	public static ParameterBuilder create() {
		return new ParameterBuilder();
	}

	public static ParameterValueList getPVL(ParameterList params) throws ConfigurationException, ParameterException {
		params.configure();

		return params.getValues(Message.nullMessage(), new PipeLineSession());
	}
}
