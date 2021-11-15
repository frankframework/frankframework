/*
   Copyright 2013 Nationale-Nederlanden, 2021 WeAreFrank!

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
package nl.nn.adapterframework.parameters;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import nl.nn.adapterframework.core.ParameterException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.stream.Message;

/**
 * List of {@link ParameterValue ParameterValues}.
 * 
 * @author Gerrit van Brakel
 */
public class ParameterValueList implements Iterable<ParameterValue> {

	Map<String, ParameterValue> map;

	public ParameterValueList() {
		super();
		map  = new LinkedHashMap<>();
	}

	public static ParameterValueList get(ParameterList params, Message message, PipeLineSession session) throws ParameterException {
		if (params==null) {
			return null;
		}
		return params.getValues(message, session);
	}

	public void add(ParameterValue pv) {
		map.put(pv.getDefinition().getName(),pv);
	}

	@Deprecated
	public ParameterValue getParameterValue(int i) {
		int index = 0;
		for(ParameterValue pv : this) {
			if(i == index) {
				return pv;
			}
			index++;
		}
		return null;
	}

	/** Get a specific {@link ParameterValue} */
	public ParameterValue getParameterValue(String name) {
		return map.get(name);
	}

	/** Find a (case insensitive) {@link ParameterValue} */
	public ParameterValue findParameterValue(String name) {
		for(Map.Entry<String, ParameterValue> entry : map.entrySet()) {
			if(entry.getKey().equalsIgnoreCase(name)) {
				return entry.getValue();
			}
		}
		return null;
	}

	public Object getValue(String name) {
		ParameterValue pv = map.get(name);
		return pv == null ? null : pv.getValue();
	}

	public boolean contains(String name) {
		return map.containsKey(name);
	}


	public ParameterValue remove(String name) {
		return map.remove(name);
	}

	public int size() {
		return map.size();
	}

	private Map<String, ParameterValue> getParameterValueMap() {
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Returns a Map of value objects
	 */
	public Map<String,Object> getValueMap() {
		Map<String, ParameterValue> paramValuesMap = getParameterValueMap();

		// convert map with parameterValue to map with value
		Map<String,Object> result = new LinkedHashMap<>(paramValuesMap.size());
		for (ParameterValue pv : paramValuesMap.values()) {
			result.put(pv.getDefinition().getName(), pv.getValue());
		}
		return result;
	}

	@Override
	public Iterator<ParameterValue> iterator() {
		return map.values().iterator();
	}
}
