package nl.nn.adapterframework.doc.model;

import lombok.Getter;
import lombok.Setter;

public class FrankElement {
	private final @Getter String fullName;
	private final @Getter String simpleName;
	private @Getter @Setter FrankElement parent;

	public FrankElement(final String fullName, final String simpleName) {
		this.fullName = fullName;
		this.simpleName = simpleName;
	}
}
