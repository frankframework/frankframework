package nl.nn.adapterframework.frankdoc.model;

import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

public class TextConfigChild extends ConfigChild {
	private final String roleName;

	TextConfigChild(FrankElement owningElement, FrankMethod method, String roleName) {
		super(owningElement, method);
		this.roleName = roleName;
	}

	// Avoid complicated Lombok syntax to add the @Override tag. Just
	// coding the getter is simpler in this case.
	@Override
	public String getRoleName() {
		return roleName;
	}

	@Override
	public String toString() {
		return String.format("%s(roleName = %s)", getClass().getSimpleName(), roleName);
	}
}
