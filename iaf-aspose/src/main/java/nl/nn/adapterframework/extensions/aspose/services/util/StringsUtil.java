package nl.nn.adapterframework.extensions.aspose.services.util;

import com.google.common.base.CharMatcher;
import com.google.common.base.Strings;

public class StringsUtil {

	private static final String ABBREVIATION_POSTFIX = "...";

	private StringsUtil() {
	}

	/**
	 * Replaces org.apache.commons.lang.StringUtils.isBlank(String) with the guava
	 * library.
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isBlank(String str) {
		return Strings.isNullOrEmpty(trimToNull(str));
	}

	/**
	 * Replaces org.apache.commons.lang.StringUtils.isNotBlank(String) with the
	 * guava library.
	 * 
	 * @param str
	 * @return
	 */
	public static boolean isNotBlank(String str) {
		return !isBlank(str);
	}

	/**
	 * Replaces org.apache.commons.lang.StringUtils.trimToNull(String) with the
	 * guava library.
	 * 
	 * @param str
	 * @return
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
