package nl.nn.adapterframework.testutil;

import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.parameters.Parameter;
import nl.nn.adapterframework.parameters.ParameterList;
import nl.nn.adapterframework.parameters.ParameterValueList;
import nl.nn.adapterframework.stream.Message;

public class ParameterBuilder extends Parameter {

	private ParameterBuilder() {
		super();
	}

	private ParameterBuilder(String name, String value) {
		super(name, value);
	}

	public ParameterBuilder withName(String name) {
		setName(name);
		return this;
	}

	public ParameterBuilder withValue(String value) {
		setValue(value);
		return this;
	}

	public ParameterBuilder withSessionKey(String sessionKey) {
		setSessionKey(sessionKey);
		return this;
	}

	public ParameterBuilder withType(ParameterType type) {
		setType(type);
		return this;
	}

	public static ParameterBuilder create() {
		return new ParameterBuilder();
	}

	public static ParameterBuilder create(String name, String value) {
		return new ParameterBuilder(name, value);
	}

	public static ParameterValueList getPVL(ParameterList params) throws ConfigurationException, ParameterException {
		params.configure();

		return params.getValues(Message.nullMessage(), new PipeLineSession());
	}
}
