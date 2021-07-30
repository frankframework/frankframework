/* 
Copyright 2021 WeAreFrank! 

Licensed under the Apache License, Version 2.0 (the "License"); 
you may not use this file except in compliance with the License. 
You may obtain a copy of the License at 

    http://www.apache.org/licenses/LICENSE-2.0 

Unless required by applicable law or agreed to in writing, software 
distributed under the License is distributed on an "AS IS" BASIS, 
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
See the License for the specific language governing permissions and 
limitations under the License. 
*/
package nl.nn.adapterframework.frankdoc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves member conflicts of shared generic element options. Also used with
 * recursion within generic element options. See {@link nl.nn.adapterframework.frankdoc.model}.
 * @author martijn
 *
 */
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

	boolean conflictsWithGenericElementOptionElementName(FrankElement frankElement) {
		return conflicts.contains(frankElement);
	}

	@Override
	public String toString() {
		return ElementRole.describeCollection(roles);
	}
}
