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

package nl.nn.adapterframework.doc.model;

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
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.util.LogUtil;

public class ElementRole implements Comparable<ElementRole> {
	private static Logger log = LogUtil.getLogger(ElementRole.class);

	private static final Comparator<ElementRole> COMPARATOR =
			Comparator.comparing(ElementRole::getSyntax1Name).thenComparing(role -> role.getElementType().getFullName());

	private final @Getter ElementType elementType;
	private final @Getter String syntax1Name;
	private final int syntax1NameSeq;
	private @Getter FrankElement defaultElementOptionConflict;
	private Set<FrankElement> nameConflicts;
	private Set<ElementRoleSet> participatesInRoleSets = new HashSet<>();
	private @Getter @Setter(AccessLevel.PACKAGE) ElementRole highestCommonInterface;

	private ElementRole(ElementType elementType, String syntax1Name, int syntax1NameSeq) {
		this.elementType = elementType;
		this.syntax1Name = syntax1Name;
		this.syntax1NameSeq = syntax1NameSeq;
		defaultElementOptionConflict = null;
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
				log.warn(String.format("Cannot resolve XML name conflict for non-deprecated FrankElement-s [%s]",
						FrankElement.describe(conflictingElementsByDeprecated.get(false))));
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
		return Utils.toUpperCamelCase(syntax1Name);
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
		return participatesInRoleSets.stream().noneMatch(roleSet -> roleSet.isConflict(frankElement));
	}

	public String createXsdElementName(String kindDifferentiatingWord) {
		return getGenericOptionElementName() + kindDifferentiatingWord + disambiguation();
	}

	private String disambiguation() {
		if(syntax1NameSeq == 1) {
			return "";
		} else {
			return "_" + syntax1NameSeq;
		}
	}

	public Key getKey() {
		return new Key(elementType.getFullName(), syntax1Name);
	}

	@Override
	public int compareTo(ElementRole other) {
		return COMPARATOR.compare(this, other);
	}

	@Override
	public String toString() {
		return getKey().toString();
	}

	public static String describeCollection(Collection<ElementRole> roles) {
		return roles.stream().map(ElementRole::toString).collect(Collectors.joining(", "));
	}

	static class Factory {
		private final Map<String, Integer> numUsagePerSyntax1Name = new HashMap<>();

		ElementRole create(ElementType elementType, String syntax1Name) {
			return new ElementRole(elementType, syntax1Name, newSyntax1NameSeq(syntax1Name));
		}

		private int newSyntax1NameSeq(String syntax1Name) {
			int maxExistingSyntax1NameSeq = numUsagePerSyntax1Name.getOrDefault(syntax1Name, 0);
			int syntax1NameSeq = maxExistingSyntax1NameSeq + 1;
			numUsagePerSyntax1Name.put(syntax1Name, syntax1NameSeq);
			return syntax1NameSeq;
		}
	}

	@EqualsAndHashCode
	public static class Key {
		private @Getter String elementTypeName;
		private @Getter String syntax1Name;

		public Key(String elementTypeName, String syntax1Name) {
			this.elementTypeName = elementTypeName;
			this.syntax1Name = syntax1Name;
		}

		public Key(ConfigChild configChild) {
			this(configChild.getElementType().getFullName(), configChild.getSyntax1Name());
		}

		@Override
		public String toString() {
			return "(" + elementTypeName + ", " + syntax1Name + ")"; 
		}

		public static String describeCollection(Collection<ElementRole.Key> keys) {
			return keys.stream().map(key -> key.toString()).collect(Collectors.joining(", "));
		}
	}
}
