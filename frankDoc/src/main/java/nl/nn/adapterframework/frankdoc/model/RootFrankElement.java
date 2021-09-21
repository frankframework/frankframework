package nl.nn.adapterframework.frankdoc.model;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

/**
 * A FrankElement that can appear as the root element in a Frank configuration. Such elements are not
 * part of a config child, but they have a role name that matches rules in digester-rules.xml.
 */
class RootFrankElement extends FrankElement implements DigesterRulesRootFrankElement {
	RootFrankElement(FrankClass clazz) {
		super(clazz);
	}

	@Override
	public String getRoleName() {
		return getSimpleName().toLowerCase();
	}
}
