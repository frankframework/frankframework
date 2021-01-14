package nl.nn.adapterframework.doc.testtarget.compatibility;

public class Container {
	public void setChildOk(IChildOk child) {
	}

	public void setChildMismatch(IChildMismatch child) {
	}

	public void setChild1(NonInterfaceChildForOwningRole child) {
	}

	@Deprecated
	public void setChild2(NonInterfaceChildNoOwningRoleBecauseDeprecated child) {
	}
}
