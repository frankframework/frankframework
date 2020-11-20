package nl.nn.adapterframework.doc.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

abstract class AncestorChildNavigation<K, T extends ElementChild<T>> {
	private final CumulativeChildHandler<T> handler;
	private final Function<FrankElement, List<T>> childFunction;
	private FrankElement current;
	private Map<K, Boolean> items;
	private Set<K> overridden;

	AncestorChildNavigation(CumulativeChildHandler<T> handler, Function<FrankElement, List<T>> childFunction) {
		this.handler = handler;
		this.childFunction = childFunction;
	}

	void run(FrankElement start) {
		enter(start);
		handler.handleChildrenOf(start);
		overridden = getCurrentOverrides();
		while(nextAncestor(current, childFunction) != null) {
			enter(nextAncestor(current, childFunction));
			if(overridden.isEmpty()) {
				safeAddCumulative();
				return;
			}
			handleOverridesForCurrent();
		}
	}

	static <U extends ElementChild<U>> FrankElement nextAncestor(FrankElement elem, Function<FrankElement, List<U>> fun) {
		FrankElement ancestor = elem.getParent();
		while((ancestor != null) && (fun.apply(ancestor).size() == 0)) {
			ancestor = ancestor.getParent();
		}
		return ancestor;
	}

	private void enter(FrankElement current) {
		this.current = current;
		items = new HashMap<>();
		for(T c: childFunction.apply(current)) {
			items.put(keyOf(c), c.getOverriddenFrom() != null);
		}
	}

	private void safeAddCumulative() {
		if(nextAncestor(current, childFunction) == null) {
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

	private List<T> selectChildren(Set<K> keys) {
		return childFunction.apply(current).stream()
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

	abstract K keyOf(T arg);
}
