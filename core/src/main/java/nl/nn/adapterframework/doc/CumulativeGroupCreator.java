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

package nl.nn.adapterframework.doc;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import nl.nn.adapterframework.doc.model.FrankElement;

/**
 * Strategy class for handling overrides while writing an XML schema from a FrankDocModel.
 * This class is reused for building attribute groups and building config child groups.
 * The use of this class is explained for attribute groups.
 *
 * For each FrankElement in the model, we need an XSD attributeGroup that lists
 * all attributes and all inherited attributes. To keep the XSD small, we would like
 * to list the inherited attributes by referencing the attributeGroup of the parent.
 * This is not possible however when the current FrankElement overrides attributes.
 * In this case, the data from the current FrankElement is needed and attribute that
 * was overridden should be omitted.
 *
 * To support this, we make two kinds of groups for each FrankElement:
 * <ul>
 * <li> The cumulative group that holds all attributes (config children) and all inherited attributes.
 * <li> The declared group that holds only the declared attributes (config children).
 * This class traverses the inheritance hierarchy of a FrankElement and chooses whether
 * to repeat attributes, to reference the declared group or to reference the cumulative group.
 * @author martijn
 *
 * @param <K> The key type for referencing attributes or config children.
 */
abstract class CumulativeGroupCreator<K> {
	private FrankElement current;
	private Map<K, Boolean> items;
	private Set<K> overridden;
	private boolean isGroupRefRepetitionNotified;

	CumulativeGroupCreator() {
	}

	void run(FrankElement start) {
		enter(start);
		addDeclaredGroup(start);
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
		items = itemsOf(current);		
	}

	private void safeAddCumulative() {
		if(nextAncestor(current) == null) {
			addDeclaredGroup(current);
		} else {
			addCumulativeGroup(current);
		}
	}

	private void handleOverridesForCurrent() {
		handleNotifyGroupRepetition();
		Set<K> omit = new HashSet<>(items.keySet());
		omit.retainAll(overridden);
		if(omit.isEmpty()) {
			addDeclaredGroup(current);
		}
		else {
			repeatNonOverriddenItems();
		}
		overridden.addAll(getCurrentOverrides());
		overridden.removeAll(getCurrentNonOverrides());
	}

	private void repeatNonOverriddenItems() {
		notifyItemsRepeated(current);
		Set<K> retain = new HashSet<>(items.keySet());
		retain.removeAll(overridden);
		if(! retain.isEmpty()) {
			addItemsOf(retain, current);	
		}
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

	private void handleNotifyGroupRepetition() {
		if(! isGroupRefRepetitionNotified) {
			notifyGroupRefRepeated(current);
			isGroupRefRepetitionNotified = true;
		}
	}

	abstract FrankElement nextAncestor(FrankElement element);
	abstract Map<K, Boolean> itemsOf(FrankElement frankElement);
	abstract void addItemsOf(Set<K> items, FrankElement itemOwner);
	abstract void addDeclaredGroup(FrankElement frankElement);
	abstract void addCumulativeGroup(FrankElement frankElement);
	abstract void notifyGroupRefRepeated(FrankElement frankElement);
	abstract void notifyItemsRepeated(FrankElement frankElement);
}
