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

import nl.nn.adapterframework.doc.model.ElementChild.AbstractKey;

class AncestorChildNavigation<T extends ElementChild> {
	private final CumulativeChildHandler<T> handler;
	private final ChildRejector<T> rejector;
	private final Class<T> kind;
	private FrankElement current;
	private Map<AbstractKey, Boolean> items;
	private Set<AbstractKey> overridden;
	private Predicate<FrankElement> noChildren;

	AncestorChildNavigation(CumulativeChildHandler<T> handler, Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Class<T> kind) {
		this.handler = handler;
		this.rejector = new ChildRejector<T>(childSelector, childRejector, kind);
		this.kind = kind;
		this.noChildren = el -> el.getChildrenOfKind(childSelector, kind).isEmpty();
	}

	void run(FrankElement start) {
		this.rejector.init(start);
		enter(start);
		overridden = new HashSet<>();
		addDeclaredGroupOrRepeatChildrenInXsd();
		while(current.getNextAncestorThatHasChildren(noChildren) != null) {
			enter(current.getNextAncestorThatHasChildren(noChildren));
			if(overridden.isEmpty() && rejector.isNoCumulativeRejected(current)) {
				safeAddCumulative();
				return;
			}
			addDeclaredGroupOrRepeatChildrenInXsd();
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

	private void addDeclaredGroupOrRepeatChildrenInXsd() {
		Set<AbstractKey> omit = new HashSet<>(items.keySet());
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
		if(current.getNextAncestorThatHasChildren(noChildren) == null) {
			handler.handleChildrenOf(current);
		} else {
			handler.handleCumulativeChildrenOf(current);
		}
	}

	private void repeatNonOverriddenItems() {
		Set<AbstractKey> retain = new HashSet<>(items.keySet());
		retain.removeAll(overridden);
		if(! retain.isEmpty()) {
			handler.handleSelectedChildren(selectChildren(retain), current);
		}
	}

	private List<T> selectChildren(Set<AbstractKey> keys) {
		return current.getChildrenOfKind(ALL, kind).stream()
				.filter(c -> keys.contains(c.getKey()))
				.map(c -> (T) c)
				.collect(Collectors.toList());
						
	}

	private Set<AbstractKey> getCurrentOverrides() {
		return getWithOverrideStatus(true);
	}

	private Set<AbstractKey> getCurrentNonOverrides() {
		return getWithOverrideStatus(false);
	}

	private Set<AbstractKey> getWithOverrideStatus(final boolean overrideStatus) {
		return items.keySet().stream()
				.filter(k -> items.get(k).booleanValue() == overrideStatus)
				.collect(Collectors.toSet());		
	}
}
