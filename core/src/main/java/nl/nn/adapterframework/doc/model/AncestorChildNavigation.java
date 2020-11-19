package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

abstract class AncestorChildNavigation<K> {
	private final CumulativeChildHandler handler;
	private FrankElement current;
	private Map<K, Boolean> items;
	private Set<K> overridden;

	AncestorChildNavigation(CumulativeChildHandler handler) {
		this.handler = handler;
	}

	void run(FrankElement start) {
		enter(start);
		handler.handleChildrenOf(start);
		overridden = getCurrentOverrides();
		while(nextAncestor(current) != null) {
			enter(nextAncestor(current));
			if(overridden.isEmpty()) {
				safeAddCumulative();
				return;
			}
			handleOverridesForCurrent();
		}
	}

	private void enter(FrankElement current) {
		this.current = current;
		items = new HashMap<>();
		for(ElementChild c: getChildrenOf(current)) {
			items.put(keyOf(c), c.getOverriddenFrom() != null);
		}
	}

	private void safeAddCumulative() {
		if(nextAncestor(current) == null) {
			handler.handleChildrenOf(current);
		} else {
			handler.handleCumulativeChildrenOf(current);
		}
	}

	private void handleOverridesForCurrent() {
		Set<K> omit = new HashSet<>(items.keySet());
		omit.retainAll(overridden);
		if(omit.isEmpty()) {
			handler.handleChildrenOf(current);
		}
		else {
			repeatNonOverriddenItems();
		}
		overridden.addAll(getCurrentOverrides());
		overridden.removeAll(getCurrentNonOverrides());
	}

	private void repeatNonOverriddenItems() {
		Set<K> retain = new HashSet<>(items.keySet());
		retain.removeAll(overridden);
		if(! retain.isEmpty()) {
			handler.handleSelectedChildren(selectChildren(retain), current);
		}
	}

	private List<ElementChild> selectChildren(Set<K> keys) {
		return getChildrenOf(current).stream()
				.filter(c -> keys.contains(keyOf(c)))
				.collect(Collectors.toList());
						
	}

	private Set<K> getCurrentOverrides() {
		return getWithOverrideStatus(true);
	}

	private Set<K> getCurrentNonOverrides() {
		return getWithOverrideStatus(false);
	}

	private Set<K> getWithOverrideStatus(final boolean overrideStatus) {
		return items.keySet().stream()
				.filter(k -> items.get(k).booleanValue() == overrideStatus)
				.collect(Collectors.toSet());		
	}

	abstract List<? extends ElementChild> getChildrenOf(FrankElement arg);
	abstract FrankElement nextAncestor(FrankElement arg);
	abstract K keyOf(ElementChild arg);
}
