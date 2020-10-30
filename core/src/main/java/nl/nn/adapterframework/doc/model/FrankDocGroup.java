package nl.nn.adapterframework.doc.model;

import lombok.Getter;

public class FrankDocGroup {
	private @Getter String name;
	private @Getter ElementType elementType;

	public FrankDocGroup(String name, ElementType elementType) {
		this.name = name;
		this.elementType = elementType;
	}
}
