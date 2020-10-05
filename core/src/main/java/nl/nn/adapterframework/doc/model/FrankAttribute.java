package nl.nn.adapterframework.doc.model;

import lombok.Getter;
import lombok.Setter;

public class FrankAttribute {
	private @Getter String name;
	
	/**
	 * Different FrankAttributes of the same FrankElement are allowed to have the same order.
	 */
	private @Getter @Setter int order;
	
	private @Getter @Setter FrankElement describingElement;
	private @Getter @Setter String description;
	private @Getter @Setter String defaultValue;
	private @Getter @Setter boolean isDeprecated;

	public FrankAttribute(String name) {
		this.name = name;
	}
}
