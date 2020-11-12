package nl.nn.adapterframework.doc.testtarget.children;

import nl.nn.adapterframework.doc.IbisDoc;

public class ContainerParent {
	@IbisDoc("50")
	protected void registerInheritedChild(InheritedChild inheritedChild) {
	}

	protected void setInheritedChildDocOnDerived(InheritedChildDocOnDerived child) {
	}

	public void setInheritedWithoutOverride(InheritedWithoutOverride child) {
	}

	@IbisDoc("20")
	public void setInheritedChildDocWithOrderOverride(InheritedChildDocWithOrderOverride child) {
	}
}
