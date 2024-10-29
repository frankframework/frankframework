package org.frankframework.testutil;

import org.frankframework.parameters.ParameterType;
import org.frankframework.parameters.XmlParameter;

public class XmlParameterBuilder extends XmlParameter {
	public XmlParameterBuilder withName(String name) {
		setName(name);
		return this;
	}

	public XmlParameterBuilder withValue(String value) {
		setValue(value);
		return this;
	}

	public static XmlParameterBuilder create() {
		return new XmlParameterBuilder();
	}

	public static XmlParameterBuilder create(String name, String value) {
		XmlParameterBuilder xmlParameterBuilder = new XmlParameterBuilder();
		xmlParameterBuilder.withName(name);
		xmlParameterBuilder.withValue(value);

		return xmlParameterBuilder;
	}

	public XmlParameterBuilder withType(ParameterType parameterType) {
		setType(parameterType);

		return this;
	}
}
