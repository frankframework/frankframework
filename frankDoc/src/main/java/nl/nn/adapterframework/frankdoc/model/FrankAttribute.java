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

package nl.nn.adapterframework.frankdoc.model;

import java.util.Comparator;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

public class FrankAttribute extends ElementChild implements Comparable<FrankAttribute> {
	@EqualsAndHashCode(callSuper = false)
	static class Key extends AbstractKey {
		private String name;

		Key(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	}

	private @Getter String name;
	private @Getter @Setter FrankElement describingElement;

	private @Getter @Setter AttributeType attributeType;

	/**
	 * Null if there is no restriction to the allowed attribute values. Should only be set if attributeType == {@link AttributeType#STRING}.
	 */
	private @Getter @Setter AttributeValues attributeValues;

	public FrankAttribute(String name, FrankElement attributeOwner) {
		super(attributeOwner);
		this.name = name;
		this.describingElement = attributeOwner;
	}

	@Override
	public Key getKey() {
		return new Key(name);
	}

	@Override
	boolean overrideIsMeaningful(ElementChild overriddenFrom) {
		return false;
	}

	@Override
	public int compareTo(FrankAttribute other) {
		return FRANK_ATTRIBUTE_COMPARATOR.compare(this, (FrankAttribute) other);
	}

	private static final Comparator<FrankAttribute> FRANK_ATTRIBUTE_COMPARATOR =
			Comparator.comparing(FrankAttribute::getOrder)
			.thenComparing(FrankAttribute::getName);
}
