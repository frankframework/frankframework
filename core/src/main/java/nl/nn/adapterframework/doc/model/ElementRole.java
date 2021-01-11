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

import java.util.HashMap;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.nn.adapterframework.doc.Utils;

public class ElementRole {
	private final @Getter ElementType elementType;
	private final @Getter String syntax1Name;
	private final int syntax1NameSeq;

	private ElementRole(ElementType elementType, String syntax1Name, int syntax1NameSeq) {
		this.elementType = elementType;
		this.syntax1Name = syntax1Name;
		this.syntax1NameSeq = syntax1NameSeq;
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

	public Key getKey() {
		return new Key(elementType.getFullName(), syntax1Name);
	}

	@Override
	public String toString() {
		return getKey().toString();
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
	}
}
