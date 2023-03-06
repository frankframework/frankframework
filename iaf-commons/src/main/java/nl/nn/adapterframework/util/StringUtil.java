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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class StringUtil {
	/**
	 * String replacer.
	 *
	 * @deprecated Use instead {@link String#replace(CharSequence, CharSequence)}.
	 *
	 * <p>
	 *     Example:
	 *     <pre>
	 *         String a = "WeAreFrank";
	 *         String res = StringUtil.replace(a, "WeAre", "IAm");
	 *         System.out.println(res); // prints "IAmFrank"
	 *     </pre>
	 * </p>
	 * @param source	is the original string
	 * @param from		is the string to be replaced
	 * @param to		is the string which will used to replace
	 */
	@Deprecated
	public static String replace (String source, String from, String to) {
		int start = source.indexOf(from);
		if (start==-1) {
			return source;
		}
		int fromLength = from.length();
		char [] sourceArray = source.toCharArray();

		StringBuilder buffer = new StringBuilder();
		int srcPos=0;

		while (start != -1) {
			buffer.append (sourceArray, srcPos, start-srcPos);
			buffer.append (to);
			srcPos=start+fromLength;
			start = source.indexOf (from, srcPos);
		}
		buffer.append (sourceArray, srcPos, sourceArray.length-srcPos);
		return buffer.toString();
	}

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
	 * Replaces low line (x'5f') by asterisk (x'2a) so it's sorted before any digit and letter
	 * <pre>
	 *      StringUtil.toSortName("new_name"); // gives out "NEW*NAME"
	 * </pre>
	 */
	public static String toSortName(String name) {
		// replace low line (x'5f') by asterisk (x'2a) so it's sorted before any digit and letter
		return StringUtils.upperCase(StringUtils.replace(name,"_", "*"));
	}

	/**
	 * Counts the number of characters that the specified reges will affect in the specified string.
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
}
