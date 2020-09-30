package nl.nn.adapterframework.doc.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class FrankElement {
	private final @Getter String fullName;
	private final @Getter String simpleName;
	private @Getter @Setter FrankElement parent;
	private @Getter @Setter List<FrankAttribute> attributes;

	public FrankElement(final String fullName, final String simpleName) {
		this.fullName = fullName;
		this.simpleName = simpleName;
	}
}
