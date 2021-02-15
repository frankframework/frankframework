package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

class ElementRoleSet {
	private final Set<ElementRole> roles;
	private Set<FrankElement> conflicts;

	ElementRoleSet(Set<ElementRole> roles) {
		this.roles = roles;
		roles.forEach(role -> role.addParticipatingRoleSet(this));
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

	Optional<FrankElement> getGenericElementDefaultCandidates() {
		List<FrankElement> candidates = roles.stream()
				.map(ElementRole::getDefaultElementOptionConflict)
				.filter(Objects::nonNull)
				.collect(Collectors.toList());
		if(candidates.size() == 1) {
			return Optional.of(candidates.get(0));
		} else {
			return Optional.empty();
		}
	}

	@Override
	public String toString() {
		return ElementRole.describeCollection(roles);
	}
}
