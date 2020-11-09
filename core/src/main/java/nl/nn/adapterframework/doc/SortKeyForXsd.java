package nl.nn.adapterframework.doc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.nn.adapterframework.doc.model.ElementType;
import nl.nn.adapterframework.doc.model.FrankElement;

@EqualsAndHashCode
final class SortKeyForXsd {
	enum Kind {
		ELEMENT,
		TYPE;
	}

	private @Getter final Kind kind;
	private @Getter final String name;

	static SortKeyForXsd getInstance(ElementType type) {
		return new SortKeyForXsd(Kind.TYPE, type.getFullName());
	}

	static SortKeyForXsd getInstance(FrankElement element) {
		return new SortKeyForXsd(Kind.ELEMENT, element.getFullName());
	}

	private SortKeyForXsd(final Kind kind, final String name) {
		this.kind = kind;
		this.name = name;
	}
}
