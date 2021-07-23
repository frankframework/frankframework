package nl.nn.adapterframework.frankdoc.model;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.frankdoc.doclet.FrankMethod;

public class ObjectConfigChild extends ConfigChild {
	private @Getter @Setter ElementRole elementRole;

	ObjectConfigChild(FrankElement owningElement, FrankMethod method) {
		super(owningElement, method);
	}

	@Override
	public ConfigChildKey getKey() {
		return new ConfigChildKey(getRoleName(), elementRole.getElementType());
	}

	@Override
	public String getRoleName() {
		return elementRole.getRoleName();
	}

	public ElementType getElementType() {
		return elementRole.getElementType();
	}

	@Override
	public String toString() {
		return String.format("%s(%s.%s(%s))",
				this.getClass().getSimpleName(), getOwningElement().getSimpleName(), getMethodName(), getElementType().getSimpleName());
	}
}
