/* 
Copyright 2020, 2021 WeAreFrank! 

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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.AccessLevel;
import lombok.Getter;
import nl.nn.adapterframework.frankdoc.doclet.FrankClass;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.Misc;

/**
 * Models a collection of FrankElement. The collection can be characterized by
 * a Java interface in the FF! sources, or there can be one member that is referenced
 * by its FrankElement. FrankElement objects that model an abstract Java class should
 * be omitted as members. This is done automatically when Spring is used to get the
 * implementing classes of a modeled Java interface.
 *
 * @author martijn
 *
 */
public class ElementType implements Comparable<ElementType> {
	private static Logger log = LogUtil.getLogger(ElementType.class);

	private static final Comparator<ElementType> COMPARATOR =
			Comparator.comparing(ElementType::getSimpleName).thenComparing(ElementType::getFullName);

	private @Getter(AccessLevel.PACKAGE) List<FrankElement> members;
	private @Getter boolean fromJavaInterface;
	
	private static class InterfaceHierarchyItem {
		private @Getter String fullName;
		private @Getter String simpleName;
		private @Getter Map<String, InterfaceHierarchyItem> parentInterfaces = new TreeMap<>();

		InterfaceHierarchyItem(FrankClass clazz) {
			this.fullName = clazz.getName();
			this.simpleName = clazz.getSimpleName();
			if(clazz.isInterface()) {
				for(FrankClass superInterface: clazz.getInterfaces()) {
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
	private final @Getter FrankDocGroup group;

	ElementType(FrankClass clazz, FrankDocGroup group) {
		interfaceHierarchy = new InterfaceHierarchyItem(clazz);
		members = new ArrayList<>();
		this.fromJavaInterface = clazz.isInterface();
		this.group = group;
	}

	public String getFullName() {
		return interfaceHierarchy.getFullName();
	}

	public String getSimpleName() {
		return interfaceHierarchy.getSimpleName();
	}

	// This is not about FrankDocGroups, but about groups in the XSDs.
	String getGroupName() {
		String result = getSimpleName();
		if(result.startsWith("I")) {
			result = result.substring(1);
		}
		return result;
	}

	void addMember(FrankElement member) {
		Misc.addToSortedListNonUnique(members, member);
	}

	FrankElement getSingletonElement() throws ReflectiveOperationException {
		if(members.size() != 1) {
			throw new ReflectiveOperationException(String.format("Expected that ElementType [%s] contains exactly one element", getFullName()));
		}
		return members.iterator().next();
	}

	void calculateHighestCommonInterface(FrankDocModel model) {
		highestCommonInterface = this;
		ElementType nextCandidate = highestCommonInterface.getNextCommonInterface(model);
		while(nextCandidate != null) {
			highestCommonInterface = nextCandidate;
			nextCandidate = highestCommonInterface.getNextCommonInterface(model);
		}
		log.trace("ElementType [{}] has highest common interface [{}]",
				() -> this.getFullName(), () -> highestCommonInterface.getFullName());
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
				log.warn("There are multiple candidates for the next common interface of ElementType [{}], which are [{}]. Chose [{}]",
						() -> getFullName(), () -> candidates.stream().map(ElementType::getFullName).collect(Collectors.joining(", ")), () -> result.getFullName());
			}
			return result;
		}
	}

	/**
	 * Get the members that can be referenced with syntax 2. Only non-abstracts are returned.
	 */
	public List<FrankElement> getSyntax2Members() {
		return members.stream()
				.filter(frankElement -> ! frankElement.getXmlElementNames().isEmpty())
				.filter(f -> ! f.syntax2ExcludedFromType(this.getFullName()))
				.sorted()
				.collect(Collectors.toList());
	}

	@Override
	public int compareTo(ElementType other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public String toString() {
		return "ElementType " + interfaceHierarchy.getFullName();
	}
}
