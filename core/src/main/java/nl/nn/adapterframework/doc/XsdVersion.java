package nl.nn.adapterframework.doc;

import java.util.function.Predicate;

import lombok.Getter;
import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.FrankElement;

public enum XsdVersion {
	STRICT(ElementChild.IN_XSD, ElementChild.DEPRECATED, f -> ! f.isDeprecated()),
	COMPATIBILITY(ElementChild.IN_COMPATIBILITY_XSD, ElementChild.NONE, f -> true);

	private final @Getter Predicate<ElementChild> childSelector;
	private final @Getter Predicate<ElementChild> childRejector;
	private final @Getter Predicate<FrankElement> elementFilter;

	private XsdVersion(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Predicate<FrankElement> elementFilter) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
		this.elementFilter = elementFilter;
	}
}
