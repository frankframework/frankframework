package org.frankframework.testutil;

import org.frankframework.parameters.NumberParameter;

public class NumberParameterBuilder extends NumberParameter {

	public NumberParameterBuilder withName(String name) {
		setName(name);
		return this;
	}

	public NumberParameterBuilder withValue(Integer value) {
		setValue("" + value);
		return this;
	}

	public NumberParameterBuilder withSessionKey(String sessionKey) {
		setSessionKey(sessionKey);
		return this;
	}

	public static NumberParameterBuilder create() {
		return new NumberParameterBuilder();
	}

	public static NumberParameterBuilder create(String name) {
		NumberParameterBuilder numberParam = new NumberParameterBuilder();
		numberParam.setName(name);
		return numberParam;
	}

	public static NumberParameterBuilder create(String name, Integer value) {
		NumberParameterBuilder numberParam = new NumberParameterBuilder();
		numberParam.setName(name);
		numberParam.setValue(""+value);
		return numberParam;
	}

	public static NumberParameterBuilder create(String name, Long value) {
		NumberParameterBuilder numberParam = new NumberParameterBuilder();
		numberParam.setName(name);
		numberParam.setValue(""+value);
		return numberParam;
	}
}
