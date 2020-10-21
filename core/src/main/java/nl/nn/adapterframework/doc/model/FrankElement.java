package nl.nn.adapterframework.doc.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class FrankElement {
	private final @Getter String fullName;
	private final @Getter String simpleName;
	private @Getter FrankElement parent;
	private @Getter @Setter List<FrankAttribute> attributes;
	private @Getter @Setter List<ConfigChild> configChildren;

	FrankElement(Class<?> clazz, FrankElement parent) {
		this(clazz.getName(), clazz.getSimpleName());
		this.parent = parent;
	}

	/**
	 * Constructor for testing purposes. We want to test attribute construction in isolation,
	 * in which case we do not have a parent.
	 */
	FrankElement(final String fullName, final String simpleName) {
		this.fullName = fullName;
		this.simpleName = simpleName;
	}
}
