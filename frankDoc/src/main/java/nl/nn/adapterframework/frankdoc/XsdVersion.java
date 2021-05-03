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
package nl.nn.adapterframework.frankdoc;

import java.util.function.Predicate;

import lombok.Getter;
import nl.nn.adapterframework.frankdoc.DocWriterNewXmlUtils.AttributeUse;
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.FrankElement;

public enum XsdVersion {
	STRICT(ElementChild.IN_XSD, ElementChild.DEPRECATED, f -> ! f.isDeprecated(), new DelegateStrict()),
	COMPATIBILITY(ElementChild.IN_COMPATIBILITY_XSD, ElementChild.NONE, f -> true, new DelegateCompatibility());

	private final @Getter Predicate<ElementChild> childSelector;
	private final @Getter Predicate<ElementChild> childRejector;
	private final @Getter Predicate<FrankElement> elementFilter;
	private final Delegate delegate;

	private XsdVersion(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Predicate<FrankElement> elementFilter, Delegate delegate) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
		this.elementFilter = elementFilter;
		this.delegate = delegate;
	}

	AttributeUse getAttributeRoleNameUse() {
		return delegate.getAttributeRoleNameUse();
	}

	AttributeUse getClassNameAttributeUse(FrankElement frankElement) {
		return delegate.getClassNameAttributeUse(frankElement);
	}

	private abstract static class Delegate {
		abstract AttributeUse getAttributeRoleNameUse();
		abstract AttributeUse getClassNameAttributeUse(FrankElement frankElement);
	}

	private static class DelegateStrict extends Delegate {
		@Override
		AttributeUse getAttributeRoleNameUse() {
			return AttributeUse.PROHIBITED;
		}

		@Override
		AttributeUse getClassNameAttributeUse(FrankElement frankElement) {
			return AttributeUse.PROHIBITED;
		}
	}

	private static class DelegateCompatibility extends Delegate {
		/**
		 * Fix of https://github.com/ibissource/iaf/issues/1760. We need
		 * to omit the "use" attribute of the "elementRole" attribute in
		 * the compatibility XSD.
		 */
		@Override
		AttributeUse getAttributeRoleNameUse() {
			return AttributeUse.OPTIONAL;
		}

		/**
		 * Fix of https://github.com/ibissource/iaf/issues/1760. We need
		 * to omit the "use" attribute of the "className" attribute in
		 * the compatibility XSD, but only for interface-based FrankElement-s.
		 */
		@Override
		AttributeUse getClassNameAttributeUse(FrankElement frankElement) {
			if(frankElement.isInterfaceBased()) {
				return AttributeUse.OPTIONAL;	
			} else {
				return AttributeUse.PROHIBITED;
			}
		}
	}
}
