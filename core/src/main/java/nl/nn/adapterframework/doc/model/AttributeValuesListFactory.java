package nl.nn.adapterframework.doc.model;

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

	int size() {
		return allAttributeValuesLists.size();
	}
}
