package nl.nn.adapterframework.doc.model;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class AttributeReferenceGroup {
	private @Getter @Setter FrankElement owningElement;
	private @Getter @Setter FrankElement describingElement;
	private @Getter @Setter List<FrankAttribute> attributes;
}
