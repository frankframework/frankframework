package nl.nn.adapterframework.frankdoc.testtarget.walking;

import nl.nn.adapterframework.doc.NoFrankAttribute;

public class NotRealChildNotExcludingInterface extends NotRealParent {
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
