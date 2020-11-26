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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

public abstract class FrankDocGroup {
	private @Getter String name;

	public static FrankDocGroup getInstanceFromFrankElements(String groupName, Collection<FrankElement> frankElements) {
		return new Other(groupName, frankElements);
	}

	public static FrankDocGroup getInstanceFromElementType(ElementType elementType) {
		return new FromType(elementType);
	}

	FrankDocGroup(String name) {
		this.name = name;
	}

	public abstract boolean hasElement(String elementFullName);
	public abstract List<FrankElement> getElements();
	public abstract boolean isFromElementType();

	private static class Other extends FrankDocGroup {
		private Map<String, FrankElement> elements;

		Other(String name, Collection<FrankElement> elements) {
			super(name);
			this.elements = new HashMap<>();
			for(FrankElement elem: elements) {
				this.elements.put(elem.getFullName(), elem);
			}
		}

		@Override
		public boolean hasElement(String elementFullName) {
			return elements.containsKey(elementFullName);
		}

		@Override
		public List<FrankElement> getElements() {
			return new ArrayList<>(elements.values());
		}

		@Override
		public boolean isFromElementType() {
			return false;
		}
	}

	private static class FromType extends FrankDocGroup {
		private ElementType elementType;

		FromType(ElementType elementType) {
			super(elementType.getSimpleName());
			this.elementType = elementType;
		}

		@Override
		public boolean hasElement(String elementFullName) {
			return elementType.getMembers().containsKey(elementFullName);
		}

		@Override
		public List<FrankElement> getElements() {
			return new ArrayList<>(elementType.getMembers().values());
		}
		
		@Override
		public boolean isFromElementType() {
			return true;
		}
	}
}
