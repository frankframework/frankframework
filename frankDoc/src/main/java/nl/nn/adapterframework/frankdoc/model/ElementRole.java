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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.frankdoc.Utils;
import nl.nn.adapterframework.util.LogUtil;

public class ElementRole implements Comparable<ElementRole> {
	private static Logger log = LogUtil.getLogger(ElementRole.class);

	private static final Comparator<ElementRole> COMPARATOR =
			Comparator.comparing(ElementRole::getRoleName).thenComparing(role -> role.getElementType().getFullName());

	private final @Getter ElementType elementType;
	private final @Getter String roleName;
	private final int roleNameSeq;

	// Used to solve conflicts between members and the element name of the
	// generic element option, see package-info of this package.
	private FrankElement defaultElementOptionConflict;

	// Used to solve element role member conflicts as described in
	// the package-info of this package.
	private Set<FrankElement> nameConflicts;

	// Used to resolve member conflicts in shared generic element options as
	// explained in the package-info of this package.
	private Set<ElementRoleSet> participatesInRoleSets = new HashSet<>();

	// Used to solve conflict caused by ElementType interface inheritance.
	private @Setter(AccessLevel.PACKAGE) ElementRole highestCommonInterface;

	private ElementRole(ElementType elementType, String roleName, int roleNameSeq) {
		this.elementType = elementType;
		this.roleName = roleName;
		this.roleNameSeq = roleNameSeq;
		defaultElementOptionConflict = null;
	}

	// TODO: A Lombok getter would look better, but it does not work properly with Javadoc.
	// A {@link} JavaDoc annotation pointing to a Lombok getter causes the method name to appear
	// in the text but not as a link.
	/**
	 * Used to resolve conflicts by {@link nl.nn.adapterframework.frankdoc.model.ElementType} interface
	 * inheritance, see {@link nl.nn.adapterframework.frankdoc.model}.
	 */
	public ElementRole getHighestCommonInterface() {
		return highestCommonInterface;
	}

	// TODO: Same problem with Lombok
	/**
	 * Used to resolve conflicts between {@link nl.nn.adapterframework.frankdoc.model.ElementRole} members
	 * and the element name of the generic element option, see {@link nl.nn.adapterframework.frankdoc.model}.
	 */
	public FrankElement getDefaultElementOptionConflict() {
		return defaultElementOptionConflict;
	}

	void addParticipatingRoleSet(ElementRoleSet roleSet) {
		participatesInRoleSets.add(roleSet);
	}

	/**
	 * Can only be called after all {@link ConfigChild},
	 * {@link ElementRole}, {@link ElementType}
	 *  and {@link FrankElement} have been created.
	 */
	void initConflicts() {
		nameConflicts = new HashSet<>();
		Map<String, List<FrankElement>> membersByXsdName = elementType.getMembers().stream()
				.collect(Collectors.groupingBy(el -> el.getXsdElementName(this)));
		Set<String> conflictNames = membersByXsdName.keySet().stream()
				.filter(name -> membersByXsdName.get(name).size() >= 2)
				.collect(Collectors.toSet());
		for(String name: conflictNames) {
			Map<Boolean, List<FrankElement>> conflictingElementsByDeprecated = membersByXsdName.get(name).stream()
					.collect(Collectors.groupingBy(FrankElement::isDeprecated));
			if(conflictingElementsByDeprecated.get(false).size() != 1) {
				log.warn("Cannot resolve XML name conflict for non-deprecated FrankElement-s [{}]", () -> FrankElement.describe(conflictingElementsByDeprecated.get(false)));
				nameConflicts.addAll(membersByXsdName.get(name));
			} else {
				nameConflicts.addAll(conflictingElementsByDeprecated.get(true));
			}
		}
		List<FrankElement> defaultOptionConflictCandidates = membersByXsdName.get(getGenericOptionElementName());
		if(defaultOptionConflictCandidates != null) {
			Set<FrankElement> asSet = new HashSet<>(defaultOptionConflictCandidates);
			asSet.removeAll(nameConflicts);
			if(asSet.size() == 1) {
				defaultElementOptionConflict = asSet.iterator().next();
			} else if(asSet.size() >= 2) {
				throw new IllegalArgumentException("Programming error. Something went wrong resolving name conflicts, please debug");
			}
		}
	}

	public String getGenericOptionElementName() {
		return Utils.toUpperCamelCase(roleName);
	}

	List<FrankElement> getRawMembers() {
		try {
		return elementType.getMembers().stream()
				.filter(el -> ! nameConflicts.contains(el))
				.collect(Collectors.toList());
		} catch(Exception e) {
			throw(e);
		}
	}

	public List<FrankElement> getMembers() {
		return getRawMembers().stream()
				.filter(frankElement -> noConflictingRoleSet(frankElement))
				.collect(Collectors.toList());
	}

	private boolean noConflictingRoleSet(FrankElement frankElement) {
		return participatesInRoleSets.stream().noneMatch(roleSet -> roleSet.conflictsWithGenericElementOptionElementName(frankElement));
	}

	public String createXsdElementName(String kindDifferentiatingWord) {
		return getGenericOptionElementName() + kindDifferentiatingWord + disambiguation();
	}

	private String disambiguation() {
		if(roleNameSeq == 1) {
			return "";
		} else {
			return "_" + roleNameSeq;
		}
	}

	public Key getKey() {
		return new Key(elementType.getFullName(), roleName);
	}

	@Override
	public int compareTo(ElementRole other) {
		return COMPARATOR.compare(this, other);
	}

	/**
	 * Solves conflicts by {@link nl.nn.adapterframework.frankdoc.model.ElementType}
	 * interface inheritance.
	 */
	public static List<ElementRole> promoteIfConflict(List<ElementRole> roles) {
		if(roles.size() == 1) {
			return roles;
		} else {
			return roles.stream().map(ElementRole::getHighestCommonInterface).distinct().collect(Collectors.toList());
		}
	}

	@Override
	public String toString() {
		return getKey().toString();
	}

	public static String describeCollection(Collection<ElementRole> roles) {
		return roles.stream().map(ElementRole::toString).collect(Collectors.joining(", "));
	}

	static class Factory {
		private final Map<String, Integer> numUsagePerRoleName = new HashMap<>();

		ElementRole create(ElementType elementType, String roleName) {
			return new ElementRole(elementType, roleName, newRoleNameSeq(roleName));
		}

		private int newRoleNameSeq(String roleName) {
			int maxExistingRoleNameSeq = numUsagePerRoleName.getOrDefault(roleName, 0);
			int roleNameSeq = maxExistingRoleNameSeq + 1;
			numUsagePerRoleName.put(roleName, roleNameSeq);
			return roleNameSeq;
		}
	}

	@EqualsAndHashCode
	public static final class Key {
		private final @Getter String elementTypeName;
		private final @Getter String elementTypeSimpleName;
		private final @Getter String roleName;

		public Key(String elementTypeName, String roleName) {
			this.elementTypeName = elementTypeName;
			this.roleName = roleName;
			int index = elementTypeName.lastIndexOf(".");
			this.elementTypeSimpleName = elementTypeName.substring(index + 1);
		}

		public Key(ObjectConfigChild configChild) {
			this(configChild.getElementType().getFullName(), configChild.getRoleName());
		}

		@Override
		public String toString() {
			return "(" + elementTypeSimpleName + ", " + roleName + ")"; 
		}

		public static String describeCollection(Collection<ElementRole.Key> keys) {
			return keys.stream().map(key -> key.toString()).collect(Collectors.joining(", "));
		}
	}
}
