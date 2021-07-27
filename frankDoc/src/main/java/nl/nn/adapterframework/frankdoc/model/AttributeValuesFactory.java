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

package nl.nn.adapterframework.frankdoc.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;

class AttributeValuesFactory {
	private Map<String, AttributeValues> allAttributeValuesInstances = new LinkedHashMap<>();
	private Map<String, Integer> lastAssignedSeq = new HashMap<>();

	AttributeValues findOrCreateAttributeValues(FrankClass clazz) {
		List<String> values = Arrays.asList(clazz.getEnumConstants()).stream()
				.map(c -> c.getName())
				.collect(Collectors.toList());
		return findOrCreateAttributeValues(clazz.getName(), clazz.getSimpleName(), values);
	}

	AttributeValues findOrCreateAttributeValues(String fullName, String simpleName, List<String> values) {
		if(allAttributeValuesInstances.containsKey(fullName)) {
			return allAttributeValuesInstances.get(fullName);
		}
		int seq = lastAssignedSeq.getOrDefault(simpleName, 0);
		seq++;
		AttributeValues result = new AttributeValues(fullName, simpleName, values, seq);
		lastAssignedSeq.put(simpleName, seq);
		allAttributeValuesInstances.put(fullName, result);
		return result;
	}

	AttributeValues findAttributeValues(String enumTypeFullName) {
		return allAttributeValuesInstances.get(enumTypeFullName);
	}

	List<AttributeValues> getAll() {
		return new ArrayList<>(allAttributeValuesInstances.values());
	}

	int size() {
		return allAttributeValuesInstances.size();
	}
}
