package nl.nn.adapterframework.frankdoc.model;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

class RootFrankElement extends FrankElement implements DigesterRulesRootFrankElement {
	RootFrankElement(FrankClass clazz) {
		super(clazz);
	}

	@Override
	public String getRoleName() {
		return getSimpleName().toLowerCase();
	}
}
