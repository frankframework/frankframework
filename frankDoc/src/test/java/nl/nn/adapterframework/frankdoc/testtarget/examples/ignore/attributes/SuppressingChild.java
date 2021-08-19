package nl.nn.adapterframework.frankdoc.testtarget.examples.ignore.attributes;

import nl.nn.adapterframework.doc.FrankDocIgnoreTypeMembership;
import nl.nn.adapterframework.doc.NoFrankAttribute;

@FrankDocIgnoreTypeMembership("nl.nn.adapterframework.frankdoc.testtarget.examples.ignore.attributes.IChild1")
public class SuppressingChild extends Parent implements IChild2 {
	public void setChildAttribute(String value) {
	}

	@NoFrankAttribute
	public void setNotChildAttribute(String value) {
	}
}
