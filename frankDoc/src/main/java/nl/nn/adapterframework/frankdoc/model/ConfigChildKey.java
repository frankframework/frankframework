package nl.nn.adapterframework.frankdoc.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@EqualsAndHashCode(callSuper = false)
class ConfigChildKey extends ElementChild.AbstractKey {
	private final @Getter String roleName;
	private final @Getter ElementType elementType;

	public ConfigChildKey(String roleName, ElementType elementType) {
		this.roleName = roleName;
		this.elementType = elementType;
	}

	@Override
	public String toString() {
		return "(roleName=" + roleName + ", elementType=" + elementType.getFullName() + ")";
	}
}
