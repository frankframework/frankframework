package nl.nn.adapterframework.frankdoc.model;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public enum ConfigChildGroupKind {
	OBJECT,
	TEXT,
	MIXED;

	/**
	 * Get the {@link ConfigChildGroupKind} of a collection of config children, assuming
	 * that they all have the same role name.
	 */
	public static ConfigChildGroupKind groupKind(Collection<ConfigChild> configChildren) {
		Set<String> roleNames = configChildren.stream().map(ConfigChild::getRoleName).collect(Collectors.toSet());
		if(roleNames.size() >= 2) {
			throw new IllegalArgumentException(String.format("Expected config children [%s] to have the same role name, but got role names [%s]",
					configChildren.stream().map(ConfigChild::toString).collect(Collectors.joining(", ")),
					roleNames.stream().collect(Collectors.joining(", "))));
		}
		boolean allObject = configChildren.stream().allMatch(c -> c instanceof ObjectConfigChild);
		// We also apply this method on config child collections where the config children
		// can have different owning elements. This applies when the generic element option
		// is being analysed. Therefore we allow multiple config children that are all
		// instance of TextConfigChild.
		boolean allSingleText = configChildren.stream().allMatch(c -> c instanceof TextConfigChild);
		if(allObject) {
			return ConfigChildGroupKind.OBJECT;
		} else if(allSingleText) {
			return ConfigChildGroupKind.TEXT;
		} else {
			return ConfigChildGroupKind.MIXED;
		}
	}
}
