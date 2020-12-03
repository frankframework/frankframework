/* 
Copyright 2020 WeAreFrank! 

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

package nl.nn.adapterframework.doc.model;

import static nl.nn.adapterframework.doc.model.ElementChild.ALL;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

class AncestorChildNavigation<K, T extends ElementChild> {
	private final CumulativeChildHandler<T> handler;
	private final Predicate<ElementChild> childSelector;
	private final ChildRejector<T> rejector;
	private final Class<T> kind;
	private FrankElement current;
	private Map<String, Boolean> items;
	private Set<String> overridden;
	

	AncestorChildNavigation(
			CumulativeChildHandler<T> handler,
			Predicate<ElementChild> childSelector,
			Predicate<ElementChild> childRejector,
			Class<T> kind) {
		this.handler = handler;
		this.childSelector = childSelector;
		this.rejector = new ChildRejector<T>(childSelector, childRejector, kind);
		this.kind = kind;
	}

	void run(FrankElement start) {
		this.rejector.init(start);
		enter(start);
		overridden = new HashSet<>();
		declaredGroupOrRepeatedChildren();
		while(current.getNextAncestor(childSelector, kind) != null) {
			enter(current.getNextAncestor(childSelector, kind));
			if(overridden.isEmpty() && rejector.isNoCumulativeRejected(current)) {
				safeAddCumulative();
				return;
			}
			declaredGroupOrRepeatedChildren();
		}
	}

	private void enter(FrankElement current) {
		this.current = current;
		List<ElementChild> children = rejector.getChildrenFor(current);
		items = new HashMap<>();
		for(ElementChild c: children) {
			items.put(c.getKey(), c.getOverriddenFrom() != null);
		}
	}

	private void declaredGroupOrRepeatedChildren() {
		Set<String> omit = new HashSet<>(items.keySet());
		omit.retainAll(overridden);
		if(omit.isEmpty() && rejector.isNoDeclaredRejected(current)) {
			handler.handleChildrenOf(current);
		}
		else {
			repeatNonOverriddenItems();
		}
		overridden.addAll(getCurrentOverrides());
		overridden.removeAll(getCurrentNonOverrides());
	}

	private void safeAddCumulative() {
		if(current.getNextAncestor(childSelector, kind) == null) {
			handler.handleChildrenOf(current);
		} else {
			handler.handleCumulativeChildrenOf(current);
		}
	}

	private void repeatNonOverriddenItems() {
		Set<String> retain = new HashSet<>(items.keySet());
		retain.removeAll(overridden);
		if(! retain.isEmpty()) {
			handler.handleSelectedChildren(selectChildren(retain), current);
		}
	}

	@SuppressWarnings("unchecked")
	private List<T> selectChildren(Set<String> keys) {
		return current.getChildren(ALL, kind).stream()
				.filter(c -> keys.contains(c.getKey()))
				.map(c -> (T) c)
				.collect(Collectors.toList());
						
	}

	private Set<String> getCurrentOverrides() {
		return getWithOverrideStatus(true);
	}

	private Set<String> getCurrentNonOverrides() {
		return getWithOverrideStatus(false);
	}

	private Set<String> getWithOverrideStatus(final boolean overrideStatus) {
		return items.keySet().stream()
				.filter(k -> items.get(k).booleanValue() == overrideStatus)
				.collect(Collectors.toSet());		
	}
}
