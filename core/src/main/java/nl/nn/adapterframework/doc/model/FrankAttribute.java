package nl.nn.adapterframework.doc.model;

import lombok.Getter;
import lombok.Setter;

public class FrankAttribute {
	private @Getter String name;
	private @Getter @Setter FrankElement describingElement;

	public FrankAttribute(String name) {
		this.name = name;
	}
}
