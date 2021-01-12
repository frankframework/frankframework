package nl.nn.adapterframework.doc.testtarget.reflect;

import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.doc.IbisDocRef;

public class FrankAttributeTargetParent {
	@IbisDoc({"1000", "This one should not count as documenting the override in FrankAttributeTarget"})
	public void setAttributeOnlySetter(String value) {
	}

	// Annotation is to test that FrankAttributeTarget is not influenced
	// by this annotation. An @IbisDoc or @IbisDorRef annotation on an
	// overridden method does not count as documenting the overriding method.
	@IbisDocRef("1000")
	public void setAttributeOnlySetterInt(int value) {
	}
}
