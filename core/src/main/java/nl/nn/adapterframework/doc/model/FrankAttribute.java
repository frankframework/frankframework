package nl.nn.adapterframework.doc.model;

import lombok.Getter;

public class FrankAttribute {
	private @Getter String name;

	public FrankAttribute(String name) {
		this.name = name;
	}
}
