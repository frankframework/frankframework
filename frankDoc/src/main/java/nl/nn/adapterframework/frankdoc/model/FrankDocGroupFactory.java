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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.Logger;

import nl.nn.adapterframework.frankdoc.doclet.FrankAnnotation;
import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.frankdoc.doclet.FrankDocException;
import nl.nn.adapterframework.util.LogUtil;

class FrankDocGroupFactory {
	private static final String JAVADOC_GROUP_ANNOTATION = "nl.nn.adapterframework.doc.FrankDocGroup";

	private static Logger log = LogUtil.getLogger(FrankDocGroupFactory.class);

	private final Map<String, FrankDocGroup> allGroups = new HashMap<>();

	FrankDocGroup getGroup(FrankClass clazz) {
		FrankDocGroup result = null;
		try {
			FrankAnnotation annotation = clazz.getAnnotationIncludingInherited(JAVADOC_GROUP_ANNOTATION);
			if(annotation == null) {
				result = getGroup(FrankDocGroup.GROUP_NAME_OTHER);
			} else {
				String groupName = (String) annotation.getValueOf("name");
				Integer groupOrder = (Integer) annotation.getValueOf("order");
				log.trace("FrankDocGroup requested for group name {} with new order {}", () -> groupName, () -> groupOrder);
				result = getGroup(groupName, groupOrder);
			}
		} catch(FrankDocException e) {
			log.warn("Class [{}] has invalid @FrankDocGroup: {}", clazz.getName(), e.getMessage());
			return getGroup(FrankDocGroup.GROUP_NAME_OTHER);
		}
		return result;
	}

	FrankDocGroup getGroup(String groupName, Integer groupOrder) {
		FrankDocGroup result;
		result = getGroup(groupName);
		if(groupOrder == null) {
			// The Doclet API does not provide the default value of the @FrankDocGroup annotation.
			// We need to repeat it here.
			groupOrder = Integer.MAX_VALUE;
		}
		result.setOrder(groupOrder);
		return result;
	}

	private FrankDocGroup getGroup(String name) {
		if(allGroups.containsKey(name)) {
			return allGroups.get(name);
		} else {
			FrankDocGroup group = new FrankDocGroup(name);
			allGroups.put(name, group);
			return group;
		}
	}

	List<FrankDocGroup> getAllGroups() {
		List<FrankDocGroup> result = new ArrayList<>(allGroups.values());
		Collections.sort(result);
		return result;
	}
}
