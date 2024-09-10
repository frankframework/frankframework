package org.frankframework.testutil;

import org.frankframework.parameters.DateParameter;

public class DateParameterBuilder extends DateParameter {

	public DateParameterBuilder withName(String name) {
		setName(name);
		return this;
	}

	public DateParameterBuilder withValue(String value) {
		setValue("" + value);
		return this;
	}

	public DateParameterBuilder withSessionKey(String sessionKey) {
		setSessionKey(sessionKey);
		return this;
	}

	public DateParameterBuilder withFormatType(DateFormatType formatType) {
		setFormatType(formatType);
		return this;
	}

	public static DateParameterBuilder create() {
		return new DateParameterBuilder();
	}

	public static DateParameterBuilder create(String name) {
		DateParameterBuilder numberParam = new DateParameterBuilder();
		numberParam.setName(name);
		return numberParam;
	}

	public static DateParameterBuilder create(String name, String value) {
		DateParameterBuilder numberParam = new DateParameterBuilder();
		numberParam.setName(name);
		numberParam.setValue(value);
		return numberParam;
	}
}
