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
package nl.nn.credentialprovider.util;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	 *         String res = Misc.replace(a, "WeAre", "IAm");
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
	 *         String res = Misc.concatStrings(a, separator, b);
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
		while(i<parts.length && isEmpty(parts[i])) {
			i++;
		}
		if (i>=parts.length) {
			return null;
		}
		String result=parts[i];
		while(++i<parts.length) {
			if (isNotEmpty(parts[i])) {
				result += separator + parts[i];
			}
		}
		return result;
	}

	/**
	 * Converts the list to a string.
	 * <pre>
	 *      List list = new ArrayList<Integer>();
	 *      list.add("We Are");
	 *      list.add(" Frank");
	 *      String res = Misc.listToString(list); // res gives out "We Are Frank"
	 * </pre>
	 */
	public static String listToString(List list) {
		StringBuilder sb = new StringBuilder();
		for (Iterator it = list.iterator(); it.hasNext();) {
			sb.append((String) it.next());
		}
		return sb.toString();
	}

	/**
	 * Counts the number of characters that the specified reges will affect in the specified string.
	 * <pre>
	 *     String s = "12ab34";
	 *     String regex = "\\d";
	 *     int regexCount = Misc.countRegex(s, regex); // regexCount gives out 4
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

	public static boolean isEmpty(String string) {
		return string==null || string.isEmpty();
	}

	public static boolean isNotEmpty(String string) {
		return string!=null && !string.isEmpty();
	}
}
