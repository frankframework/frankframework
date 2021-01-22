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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Logger;

import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.doc.Utils;
import nl.nn.adapterframework.util.LogUtil;

public class ElementRole {
	private static Logger log = LogUtil.getLogger(ElementRole.class);

	private @Getter ElementType elementType;
	private final @Getter String syntax1Name;
	private final int syntax1NameSeq;
	private @Getter boolean deprecated;
	private @Getter @Setter(AccessLevel.PACKAGE) boolean superseded;

	private ElementRole(ElementType elementType, String syntax1Name, int syntax1NameSeq, boolean isDeprecated) {
		this.elementType = elementType;
		this.syntax1Name = syntax1Name;
		this.syntax1NameSeq = syntax1NameSeq;
		this.deprecated = isDeprecated;
		this.superseded = false;
	}

	void updateDeprecated(boolean newDeprecated) {
		if(deprecated != newDeprecated) {
			log.warn(String.format("Ambiguous deprecated status of ElementRole [%s], set to false", toString()));
		}
		deprecated = (deprecated && newDeprecated);
	}

	public String createXsdElementName(String kindDifferentiatingWord) {
		return Utils.toUpperCamelCase(syntax1Name) + kindDifferentiatingWord + disambiguation();
	}

	private String disambiguation() {
		if(syntax1NameSeq == 1) {
			return "";
		} else {
			return "_" + syntax1NameSeq;
		}
	}

	// TODO: Cover with unit tests and document
	public String getGenericOptionElementName() {
		// Do not include sequence number that made the role name unique.
		String result = Utils.toUpperCamelCase(syntax1Name);
		Set<String> conflictCandidates = getOptions(f -> true).stream()
				.map(f -> f.getXsdElementName(this))
				.collect(Collectors.toSet());
		if(conflictCandidates.contains(result)) {
			result = "Generic" + result;
		}
		return result;
	}

	// TODO: Cover with unit tests and document
	public List<FrankElement> getOptions(Predicate<FrankElement> frankElementFilter) {
		List<FrankElement> frankElementOptions = elementType.getMembers().values().stream()
				.filter(frankElementFilter)
				.filter(f -> ! f.isAbstract())
				.collect(Collectors.toList());
		frankElementOptions.sort((o1, o2) -> o1.getSimpleName().compareTo(o2.getSimpleName()));
		return frankElementOptions;
	}

	public Key getKey() {
		return new Key(elementType.getFullName(), syntax1Name);
	}

	@Override
	public String toString() {
		return getKey().toString();
	}

	public static String collection2String(Collection<ElementRole> c) {
		return c.stream()
				.map(role -> role.toString())
				.collect(Collectors.joining(", "));		
	}

	static class Factory {
		private final Map<String, Integer> numUsagePerSyntax1Name = new HashMap<>();

		ElementRole create(ElementType elementType, String syntax1Name, boolean isDeprecated) {
			return new ElementRole(elementType, syntax1Name, newSyntax1NameSeq(syntax1Name), isDeprecated);
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
	}
}
