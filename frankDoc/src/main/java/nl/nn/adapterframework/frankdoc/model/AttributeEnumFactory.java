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

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.util.LogUtil;

class AttributeEnumFactory {
	private static Logger log = LogUtil.getLogger(AttributeEnumFactory.class);

	private Map<String, AttributeEnum> allAttributeEnumInstances = new LinkedHashMap<>();
	private Map<String, Integer> lastAssignedSeq = new HashMap<>();

	AttributeEnum findOrCreateAttributeEnum(FrankClass clazz) {
		List<AttributeEnumValue> values = Arrays.asList(clazz.getEnumConstants()).stream()
				.map(AttributeEnumValue::new)
				.collect(Collectors.toList());
		boolean labelsForgotten = values.stream().map(AttributeEnumValue::isExplicitLabel).distinct().collect(Collectors.counting()).longValue() >= 2;
		if(labelsForgotten) {
			log.warn("Some enum values of class [{}] have a label, but not all. Did you forget some?", clazz.getName());
		}
		return findOrCreateAttributeEnum(clazz.getName(), clazz.getSimpleName(), values);
	}

	AttributeEnum findOrCreateAttributeEnum(String fullName, String simpleName, List<AttributeEnumValue> values) {
		if(allAttributeEnumInstances.containsKey(fullName)) {
			return allAttributeEnumInstances.get(fullName);
		}
		int seq = lastAssignedSeq.getOrDefault(simpleName, 0);
		seq++;
		AttributeEnum result = new AttributeEnum(fullName, simpleName, values, seq);
		lastAssignedSeq.put(simpleName, seq);
		allAttributeEnumInstances.put(fullName, result);
		return result;
	}

	AttributeEnum findAttributeEnum(String enumTypeFullName) {
		return allAttributeEnumInstances.get(enumTypeFullName);
	}

	List<AttributeEnum> getAll() {
		return new ArrayList<>(allAttributeEnumInstances.values());
	}

	int size() {
		return allAttributeEnumInstances.size();
	}
}
