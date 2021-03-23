package nl.nn.adapterframework.frankdoc.testtarget.walking;

import nl.nn.adapterframework.doc.IbisDoc;

public class GrandChild extends Child {
	@IbisDoc("Some description")
	@Override
	public void setParentAttributeSecond(String value) {
	}

	public void setGrandChildAttribute(String value) {
	}
}
