package nl.nn.adapterframework.frankdoc.testtarget.children;

import nl.nn.adapterframework.doc.IbisDoc;

public class ContainerParent {
	@IbisDoc("50")
	protected void registerInheritedChilds(InheritedChild inheritedChild) {
	}

	protected void setInheritedChildDocOnDerived(InheritedChildDocOnDerived child) {
	}

	public void setInheritedWithoutOverride(InheritedWithoutOverride child) {
	}

	@IbisDoc("Description of ContainerParent.setInheritedChildDocWithDescriptionOverride")
	public void setInheritedChildDocWithDescriptionOverride(InheritedChildDocWithDescriptionOverride child) {
	}

	@IbisDoc("120")
	public void setInheritedChildNonSelected(InheritedChildNonSelected child) {
	}

	@Deprecated
	@IbisDoc("Description of ContainerParent.setChildOverriddenOnlyParentAnnotated")
	public void setChildOverriddenOnlyParentAnnotated(ChildOverriddenOnlyParentAnnotated child) {
	}
}
