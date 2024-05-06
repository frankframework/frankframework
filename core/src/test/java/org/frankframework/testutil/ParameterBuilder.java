package org.frankframework.testutil;

import org.frankframework.configuration.ConfigurationException;
import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.parameters.Parameter;
import org.frankframework.parameters.ParameterList;
import org.frankframework.parameters.ParameterType;
import org.frankframework.parameters.ParameterValueList;
import org.frankframework.stream.Message;

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
