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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Holds the list of all cumulative config children sharing some role name, say R, but
 * only if there is no ancestor having the same config children for role name R.
 * <p>
 * Example. Assume that a class Super has a derived class Derived. Super has a method
 * registerChild(A a) while Derived has a method registerChild(B b). Then the cumulative
 * config children of Derived with role name "child" are (A, child), (B, child). These
 * two go into a ConfigChildSet with common role name "child".
 * <p>
 * If the method registerChild(B b) would not be in Derived but in Super, then the cumulative
 * config children with role name "child" would be the same for Derived and Super. Therefore
 * only Super would have a ConfigChildSet for this role name, not Derived.
 * @author martijn
 *
 */
public class ConfigChildSet {
	private static Logger log = LogUtil.getLogger(ConfigChildSet.class);

	private final @Getter List<ConfigChild> configChildren;

	/**
	 * @throws IllegalStateException if input children is empty or when the elements
	 * are not in the right sort order
	 * or do not share a role name. The children should be sorted from derived to ancestor
	 * owning element and then by order. This precondition should be enforced
	 * by {@link AncestorChildNavigation}.
	 */
	ConfigChildSet(List<ConfigChild> configChildren) {
		this.configChildren = configChildren;
		if(configChildren.isEmpty()) {
			throw new IllegalStateException("A config child cannot have an empty list of config childs");
		}
		if(configChildren.size() >= 2) {
			FrankElement parent = configChildren.get(0).getOwningElement();
			int order = configChildren.get(0).getOrder();
			for(ConfigChild c: configChildren.subList(1, configChildren.size())) {
				if(c.getOwningElement() != parent) {
					if(c.getOwningElement() != parent.getNextAncestorThatHasConfigChildren(ElementChild.ALL_NOT_EXCLUDED)) {
						throw new IllegalStateException(String.format("Cumulative config children are not sorted: [%s] should not be followed by [%s]",
								parent.getFullName(), c.getOwningElement().getFullName()));
					}
					parent = c.getOwningElement();
				}
				else {
					if(! (order <= c.getOrder())) {
						throw new IllegalStateException(String.format("Cumulative config children are not sorted by order. Offending config child [%s]",
								c.getKey().toString()));
					}
					order = c.getOrder();
				}
			}
		}
	}

	public ConfigChildGroupKind getConfigChildGroupKind() {
		return ConfigChildGroupKind.groupKind(configChildren);
	}

	public Stream<ElementRole> getElementRoleStream() {
		return ConfigChild.getElementRoleStream(configChildren);
	}

	public String getRoleName() {
		return configChildren.get(0).getRoleName();
	}

	public List<ElementRole> getFilteredElementRoles(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		List<ConfigChild> filteredConfigChildren = filter(selector, rejector);
		return ConfigChild.getElementRoleStream(filteredConfigChildren).collect(Collectors.toList());
	}

	List<ConfigChild> filter(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		Set<ConfigChildKey> keys = configChildren.stream().map(ConfigChild::getKey).distinct().collect(Collectors.toSet());
		List<ConfigChild> result = new ArrayList<>();
		for(ConfigChild c: configChildren) {
			if(rejector.test(c)) {
				keys.remove(c.getKey());
			} else if(selector.test(c)) {
				result.add(c);
				keys.remove(c.getKey());
			}
		}
		return result;
	}

	/**
	 * Handles generic element option recursion as explained in
	 * {@link nl.nn.adapterframework.frankdoc.model}.
	 */
	public static Map<String, List<ConfigChild>> getMemberChildren(
			List<ElementRole> parents, Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Predicate<FrankElement> elementFilter) {
		if(log.isTraceEnabled()) {
			log.trace("ConfigChildSet.getMemberChildren called with parents: [{}]", elementRolesToString(parents));
		}
		List<FrankElement> members = parents.stream()
				.flatMap(role -> role.getMembers().stream())
				.filter(elementFilter)
				.distinct()
				.collect(Collectors.toList());
		if(log.isTraceEnabled()) {
			String elementsString = members.stream().map(FrankElement::getSimpleName).collect(Collectors.joining(", "));
			log.trace("Members of parents are: [{}]", elementsString);
		}
		Map<String, List<ConfigChild>> memberChildrenByRoleName = members.stream().flatMap(element -> element.getCumulativeConfigChildren(selector, rejector).stream())
				.distinct()
				.collect(Collectors.groupingBy(ConfigChild::getRoleName));
		if(log.isTraceEnabled()) {
			log.trace("Found the following member children:");
			for(String roleName: memberChildrenByRoleName.keySet()) {
				List<ConfigChild> memberChildren = memberChildrenByRoleName.get(roleName);
				String memberChildrenString = memberChildren.stream()
						.map(ConfigChild::toString)
						.collect(Collectors.joining(", "));
				log.trace("  [{}]: [{}]", roleName, memberChildrenString);				
			}
		}
		return memberChildrenByRoleName;
	}

	private static String elementRolesToString(List<ElementRole> elementRoles) {
		return elementRoles.stream().map(ElementRole::toString).collect(Collectors.joining(", "));
	}

	public static Set<ElementRole.Key> getKey(List<ElementRole> roles) {
		return roles.stream().map(ElementRole::getKey).collect(Collectors.toSet());
	}

	public Optional<FrankElement> getGenericElementOptionDefault(Predicate<FrankElement> elementFilter) {
		List<FrankElement> candidates = ConfigChild.getElementRoleStream(configChildren)
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
		return "ConfigChildSet(" +
				configChildren.stream().map(ConfigChild::toString).collect(Collectors.joining(", "))
				+ ")";
	}
}
