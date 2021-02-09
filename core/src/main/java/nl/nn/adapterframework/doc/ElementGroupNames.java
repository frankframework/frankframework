package nl.nn.adapterframework.doc;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.nn.adapterframework.doc.model.ConfigChildSet;
import nl.nn.adapterframework.doc.model.ElementChild;
import nl.nn.adapterframework.doc.model.ElementRole;

class ElementGroupNames {
	private static final String ELEMENT_GROUP = "ElementGroup";

	private final Map<Set<ElementRole.Key>, Integer> genericGroupKeyToSeq = new HashMap<>();
	private final Predicate<ElementChild> childSelector;
	private final Predicate<ElementChild> childRejector;

	ElementGroupNames(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
	}

	boolean isGroupExists(Set<ElementRole.Key> key) {
		return genericGroupKeyToSeq.containsKey(key);
	}

	private static Set<ElementRole.Key> keyOf(Set<ElementRole> roles) {
		Set<ElementRole.Key> key = roles.stream().map(ElementRole::getKey).collect(Collectors.toSet());
		return key;
	}

	String addGroup(ConfigChildSet configChildSet) {
		Set<ElementRole.Key> key = configChildSet.getKey(childSelector, childRejector);
		return addGroup(key);
	}

	String addGroup(Set<ElementRole.Key> key) {
		String syntax1Name = getSyntax1Name(key);
		return addGroup(key, syntax1Name);
	}

	private String addGroup(Set<ElementRole.Key> key, String syntax1Name) {
		List<Set<ElementRole.Key>> shared = genericGroupKeyToSeq.keySet().stream()
				.filter(rs -> getSyntax1Name(rs).equals(syntax1Name))
				.collect(Collectors.toList());
		int seq = shared.stream().map(genericGroupKeyToSeq::get).collect(Collectors.maxBy(Integer::compare)).orElse(0) + 1;
		genericGroupKeyToSeq.put(key, seq);
		return getGroupName(key, syntax1Name);
	}

	private String getSyntax1Name(Set<ElementRole.Key> key) {
		return key.iterator().next().getSyntax1Name();
	}

	String getGroupName(ConfigChildSet configChildSet) {
		Set<ElementRole.Key> key = configChildSet.getKey(childSelector, childRejector);
		return getGroupName(key);
	}

	String getGroupName(Set<ElementRole.Key> key) {
		String syntax1Name = getSyntax1Name(key);
		return getGroupName(key, syntax1Name);
	}

	String getGroupName(List<ElementRole> roles) {
		Set<ElementRole.Key> key = keyOf(new HashSet<>(roles));
		String syntax1Name = roles.iterator().next().getSyntax1Name();
		return getGroupName(key, syntax1Name);
	}

	private String getGroupName(Set<ElementRole.Key> key, String syntax1Name) {
		return Utils.toUpperCamelCase(syntax1Name) + ELEMENT_GROUP + disambiguation(genericGroupKeyToSeq.get(key));
	}

	static String disambiguation(int seq) {
		String result = "";
		if(seq != 1) {
			result = String.format("_%d", seq);
		}
		return result;
	}
}
