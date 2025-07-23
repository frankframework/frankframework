/*
   Copyright 2023-2024 WeAreFrank!

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

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.logging.log4j.LogManager;

public class StringUtil {

	public static final ToStringStyle OMIT_PASSWORD_FIELDS_STYLE = new FieldNameSensitiveToStringStyle();
	public static final String DEFAULT_STRING_SPLIT_DELIMITER = ",";
	public static final String MATCH_OPTIONAL_WHITESPACE = "\\s*";
	private static final Pattern DEFAULT_SPLIT_PATTERN = Pattern.compile(MATCH_OPTIONAL_WHITESPACE + DEFAULT_STRING_SPLIT_DELIMITER + "+" + MATCH_OPTIONAL_WHITESPACE);

	/**
	 * Private constructor for utility class, for Sonar
	 */
	private StringUtil() {}

	/**
	 * Concatenates two strings, if specified, uses the separator in between two strings.
	 * Does not use any separators if both or one of the strings are empty.
	 * <p>
	 * Example:
	 * <pre>
	 *         String a = "We";
	 *         String b = "Frank";
	 *         String separator = "Are";
	 *         String res = StringUtil.concatStrings(a, separator, b);
	 *         System.out.println(res); // prints "WeAreFrank"
	 *     </pre>
	 * </p>
	 *
	 * @param part1     First string
	 * @param separator Specified separator
	 * @param part2     Second string
	 * @return the concatenated string
	 */
	public static String concatStrings(String part1, String separator, String part2) {
		return concat(separator, part1, part2);
	}

	public static String concat(String separator, String... parts) {
		int i = 0;
		while (i < parts.length && StringUtils.isEmpty(parts[i])) {
			i++;
		}
		if (i >= parts.length) {
			return null;
		}
		StringBuilder result = new StringBuilder(parts[i]);
		while (++i < parts.length) {
			if (StringUtils.isNotEmpty(parts[i])) {
				result.append(separator).append(parts[i]);
			}
		}
		return result.toString();
	}

	/**
	 * @return hidden string with all characters replaced with '*'
	 * @see #hide(String)
	 */
	public static String hide(String string) {
		return hide(string, 0);
	}

	/**
	 * Hides the string based on the mode given.
	 * Mode 1 hides starting from the second character of the string
	 * until, excluding, the last character.
	 * <p>
	 * Example:
	 * <pre>
	 *         String a = "test";
	 *         String res = StringUtil.hide(a, 1);
	 *         System.out.println(res) // prints "t**t"
	 *     </pre>
	 * </p>
	 */
	public static String hide(String string, int mode) {
		if (StringUtils.isEmpty(string)) {
			return string;
		}
		int len = string.length();
		if (mode != 1) {
			return StringUtils.repeat("*", len);
		}
		if (len <= 2) {
			return string;
		}
		char firstChar = string.charAt(0);
		char lastChar = string.charAt(len - 1);
		return firstChar + StringUtils.repeat("*", len - 2) + lastChar;
	}

	/**
	 * Hide all characters matching the given Regular Expression.
	 * If the set of expressions is null or empty it will return the raw message.
	 *
	 * @see #hideAll(String, Collection, int)
	 */
	public static String hideAll(String message, Collection<Pattern> collection) {
		return hideAll(message, collection, 0);
	}

	/**
	 * Hide all characters matching the given Regular Expression.
	 * If the set of expressions is null or empty it will return the raw message
	 *
	 * @see #hideAll(String, String, int)
	 */
	public static String hideAll(String message, Collection<Pattern> collection, int mode) {
		if (collection == null || collection.isEmpty() || StringUtils.isEmpty(message))
			return message; // Nothing to do!

		String result = message;
		for (Pattern regex : collection) {
			result = hideAll(result, regex, mode);
		}
		return result;
	}

	/**
	 * @see #hideAll(String, String, int)
	 */
	public static String hideAll(String inputString, String regex) {
		return hideAll(inputString, regex, 0);
	}

	/**
	 * Hides the input string according to the given regex and mode.
	 * If mode is set to 1, then the first half of the string gets hidden.
	 * Else, all of it.
	 */
	public static String hideAll(String inputString, String regex, int mode) {
		return hideAll(inputString, Pattern.compile(regex), mode);
	}

	/**
	 * Hides the input string according to the given regex and mode.
	 * If mode is set to 1, then the first half of the string gets hidden.
	 * Else, all of it.
	 */
	public static String hideAll(String inputString, Pattern regex, int mode) {
		StringBuilder result = new StringBuilder();
		Matcher matcher = regex.matcher(inputString);
		int previous = 0;
		while (matcher.find()) {
			result.append(inputString, previous, matcher.start());
			int len = matcher.end() - matcher.start();
			if (mode == 1) {
				int lenFirstHalf = (len + 1) >> 1;
				result.append(StringUtils.repeat("*", lenFirstHalf));
				result.append(inputString, matcher.start()
						+ lenFirstHalf, matcher.start() + len);
			} else {
				result.append(StringUtils.repeat("*", len));
			}
			previous = matcher.end();
		}
		result.append(inputString.substring(previous));
		return result.toString();
	}

	/**
	 * Counts the number of characters that the specified regexes will affect in the specified string.
	 * <pre>
	 *     String s = "12ab34";
	 *     String regex = "\\d";
	 *     int regexCount = StringUtil.countRegex(s, regex); // regexCount gives out 4
	 * </pre>
	 */
	public static int countRegex(String string, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(string);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	/**
	 * Turns the first Char into lower case.
	 */
	public static String lcFirst(String input) {
		char[] c = input.toCharArray();
		c[0] = Character.toLowerCase(c[0]);
		return new String(c);
	}

	/**
	 * Turns the first Char into upper case.
	 */
	public static String ucFirst(String input) {
		char[] c = input.toCharArray();
		c[0] = Character.toUpperCase(c[0]);
		return new String(c);
	}

	public static String safeCollectionToString(Collection<?> collection) {
		StringBuilder sb = new StringBuilder();
		try {
			for (Object o : collection) {
				if (!sb.isEmpty()) sb.append(", ");
				sb.append(o);
			}
		} catch (ConcurrentModificationException e) {
			sb.append(" ...more");
		}
		return sb.toString();
	}

	/**
	 * Splits a string into a list of substrings using default delimiter {@value DEFAULT_STRING_SPLIT_DELIMITER}.
	 * Spaces before or after separators, and any leading trailing spaces, are trimmed from the result.
	 *
	 * @param input the string to split, can be {@literal null}.
	 * @return a (modifiable) {@link List} of strings. An empty list if the input was {@literal null}.
	 */
	@Nonnull
	@SuppressWarnings("java:S6204") // Returns a modifiable list
	public static List<String> split(@Nullable String input) {
		return splitToStream(input)
				.collect(Collectors.toList());
	}

	/**
	 * Splits a string into a stream of substrings using default delimiter {@value DEFAULT_STRING_SPLIT_DELIMITER}.
	 * Spaces before or after separators, and any leading trailing spaces, are trimmed from the result.
	 *
	 * @param input the string to split, can be {@literal null}.
	 * @return a {@link Stream} of strings. An empty stream if the input was {@literal null}.
	 */
	@Nonnull
	public static Stream<String> splitToStream(@Nullable final String input) {
		if (input == null) {
			return Stream.empty();
		}
		return DEFAULT_SPLIT_PATTERN.splitAsStream(input.trim())
				.filter(StringUtils::isNotBlank);
	}

	/**
	 * Splits a string into an array of substrings using specified delimiters.
	 * Spaces before or after separators, and any leading trailing spaces, are trimmed from the result.
	 *
	 * @param input the string to split, can be {@literal null}.
	 * @param delim the delimiters to split the string by
	 * @return a (modifiable) {@link List} of strings. An empty list if the input was {@literal null}.
	 */
	@Nonnull
	@SuppressWarnings("java:S6204") // Returns a modifiable list
	public static List<String> split(@Nullable String input, @Nonnull String delim) {
		return splitToStream(input, delim)
				.collect(Collectors.toList());
	}

	/**
	 * Splits a string into a stream of substrings using specified delimiters.
	 * Spaces before or after separators, and any leading trailing spaces, are trimmed from the result.
	 *
	 * @param input the string to split, can be {@literal null}.
	 * @param delim the delimiters to split the string by. Each character in the string is a potential delimiter, so if you want to split strings by for instance a space, {@code ,} or {@code ;} then pass {@code " ,;"}.
	 * @return a Stream of strings. An empty stream if the input was {@literal null}.
	 */
	@Nonnull
	public static Stream<String> splitToStream(@Nullable final String input, @Nonnull final String delim) {
		if (DEFAULT_STRING_SPLIT_DELIMITER.equals(delim)) {
			// This version of the method uses a pre-compiled pattern, instead of compiling on every invocation.
			return splitToStream(input);
		}
		if (input == null) {
			return Stream.empty();
		}
		Pattern splitPattern = Pattern.compile(MATCH_OPTIONAL_WHITESPACE + "[" + delim + "]+" + MATCH_OPTIONAL_WHITESPACE);
		return splitPattern.splitAsStream(input.trim())
				.filter(StringUtils::isNotBlank);
	}

	/**
	 * toStrings and concatenates all fields of the given object, except fields containing the word 'password' or 'secret'.
	 * 'fail-safe' method, returns toString if it is unable to use reflection.
	 * Uses the {@link #OMIT_PASSWORD_FIELDS_STYLE OMIT_PASSWORD_FIELDS_STYLE}.
	 *
	 * @see org.apache.commons.lang3.builder.ToStringBuilder#reflectionToString
	 */
	@Nonnull
	public static String reflectionToString(@Nullable Object object) {
		if (object == null) {
			return "<null>";
		}

		try {
			return new ReflectionToStringBuilder(object, OMIT_PASSWORD_FIELDS_STYLE).toString();
		} catch (Exception e) { // amongst others, IllegalAccess-, ConcurrentModification- and Security-Exceptions
			LogManager.getLogger(object).warn("exception getting string representation of object", e);

			// In case this method is called from the objects toString method, we cannot call toString here!
			return "cannot get toString(): " + e.getMessage();
		}
	}
}
