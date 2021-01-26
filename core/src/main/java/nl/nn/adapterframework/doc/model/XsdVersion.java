package nl.nn.adapterframework.doc.model;

import java.util.function.Predicate;

import lombok.Getter;

public enum XsdVersion {
	STRICT(ElementChild.IN_XSD, ElementChild.DEPRECATED, f -> ! f.isDeprecated()),
	COMPATIBILITY(ElementChild.IN_COMPATIBILITY_XSD, ElementChild.NONE, f -> ! f.isCausesNameConflict());

	private final @Getter Predicate<ElementChild> childSelector;
	private final @Getter Predicate<ElementChild> childRejector;
	private final @Getter Predicate<FrankElement> elementFilter;

	private XsdVersion(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Predicate<FrankElement> elementFilter) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
		this.elementFilter = elementFilter;
	}
}
