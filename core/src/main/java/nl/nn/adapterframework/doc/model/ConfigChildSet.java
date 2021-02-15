package nl.nn.adapterframework.doc.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;

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
	private final @Getter List<ConfigChild> configChildren;
	private @Getter @Setter ElementRoleSet elementRoleSet;

	/**
	 * @throws IllegalArgumentException if input children is empty or when the elements
	 * are not in the right sort order
	 * or do not share a role name. The children should be sorted from derived to ancestor
	 * owning element and then by order. This precondition should be enforced
	 * by {@link AncestorChildNavigation}.
	 */
	ConfigChildSet(List<ConfigChild> configChildren) {
		this.configChildren = configChildren;
		if(configChildren.isEmpty()) {
			throw new IllegalArgumentException("A config child cannot have an empty list of config childs");
		}
		if(configChildren.size() >= 2) {
			FrankElement parent = configChildren.get(0).getOwningElement();
			int order = configChildren.get(0).getOrder();
			for(ConfigChild c: configChildren.subList(1, configChildren.size())) {
				if(c.getOwningElement() != parent) {
					if(c.getOwningElement() != parent.getNextAncestorThatHasConfigChildren(ElementChild.ALL)) {
						throw new IllegalArgumentException(String.format("Cumulative config children are not sorted: [%s] should not be followed by [%s]",
								parent.getFullName(), c.getOwningElement().getFullName()));
					}
					parent = c.getOwningElement();
				}
				else {
					if(! (order <= c.getOrder())) {
						throw new IllegalArgumentException(String.format("Cumulative config children are not sorted by order. Offending config child [%s]",
								c.getKey().toString()));
					}
					order = c.getOrder();
				}
			}
		}
	}

	public String getSyntax1Name() {
		return configChildren.get(0).getSyntax1Name();
	}

	public List<ElementRole> getFilteredElementRoles(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		return new ArrayList<>(filter(selector, rejector).stream().map(ConfigChild::getElementRole).distinct().collect(Collectors.toList()));
	}

	List<ConfigChild> filter(Predicate<ElementChild> selector, Predicate<ElementChild> rejector) {
		Set<ConfigChild.Key> keys = configChildren.stream().map(ConfigChild.Key::new).distinct().collect(Collectors.toSet());
		List<ConfigChild> result = new ArrayList<>();
		for(ConfigChild c: configChildren) {
			if(rejector.test(c)) {
				keys.remove(new ConfigChild.Key(c));
			} else if(selector.test(c)) {
				result.add(c);
				keys.remove(new ConfigChild.Key(c));
			}
		}
		return result;
	}

	public static Map<String, List<ElementRole>> getMemberChildren(
			List<ElementRole> parents, Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Predicate<FrankElement> elementFilter) {
		List<FrankElement> members = parents.stream()
				.flatMap(role -> role.getMembers().stream())
				.filter(elementFilter)
				.distinct()
				.collect(Collectors.toList());
		List<ConfigChild> memberChildren = members.stream().flatMap(element -> element.getCumulativeConfigChildren(selector, rejector).stream())
				.distinct()
				.collect(Collectors.toList());
		return promoteIfConflict(memberChildren.stream()
				.map(ConfigChild::getElementRole)
				.distinct()
				.collect(Collectors.groupingBy(ElementRole::getSyntax1Name)));
	}

	private static Map<String, List<ElementRole>> promoteIfConflict(Map<String, List<ElementRole>> original) {
		Map<String, List<ElementRole>> result = new HashMap<>();
		for(String syntax1Name: original.keySet()) {
			List<ElementRole> group = original.get(syntax1Name);
			if(group.size() == 1) {
				result.put(syntax1Name, group);
			} else {
				result.put(syntax1Name, group.stream().map(ElementRole::getHighestCommonInterface).distinct().collect(Collectors.toList()));
			}
		}
		return result;
	}

	public static Set<ElementRole.Key> getKey(List<ElementRole> roles) {
		return roles.stream().map(ElementRole::getKey).collect(Collectors.toSet());
	}

	public Optional<FrankElement> getGenericElementOptionDefault(Predicate<FrankElement> elementFilter) {
		return elementRoleSet.getGenericElementDefaultCandidates().filter(elementFilter);
	}

	@Override
	public String toString() {
		return elementRoleSet.toString();
	}
}
