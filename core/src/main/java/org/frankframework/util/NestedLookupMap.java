/*
   Copyright 2026 WeAreFrank!

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
package org.frankframework.util;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Map implementation that allows looking up dotted keys in nested sub-maps. Only the master-map needs to be of type {@code NestedLookupMap},
 * sub-maps can be of any type.
 * Key lookup in the master map is also case-insensitive. If sub-maps are case-insensitive as well, then those parts can also be retrieved
 * case-insensitive; however that depends on the actual submap-implementation type.
 * Map keys are always Strings.
 *
 * @param <V> Type of the values in the map.
 */
public class NestedLookupMap<V> extends LinkedCaseInsensitiveMap<V> {
	/**
	 * Overridden `get` method to supported dot-separated keys to get values from nested sub-maps.
	 * @param key the key whose associated value is to be returned. A {@code NULL} key always returns value NULL.
	 * @return Value from (nested) map, or {@code NULL}.
	 */
	@Override
	public @Nullable V get(@Nullable Object key) {
		if (!(key instanceof String strKey)) return null;
		if (!strKey.contains(".") || containsKey(key)) {
			return super.get(key);
		}
		Map<?,?> subMap = this;
		Object value = null;
		for (String part : strKey.split("\\.")) {
			if (subMap == null) {
				// We cannot find the sub-key, or sub-key was not instance of Map, before we exhausted all parts
				return null;
			}
			value = subMap.get(part);
			if (!(value instanceof Map<?,?> newSubMap)) {
				// Do not directly return NULL because we don't know if we're on the last subKey, which can be any type
				subMap = null;
				continue;
			}
			subMap = newSubMap;
		}
		//noinspection ReassignedVariable,unchecked
		return (V)value;
	}
}
