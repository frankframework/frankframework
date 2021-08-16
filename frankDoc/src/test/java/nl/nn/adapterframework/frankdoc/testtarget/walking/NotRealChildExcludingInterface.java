package nl.nn.adapterframework.frankdoc.testtarget.walking;

import nl.nn.adapterframework.doc.FrankDocIgnoreTypeMembership;
import nl.nn.adapterframework.doc.NoFrankAttribute;

@FrankDocIgnoreTypeMembership("nl.nn.adapterframework.frankdoc.testtarget.walking.INotRealExcluded")
public class NotRealChildExcludingInterface extends NotRealParent {
	/** This attribute is documented */
	@Override
	public void setExcludedAttribute2(String value) {
	}

	public void setChildAttribute(String value) {
	}

	@NoFrankAttribute
	public void setNotChildAttribute(String value) {
	}
}
