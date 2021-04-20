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
package nl.nn.adapterframework.frankdoc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import nl.nn.adapterframework.frankdoc.model.ConfigChildSet;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.ElementRole;
import nl.nn.adapterframework.frankdoc.model.ElementRole.Key;
import nl.nn.adapterframework.util.XmlBuilder;

class ElementGroupManager {
	private static final String ELEMENT_GROUP = "ElementGroup";

	private final Map<Set<ElementRole.Key>, Integer> genericGroupKeyToSeq = new HashMap<>();
	private final Map<Set<ElementRole.Key>, GenericOptionAttributeTask> genericOptionAttributeTasks = new LinkedHashMap<>();
	private final Predicate<ElementChild> childSelector;
	private final Predicate<ElementChild> childRejector;

	ElementGroupManager(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
	}

	boolean groupExists(Set<ElementRole.Key> key) {
		return genericGroupKeyToSeq.containsKey(key);
	}

	private Set<Key> keyOf(ConfigChildSet configChildSet) {
		return ConfigChildSet.getKey(configChildSet.getFilteredElementRoles(childSelector, childRejector));
	}

	String addGroup(Set<ElementRole.Key> key) {
		String roleName = getRoleName(key);
		List<Set<ElementRole.Key>> shared = genericGroupKeyToSeq.keySet().stream()
				.filter(rs -> getRoleName(rs).equals(roleName))
				.collect(Collectors.toList());
		int seq = shared.stream().map(genericGroupKeyToSeq::get).collect(Collectors.maxBy(Integer::compare)).orElse(0) + 1;
		genericGroupKeyToSeq.put(key, seq);
		return getGroupName(key, roleName);
	}

	static String getRoleName(List<ElementRole> roles) {
		return roles.get(0).getRoleName();
	}

	private String getRoleName(Set<ElementRole.Key> key) {
		return key.iterator().next().getRoleName();
	}

	String getGroupName(Set<ElementRole.Key> key) {
		String roleName = getRoleName(key);
		return getGroupName(key, roleName);
	}

	String getGroupName(List<ElementRole> roles) {
		Set<ElementRole.Key> key = ConfigChildSet.getKey(roles);
		String roleName = roles.iterator().next().getRoleName();
		return getGroupName(key, roleName);
	}

	private String getGroupName(Set<ElementRole.Key> key, String roleName) {
		return Utils.toUpperCamelCase(roleName) + ELEMENT_GROUP + disambiguation(genericGroupKeyToSeq.get(key));
	}

	static String disambiguation(int seq) {
		String result = "";
		if(seq != 1) {
			result = String.format("_%d", seq);
		}
		return result;
	}

	void addGenericOptionAttributeTask(List<ElementRole> roles, XmlBuilder builder) {
		Set<ElementRole.Key> key = ConfigChildSet.getKey(roles);
		genericOptionAttributeTasks.put(key, new GenericOptionAttributeTask(key, builder));
	}

	boolean hasGenericOptionAttributeTask(ConfigChildSet configChildSet) {
		return genericOptionAttributeTasks.containsKey(keyOf(configChildSet));
	}

	XmlBuilder doGenericOptionAttributeTask(ConfigChildSet configChildSet) {
		return genericOptionAttributeTasks.remove(keyOf(configChildSet)).getBuilder();
	}

	List<GenericOptionAttributeTask> doLeftoverGenericOptionAttributeTasks() {
		List<GenericOptionAttributeTask> result = new ArrayList<>(genericOptionAttributeTasks.values());
		genericOptionAttributeTasks.clear();
		return result;
	}
}
