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
import nl.nn.adapterframework.frankdoc.model.ElementChild;
import nl.nn.adapterframework.frankdoc.model.FrankElement;

public enum XsdVersion {
	STRICT(ElementChild.IN_XSD, ElementChild.DEPRECATED, f -> ! f.isDeprecated()),
	COMPATIBILITY(ElementChild.IN_COMPATIBILITY_XSD, ElementChild.NONE, f -> true);

	private final @Getter Predicate<ElementChild> childSelector;
	private final @Getter Predicate<ElementChild> childRejector;
	private final @Getter Predicate<FrankElement> elementFilter;

	private XsdVersion(Predicate<ElementChild> childSelector, Predicate<ElementChild> childRejector, Predicate<FrankElement> elementFilter) {
		this.childSelector = childSelector;
		this.childRejector = childRejector;
		this.elementFilter = elementFilter;
	}
}
