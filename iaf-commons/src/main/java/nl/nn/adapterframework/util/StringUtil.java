/*
   Copyright 2023 WeAreFrank!

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
package nl.nn.adapterframework.util;

import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;

public class StringUtil {

	private static final Pattern DEFAULT_SPLIT_PATTERN = Pattern.compile("\\s*,+\\s*");

	/**
	 * Concatenates two strings, if specified, uses the separator in between two strings.
	 * Does not use any separators if both or one of the strings are empty.
	 *<p>
	 *     Example:
	 *     <pre>
	 *         String a = "We";
	 *         String b = "Frank";
	 *         String separator = "Are";
	 *         String res = StringUtil.concatStrings(a, separator, b);
	 *         System.out.println(res); // prints "WeAreFrank"
	 *     </pre>
	 * </p>
	 * @param part1 First string
	 * @param separator Specified separator
	 * @param part2 Second string
	 * @return the concatenated string
	 */
	public static String concatStrings(String part1, String separator, String part2) {
		return concat(separator, part1, part2);
	}

	public static String concat(String separator, String... parts) {
		int i=0;
		while(i<parts.length && StringUtils.isEmpty(parts[i])) {
			i++;
		}
		if (i>=parts.length) {
			return null;
		}
		StringBuilder result= new StringBuilder(parts[i]);
		while(++i<parts.length) {
			if (StringUtils.isNotEmpty(parts[i])) {
				result.append(separator).append(parts[i]);
			}
		}
		return result.toString();
	}

	/**
	 * @see #hide(String)
	 * @return hidden string with all characters replaced with '*'
	 */
	public static String hide(String string) {
		return hide(string, 0);
	}

	/**
	 * Hides the string based on the mode given.
	 * Mode 1 hides starting from the second character of the string
	 * until, excluding, the last character.
	 *<p>
	 *     Example:
	 *     <pre>
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
	 * Hides the first half of the string.
	 * @see #hideAll(String, String, int)
	 */
	public static String hideFirstHalf(String inputString, String regex) {
		return hideAll(inputString, regex, 1);
	}

	/**
	 * Hide all characters matching the given Regular Expression.
	 * If the set of expressions is null or empty it will return the raw message.
	 * @see #hideAll(String, Collection, int)
	 */
	public static String hideAll(String message, Collection<String> collection) {
		return hideAll(message, collection, 0);
	}

	/**
	 * Hide all characters matching the given Regular Expression.
	 * If the set of expressions is null or empty it will return the raw message
	 * @see #hideAll(String, String, int)
	 */
	public static String hideAll(String message, Collection<String> collection, int mode) {
		if(collection == null || collection.isEmpty() || StringUtils.isEmpty(message))
			return message; //Nothing to do!

		for (String regex : collection) {
			if (StringUtils.isNotEmpty(regex))
				message = hideAll(message, regex, mode);
		}
		return message;
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
		StringBuilder result = new StringBuilder();
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(inputString);
		int previous = 0;
		while (matcher.find()) {
			result.append(inputString, previous, matcher.start());
			int len = matcher.end() - matcher.start();
			if (mode == 1) {
				int lenFirstHalf = (int) Math.ceil((double) len / 2);
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
		while (matcher.find())
			count++;
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
			for(Object o: collection) {
				if (sb.length() > 0) sb.append(", ");
				sb.append(o);
			}
		} catch (ConcurrentModificationException e) {
			sb.append(" ...more");
		}
		return sb.toString();
	}

	/**
	 * Splits a string into a list of substrings using default delimiter {@literal ','}.
	 * Spaces before or after separators, and any leading trailing spaces, are trimmed from the result.
	 *
	 * @param input the string to split, can be {@literal null}.
	 * @return a {@link List} of strings. An empty list if the input was {@literal null}.
	 */
	@Nonnull
	public static List<String> split(@Nullable String input) {
		return splitToStream(input)
				.collect(Collectors.toList());
	}

	public static List<String> splitAndTrim(@Nullable String input){
		return splitToStream(input)
		        .map(item::trim)
				.collect(Collectors.toList());
	}

	/**
	 * Splits a string into a stream of substrings using default delimiter {@literal ','}.
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
	 * @return a {@link List} of strings. An empty list if the input was {@literal null}.
	 */
	@Nonnull
	public static List<String> split(@Nullable String input, @Nonnull String delim) {
		return splitToStream(input, delim)
				.collect(Collectors.toList());
	}

	/**
	 * Splits a string into a stream of substrings using specified delimiters.
	 * Spaces before or after separators, and any leading trailing spaces, are trimmed from the result.
	 *
	 * @param input the string to split, can be {@literal null}.
	 * @param delim the delimiters to split the string by
	 * @return a Stream of strings. An empty stream if the input was {@literal null}.
	 */
	@Nonnull
	public static Stream<String> splitToStream(@Nullable final String input, @Nonnull final String delim) {
		if (input == null) {
			return Stream.empty();
		}
		Pattern splitPattern = Pattern.compile("\\s*[" + delim + "]+\\s*");
		return splitPattern.splitAsStream(input.trim())
				.filter(StringUtils::isNotBlank);
	}
}
