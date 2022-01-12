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

package nl.nn.adapterframework.frankdoc;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import nl.nn.adapterframework.frankdoc.model.ElementType;
import nl.nn.adapterframework.frankdoc.model.FrankElement;

@EqualsAndHashCode
final class SortKeyForXsd {
	enum Kind {
		ELEMENT,
		TYPE;
	}

	private @Getter final Kind kind;
	private @Getter final String name;

	static SortKeyForXsd getInstance(ElementType type) {
		return new SortKeyForXsd(Kind.TYPE, type.getFullName());
	}

	static SortKeyForXsd getInstance(FrankElement element) {
		return new SortKeyForXsd(Kind.ELEMENT, element.getFullName());
	}

	private SortKeyForXsd(final Kind kind, final String name) {
		this.kind = kind;
		this.name = name;
	}
}
