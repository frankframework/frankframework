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

	@IbisDoc("Description of Container.setInheritedChildDocWithDescriptionOverride")
	@Override
	public void setInheritedChildDocWithDescriptionOverride(InheritedChildDocWithDescriptionOverride child) {
	}

	@Override
	public void setInheritedChildNonSelected(InheritedChildNonSelected child) {
	}

	@Override
	public void setChildOverriddenOnlyParentAnnotated(ChildOverriddenOnlyParentAnnotated child) {
	}

	// To test TextConfigChild
	public void registerText(String value) {
	}

	// Not a config child because the name starts with "set" and the argument is String.
	public void setNotConfigChildButAttribute(String value) {
	}
}
