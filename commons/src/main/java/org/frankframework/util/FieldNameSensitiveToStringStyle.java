/*
   Copyright 2024 WeAreFrank!

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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Similar to the {@link ToStringStyle#SHORT_PREFIX_STYLE} which excludes
 * fields containing the words 'password' and 'secret'.
 *
 * @author Niels Meijer
 */
class FieldNameSensitiveToStringStyle extends ToStringStyle {
	private static final List<String> PROTECTED_FIELDS = Arrays.asList("password", "secret", "token");

	private static final long serialVersionUID = 1L;

	public FieldNameSensitiveToStringStyle() {
		this.setUseShortClassName(true);
		this.setUseIdentityHashCode(false);
	}

	@Override
	protected void appendDetail(StringBuffer buffer, final String fieldName, final Object value) {
		if (containsHiddenWord(fieldName) && value instanceof String) {
			super.appendDetail(buffer, fieldName, hideValue(value));
		} else {
			super.appendDetail(buffer, fieldName, value);
		}
	}

	private boolean containsHiddenWord(final String name) {
		String lcName = name.toLowerCase();
		for (String field : PROTECTED_FIELDS) {
			if (lcName.contains(field)) {
				return true;
			}
		}
		return false;
	}

	private String hideValue(Object value) {
		if (value instanceof String string) {
			return StringUtil.hide(string);
		} else {
			return "***hidden***";
		}
	}

	/**
	 * Overriding Map implementation so we can also hide properties containing {@link #PROTECTED_FIELDS}.
	 */
	@Override
	protected void appendDetail(StringBuffer buffer, String fieldName, final Map<?, ?> map) {
		Map<Object, Object> hiddenValues = new HashMap<>(map); // Deep copy, we don't want to alter the original map!

		for (Object key : map.keySet()) {
			if (key instanceof String string && containsHiddenWord(string)) {
				Object hideMe = hiddenValues.remove(key);
				hiddenValues.put(key, hideValue(hideMe));
			}
		}

		super.appendDetail(buffer, fieldName, hiddenValues);
	}
}
