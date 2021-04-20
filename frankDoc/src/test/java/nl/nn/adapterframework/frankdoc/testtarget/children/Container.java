package nl.nn.adapterframework.frankdoc.testtarget.children;

import nl.nn.adapterframework.doc.IbisDoc;

public class Container extends ContainerParent {
	@IbisDoc("100")
	public void setChild(Child child) {
	}

	@Deprecated
	@IbisDoc("200")
	public void setDeprecatedChild(Child child) {
	}

	@Override
	public void registerInheritedChilds(InheritedChild inheritedChild) {
		super.registerInheritedChilds(inheritedChild);
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

	@Override
	public void setInheritedChildNonSelected(InheritedChildNonSelected child) {
	}

	@Override
	public void setChildOverriddenOnlyParentAnnotated(ChildOverriddenOnlyParentAnnotated child) {
	}
}
