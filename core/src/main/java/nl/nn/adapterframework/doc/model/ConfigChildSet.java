package nl.nn.adapterframework.doc.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
	private final List<ConfigChild> configChildren;

	/**
	 * @throws IllegalArgumentException if input children are not in the right sort order
	 * or do not share a role name. The children should be sorted from derived to ancestor
	 * owning element and then by order. This precondition should be enforced
	 * by {@link AncestorChildNavigation}.
	 */
	ConfigChildSet(List<ConfigChild> configChildren) {
		this.configChildren = configChildren;
		if(configChildren.size() >= 2) {
			FrankElement parent = configChildren.get(0).getOwningElement();
			int order = configChildren.get(0).getSequenceInConfig();
			for(ConfigChild c: configChildren.subList(1, configChildren.size())) {
				if(c.getOwningElement() != parent) {
					if(c.getOwningElement() != parent.getNextAncestorThatHasConfigChildren(ElementChild.ALL)) {
						throw new IllegalArgumentException(String.format("Cumulative config children are not sorted: [%s] should not be followed by [%s]",
								parent.getFullName(), c.getOwningElement().getFullName()));
					}
					parent = c.getOwningElement();
				}
				else {
					if(! (order <= c.getSequenceInConfig())) {
						throw new IllegalArgumentException(String.format("Cumulative config children are not sorted by order. Offending config child [%s]",
								c.getKey().toString()));
					}
					order = c.getSequenceInConfig();
				}
			}
		}
	}

	public List<ElementRole> getMemberChildren(
			Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Predicate<FrankElement> elementFilter, List<String> nestedRoleNames) {
		List<ElementRole> parents = filter(selector, rejector).stream().map(ConfigChild::getElementRole).distinct().collect(Collectors.toList());
		return getMemberChildren(selector, rejector, elementFilter, nestedRoleNames, parents);
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

	private List<ElementRole> getMemberChildren(Predicate<ElementChild> selector, Predicate<ElementChild> rejector, Predicate<FrankElement> elementFilter, List<String> nestedRoleNames,
			List<ElementRole> parents) {
		List<FrankElement> members = parents.stream()
				.flatMap(role -> role.getMembers().stream())
				.filter(elementFilter)
				.distinct()
				.collect(Collectors.toList());
		List<ConfigChild> configChildren = members.stream().flatMap(element -> element.getCumulativeConfigChildren(selector, rejector).stream())
				.distinct()
				.collect(Collectors.toList());
		if(nestedRoleNames.isEmpty()) {
				return configChildren.stream()
						.map(ConfigChild::getElementRole)
						.distinct()
						.collect(Collectors.toList());
		} else {
			final String roleName = nestedRoleNames.get(0);
			List<String> remainingRoleNames = new ArrayList<>(nestedRoleNames);
			remainingRoleNames.remove(0);
			List<ElementRole> newParents = configChildren.stream()
					.filter(c -> c.getSyntax1Name().equals(roleName))
					.map(ConfigChild::getElementRole)
					.distinct()
					.collect(Collectors.toList());
			return getMemberChildren(selector, rejector, elementFilter, remainingRoleNames, newParents);
		}
	}
}
