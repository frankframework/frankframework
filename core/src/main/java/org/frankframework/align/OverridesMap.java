/*
   Copyright 2018 Nationale-Nederlanden, 2021, 2024 WeAreFrank!

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
package org.frankframework.align;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class OverridesMap<V> extends SubstitutionNode<V> implements SubstitutionProvider<V> {

	private final Map<String, Set<String>> allParents = new HashMap<>();

	@Override
	protected void registerSubstitute(String[] elements, int index, V value) {
		super.registerSubstitute(elements, index, value);
		for(int i=0;i<index;i++) {
			registerParent(i>0?elements[i-1]:null,elements[i]);
		}
	}

	protected void registerParent(String parent, String child) {
		if (parent==null) {
			// if parent is null, searching can stop after this child
			allParents.put(child, null);
			return;
		}
		Set<String> parentSetOfChild;
		boolean created=false;
		if (allParents.containsKey(child)) {
			parentSetOfChild = allParents.get(child);
			if (parentSetOfChild==null) {
				return; // child already registered with null parent;
			}
		} else {
			parentSetOfChild=new HashSet<>();
			allParents.put(child, parentSetOfChild);
			created=true;
		}
		if (created || !parentSetOfChild.contains(parent)) {
			parentSetOfChild.add(parent);
		}
	}

	@Override
	public boolean hasSubstitutionsFor(AlignmentContext context, String childName) {
		while (allParents.containsKey(childName)) {
			Set<String> parentSetOfChild = allParents.get(childName);
			if (parentSetOfChild==null) {
				return true;
			}
			if (context==null) {
				return false;
			}
			childName=context.getLocalName();
			context=context.getParent();
		}
		return false;
	}

	@Override
	public V getSubstitutionsFor(AlignmentContext context, String childName) {
		if (!hasSubstitutionsFor(context, childName)) {
			return null;
		}
		return getMatchingValue(context, childName);
	}

	@Override
	public boolean hasOverride(AlignmentContext context) {
		return getOverride(context)!=null;
	}

	@Override
	public V getOverride(AlignmentContext context) {
		return getMatchingValue(context.getParent(), context.getLocalName());
	}

	@Override
	public V getDefault(AlignmentContext context) {
		return null;
	}

	@Override
	public boolean isNotEmpty() {
		return !allParents.isEmpty();
	}
}
