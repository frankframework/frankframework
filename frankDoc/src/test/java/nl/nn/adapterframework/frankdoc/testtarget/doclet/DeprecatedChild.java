package nl.nn.adapterframework.frankdoc.testtarget.doclet;

import nl.nn.adapterframework.doc.IbisDoc;

@Deprecated
public class DeprecatedChild extends Parent {
	@Deprecated
	@IbisDoc({"100", "Some description", "0"})
	public void someSetter(int value) {
	}

	void packagePrivateMethod() {
	}
}
