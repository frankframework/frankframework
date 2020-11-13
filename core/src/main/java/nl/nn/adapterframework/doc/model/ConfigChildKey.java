package nl.nn.adapterframework.doc.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Helper class to check whether a ConfigChild overrides a ConfigChild from an ancestor in
 * the inheritance hierarchy.
 * @author martijn
 *
 */
@EqualsAndHashCode
public final class ConfigChildKey {
	private final @Getter String syntax1Name;
	private final @Getter ElementType elementType;
	private final @Getter boolean mandatory;
	private final @Getter boolean allowMultiple;

	public ConfigChildKey(ConfigChild configChild) {
		syntax1Name = configChild.getSyntax1Name();
		elementType = configChild.getElementType();
		mandatory = configChild.isMandatory();
		allowMultiple = configChild.isAllowMultiple();
	}
}
