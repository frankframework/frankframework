/*
   Copyright 2019 Integration Partners

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
package nl.nn.adapterframework.extensions.aspose.services.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

public class StringsUtil {

	private static final String ABBREVIATION_POSTFIX = "...";

	private StringsUtil() {
	}

	/**
	 * Replaces org.apache.commons.lang3.StringUtils.isBlank(String) with the guava library.
	 */
	public static boolean isBlank(String str) {
		return Strings.isNullOrEmpty(trimToNull(str));
	}

	/**
	 * Replaces org.apache.commons.lang3.StringUtils.isNotBlank(String) with the guava library.
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	/**
	 * Replaces org.apache.commons.lang3.StringUtils.trimToNull(String) with the guava library.
	 */
	public static String trimToNull(String str) {
		String result = null;
		if (str != null) {
			result = CharMatcher.WHITESPACE.trimFrom(str);
			result = result.isEmpty() ? null : result;
		}
		return result;
	}

	public static String abbreviate(String str, int maxLength) {
		if (maxLength <= ABBREVIATION_POSTFIX.length()) {
			throw new IllegalArgumentException("MaxLength should be larger than " + ABBREVIATION_POSTFIX.length());
		}
		if (str != null && str.length() > maxLength - ABBREVIATION_POSTFIX.length()) {
			return str.substring(0, maxLength - ABBREVIATION_POSTFIX.length()) + ABBREVIATION_POSTFIX;
		} else {
			return str;
		}
	}
}
