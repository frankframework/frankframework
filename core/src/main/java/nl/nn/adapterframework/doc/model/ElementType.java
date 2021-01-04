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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.Getter;
import nl.nn.adapterframework.util.LogUtil;

/**
 * Represents a type of FrankElement instances, which appears in the FF! Java code as
 * a Java interface. FrankElement objects that represent an abstract Java class should
 * be omitted as members. This is done automatically when Spring is used to get the
 * implementing classes of a Java interface.
 *
 * @author martijn
 *
 */
public class ElementType {
	private static Logger log = LogUtil.getLogger(ElementType.class);
	private @Getter Map<String, FrankElement> members;
	private @Getter boolean fromJavaInterface;
	
	private @Getter LinkedHashSet<ElementRole> elementRoles = new LinkedHashSet<>();

	private static class InterfaceHierarchyItem {
		private @Getter String fullName;
		private @Getter String simpleName;
		private @Getter Map<String, InterfaceHierarchyItem> parentInterfaces = new TreeMap<>();

		InterfaceHierarchyItem(Class<?> clazz) {
			this.fullName = clazz.getName();
			this.simpleName = clazz.getSimpleName();
			if(clazz.isInterface()) {
				for(Class<?> superInterface: clazz.getInterfaces()) {
					InterfaceHierarchyItem superInterfaceHierarchyItem = new InterfaceHierarchyItem(superInterface);
					parentInterfaces.put(superInterfaceHierarchyItem.getFullName(), superInterfaceHierarchyItem);
				}
			}
		}

		List<ElementType> findMatchingElementTypes(FrankDocModel model) {
			ElementType currentMatch = model.findElementType(fullName);
			if(currentMatch != null) {
				return Arrays.asList(currentMatch);
			}
			List<ElementType> result = new ArrayList<>();
			for(String parentKey: parentInterfaces.keySet()) {
				result.addAll(parentInterfaces.get(parentKey).findMatchingElementTypes(model));
			}
			return result;
		}
	}

	private final InterfaceHierarchyItem interfaceHierarchy;
	private @Getter ElementType highestCommonInterface;

	ElementType(Class<?> clazz) {
		interfaceHierarchy = new InterfaceHierarchyItem(clazz);
		members = new HashMap<>();
		this.fromJavaInterface = clazz.isInterface();
	}

	public String getFullName() {
		return interfaceHierarchy.getFullName();
	}

	public String getSimpleName() {
		return interfaceHierarchy.getSimpleName();
	}

	void addMember(FrankElement member) {
		members.put(member.getFullName(), member);
	}

	FrankElement getSingletonElement() throws ReflectiveOperationException {
		if(members.size() != 1) {
			throw new ReflectiveOperationException(String.format("Expected that ElementType [%s] contains exactly one element", getFullName()));
		}
		return members.values().iterator().next();
	}

	void registerElementRole(ElementRole elementRole) {
		elementRoles.add(elementRole);
	}

	void calculateHighestCommonInterface(FrankDocModel model) {
		highestCommonInterface = this;
		ElementType nextCandidate = highestCommonInterface.getNextCommonInterface(model);
		while(nextCandidate != null) {
			highestCommonInterface = nextCandidate;
			nextCandidate = highestCommonInterface.getNextCommonInterface(model);
		}
	}

	private ElementType getNextCommonInterface(FrankDocModel model) {
		if(! fromJavaInterface) {
			return null;
		}
		List<ElementType> candidates = new ArrayList<>();
		for(String key: interfaceHierarchy.getParentInterfaces().keySet()) {
			candidates.addAll(interfaceHierarchy.getParentInterfaces().get(key).findMatchingElementTypes(model));
		}
		if(candidates.isEmpty()) {
			return null;
		} else {
			ElementType result = candidates.get(0);
			if(candidates.size() >= 2) {
				log.warn(String.format("There are multiple candidates for the next common interface of ElementType [%s], which are [%s]. Chose [%s]",
						getFullName(),
						candidates.stream().map(ElementType::getFullName).collect(Collectors.joining(", ")),
						result.getFullName()));
			}
			return result;
		}
	}

	@Override
	public String toString() {
		return "ElementType " + interfaceHierarchy.getFullName();
	}
}
