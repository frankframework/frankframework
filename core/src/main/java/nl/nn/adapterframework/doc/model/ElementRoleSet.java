package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class ElementRoleSet {
	private final Set<ElementRole> roles;
	private Set<FrankElement> conflicts;

	ElementRoleSet(Set<ElementRole> roles) {
		this.roles = roles;
	}

	void initConflicts() {
		Map<String, Set<FrankElement>> byXsdElementName = new HashMap<>();
		for(ElementRole role: roles) {
			Map<String, List<FrankElement>> roleMembersByName = role.getRawMembers().stream()
					.collect(Collectors.groupingBy(elem -> elem.getXsdElementName(role)));
			for(String name: roleMembersByName.keySet()) {
				byXsdElementName.merge(name, new HashSet<>(roleMembersByName.get(name)), FrankElement::join);
			}
		}
		Set<String> nameConflicts = byXsdElementName.keySet().stream()
				.filter(name -> byXsdElementName.get(name).size() >= 2)
				.collect(Collectors.toSet());
		conflicts = nameConflicts.stream()
				.flatMap(name -> byXsdElementName.get(name).stream())
				.collect(Collectors.toSet());
	}

	boolean isConflict(FrankElement frankElement) {
		return conflicts.contains(frankElement);
	}
}
