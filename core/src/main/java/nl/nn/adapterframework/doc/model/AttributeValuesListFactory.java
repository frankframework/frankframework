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

package nl.nn.adapterframework.doc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class AttributeValuesListFactory {
	private Map<String, AttributeValuesList> allAttributeValuesLists = new LinkedHashMap<>();
	private Map<String, Integer> lastAssignedSeq = new HashMap<>();

	AttributeValuesList findOrCreateAttributeValuesList(Class<? extends Enum<?>> clazz) {
		List<String> values = Arrays.asList(clazz.getEnumConstants()).stream()
				.map(c -> c.name())
				.collect(Collectors.toList());
		return findOrCreateAttributeValuesList(clazz.getName(), clazz.getSimpleName(), values);
	}

	AttributeValuesList findOrCreateAttributeValuesList(String fullName, String simpleName, List<String> values) {
		if(allAttributeValuesLists.containsKey(fullName)) {
			return allAttributeValuesLists.get(fullName);
		}
		int seq = lastAssignedSeq.getOrDefault(simpleName, 0);
		seq++;
		AttributeValuesList result = new AttributeValuesList(fullName, simpleName, values, seq);
		lastAssignedSeq.put(simpleName, seq);
		allAttributeValuesLists.put(fullName, result);
		return result;
	}

	AttributeValuesList findAttributeValuesList(String enumTypeFullName) {
		return allAttributeValuesLists.get(enumTypeFullName);
	}

	List<AttributeValuesList> getAll() {
		return new ArrayList<>(allAttributeValuesLists.values());
	}

	int size() {
		return allAttributeValuesLists.size();
	}
}
