/*
   Copyright 2018 Nationale-Nederlanden

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
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import org.frankframework.util.LogUtil;

public class SubstitutionNode<V> {
	protected Logger log = LogUtil.getLogger(this.getClass());

	private Map<String,SubstitutionNode<V>> parents;
	private V value;

	public void registerSubstitutes(Map<String,V> substitutes) {
		for(Entry<String,V> entry:substitutes.entrySet()) {
			registerSubstitute(entry.getKey(), entry.getValue());
		}
	}

	public void registerSubstitute(String path, V value) {
		if (log.isDebugEnabled()) log.debug("register override [{}]=[{}]", path, value);
		String[] elements = path.split("/");
		registerSubstitute(elements, elements.length, value);
	}

	protected void registerSubstitute(String[] elements, int index, V value) {
		if (index==0) {
			this.value=value;
		} else {
			String key = elements[--index];
			SubstitutionNode<V> parent=null;
			if (parents==null) {
				parents=new HashMap<>();
			} else {
				parent = parents.get(key);
			}
			if (parent==null) {
				parent=new SubstitutionNode<>();
				parents.put(key, parent);
			}
			parent.registerSubstitute(elements, index, value);
		}
	}

	public V getMatchingValue(AlignmentContext context, String elementName) {
		if (parents==null || !parents.containsKey(elementName)) {
			return value;
		}
		SubstitutionNode<V> parent = parents.get(elementName);
		if (context==null) {
			return parent.getMatchingValue(null, null);
		}
		return parent.getMatchingValue(context.getParent(), context.getLocalName());
	}
}
