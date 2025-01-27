/*
   Copyright 2013 Nationale-Nederlanden, 2021-2024 WeAreFrank!

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
package org.frankframework.parameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

import org.frankframework.core.ParameterException;
import org.frankframework.core.PipeLineSession;
import org.frankframework.stream.Message;

/**
 * List of {@link ParameterValue ParameterValues}.
 *
 * @author Gerrit van Brakel
 */
public class ParameterValueList implements Iterable<ParameterValue> {

	private final List<ParameterValue> list;
	private final Map<String, ParameterValue> map;

	public ParameterValueList() {
		super();
		list = new ArrayList<>();
		map  = new LinkedHashMap<>();
	}

	public static ParameterValueList get(ParameterList params, Message message, PipeLineSession session) throws ParameterException {
		if (params == null) {
			return null;
		}
		return params.getValues(message, session);
	}

	protected void add(ParameterValue pv) {
		if (pv == null || pv.getDefinition() == null) {
			throw new IllegalStateException("No parameter defined");
		}
		if (StringUtils.isEmpty(pv.getDefinition().getName())) {
			throw new IllegalStateException("Parameter must have a name");
		}
		list.add(pv);
		map.put(pv.getDefinition().getName(), pv);
	}

	/** Get a specific {@link ParameterValue} */
	public ParameterValue get(String name) {
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

	public boolean contains(String name) {
		return map.containsKey(name);
	}

	/**
	 * should not be used in combination with {@link ParameterValueList#iterator()}!
	 */
	@Deprecated
	@Nullable
	public ParameterValue remove(String name) {
		ParameterValue pv = map.remove(name);
		if(pv != null) {
			list.remove(pv);
		}
		return pv;
	}

	@Nonnull
	private Map<String, ParameterValue> getParameterValueMap() {
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Returns a Map of value objects which may be a subset of the ParameterList when multiple parameters exist with the same name!
	 */
	@Nonnull
	public Map<String, Object> getValueMap() {
		Map<String, ParameterValue> paramValuesMap = getParameterValueMap();

		// convert map with parameterValue to map with value
		Map<String, Object> result = new LinkedHashMap<>(paramValuesMap.size());
		for (ParameterValue pv : paramValuesMap.values()) {
			result.put(pv.getDefinition().getName(), pv.getValue());
		}
		return result;
	}

	public static Message getValue(ParameterValueList pvl, String name, Message defaultValue) {
		if (pvl!=null) {
			ParameterValue pv = pvl.get(name);
			Message value = pv!=null ? pv.asMessage() : null;
			if (!Message.isNull(value)) {
				return value;
			}
		}
		return defaultValue;
	}

	public static String getValue(ParameterValueList pvl, String name, String defaultValue) {
		if (pvl != null) {
			ParameterValue pv = pvl.get(name);
			if (pv != null) {
				return pv.asStringValue(defaultValue);
			}
		}
		return defaultValue;
	}

	// >> List implementations, can differ in size from Map implementation when multiple ParameterValues with the same name exist!

	/**
	 * @return The list size, should only be used in combination with {@link ParameterValueList#iterator()}!
	 */
	public int size() {
		return list.size();
	}

	/**
	 * @return The corresponding {@link ParameterValue}, should only be used in combination with {@link ParameterValueList#iterator()}!
	 */
	public ParameterValue getParameterValue(int i) {
		return list.get(i);
	}

	/**
	 * Returns the {@code List} iterator which may contain {@link Parameter Parameters} with the same name!
	 */
	@Override
	@Nonnull
	public Iterator<ParameterValue> iterator() {
		return list.iterator();
	}

	public Stream<ParameterValue> stream() {
		return list.stream();
	}
}
