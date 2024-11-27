package org.frankframework.testutil;

import org.frankframework.parameters.BooleanParameter;

public class BooleanParameterBuilder extends BooleanParameter {

	public BooleanParameterBuilder withName(String name) {
		setName(name);
		return this;
	}

	public BooleanParameterBuilder withValue(boolean value) {
		setValue("" + value);
		return this;
	}

	public BooleanParameterBuilder withSessionKey(String sessionKey) {
		setSessionKey(sessionKey);
		return this;
	}

	public static BooleanParameterBuilder create(String name) {
		BooleanParameterBuilder numberParam = new BooleanParameterBuilder();
		numberParam.setName(name);
		return numberParam;
	}

	public static BooleanParameterBuilder create(String name, boolean value) {
		BooleanParameterBuilder numberParam = new BooleanParameterBuilder();
		numberParam.setName(name);
		numberParam.setValue(""+value);
		return numberParam;
	}
}
