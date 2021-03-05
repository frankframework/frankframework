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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;

/**
 * Holds a group of FrankElement objects for the Frank!Doc website.
 * This website will show a table-of-contents with the groups. When you
 * select a group, the FrankElement objects inside will be shown.
 * <p>
 * There are two kinds of groups that are represented by two subclasses of
 * {@link FrankDocGroup} that are also inner classes, namely
 * <code>FromType</code> and <code>Other</code>. <code>FromType</code>
 * holds a group that has the {@link FrankElement} objects that represent
 * the Java classes that implement a Java interface.
 * That Java interface is represented by a model object of class
 * {@link ElementType}. <code>Other</code> holds a group of remaining
 * {@link FrankElement}
 * that are each defined with a class rather then with an interface.
 * <p>
 * The Frank!Doc website has not been created yet.
 *
 * @author martijn
 *
 */
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

	public abstract List<FrankElement> getElements();
	public abstract String getCategory();
	public abstract boolean isFromElementType();

	private static class Other extends FrankDocGroup {
		private Map<String, FrankElement> elements;

		Other(String name, Collection<FrankElement> elements) {
			super(name);
			this.elements = new LinkedHashMap<>();
			for(FrankElement elem: elements) {
				this.elements.put(elem.getFullName(), elem);
			}
		}

		@Override
		public List<FrankElement> getElements() {
			return new ArrayList<>(elements.values());
		}

		@Override
		public boolean isFromElementType() {
			return false;
		}

		@Override
		public String getCategory() {
			return getName();
		}
	}

	private static class FromType extends FrankDocGroup {
		private ElementType elementType;

		FromType(ElementType elementType) {
			super(elementType.getGroupName());
			this.elementType = elementType;
		}

		@Override
		public List<FrankElement> getElements() {
			return new ArrayList<>(elementType.getSyntax2Members());
		}
		
		@Override
		public boolean isFromElementType() {
			return true;
		}

		@Override
		public String getCategory() {
			ElementType highestCommonInterface = elementType.getHighestCommonInterface();
			return highestCommonInterface.getGroupName();
		}
	}
}
