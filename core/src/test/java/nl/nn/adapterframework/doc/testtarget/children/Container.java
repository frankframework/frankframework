package nl.nn.adapterframework.doc.testtarget.children;

import nl.nn.adapterframework.doc.IbisDoc;

public class Container extends ContainerParent {
	@IbisDoc("100")
	public void setChild(Child child) {
	}

	@Override
	public void registerInheritedChild(InheritedChild inheritedChild) {
		super.registerInheritedChild(inheritedChild);
	}

	@Override
	@IbisDoc("70")
	public void setInheritedChildDocOnDerived(InheritedChildDocOnDerived child) {
		super.setInheritedChildDocOnDerived(child);
	}

	public void setNoChildNotInDictionary(NoChild noChild) {
	}

	public void setAttribute(String attributeValue) {
	}

	@IbisDoc("10")
	public void setInheritedChildDocWithOrderOverride(InheritedChildDocWithOrderOverride child) {
	}
}
