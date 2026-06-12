/*
   Copyright 2020 WeAreFrank!

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

import java.util.Comparator;

public abstract class AbstractNameComparator<T> implements Comparator<T> {

	protected static int getNextIndex(String s, int start, boolean numericPart) {
		int pos=start;
		while (pos<s.length() && !Character.isWhitespace(s.charAt(pos)) && numericPart == Character.isDigit(s.charAt(pos))) {
			pos++;
		}
		return pos;
	}

	protected static int skipWhitespace(String s, int start) {
		while (start<s.length() && Character.isWhitespace(s.charAt(start))) {
			start++;
		}
		return start;
	}

	public static int compareStringsNaturalOrder(String s0, String s1, boolean caseSensitive) {
		int result=0;
		int start_pos0=0;
		int start_pos1=0;
		int end_pos0=0;
		int end_pos1=0;
		boolean numeric=false;

		while(result ==0 && end_pos0<s0.length() && end_pos1<s1.length()) {
			start_pos0=skipWhitespace(s0, start_pos0);
			start_pos1=skipWhitespace(s1, start_pos1);
			end_pos0=getNextIndex(s0, start_pos0, numeric);
			end_pos1=getNextIndex(s1, start_pos1, numeric);
			String part0=s0.substring(start_pos0,end_pos0);
			String part1=s1.substring(start_pos1,end_pos1);
			if (numeric) {
				long lres;
				try {
					lres = Long.parseLong(part0)-Long.parseLong(part1);
				} catch (NumberFormatException e) {
					lres = part0.compareTo(part1);
				}
				if (lres!=0) {
					if (lres<0) {
						return -1;
					}
					return 1;
				}
			} else {
				if (caseSensitive) {
					result = part0.compareTo(part1);
				} else {
					result = part0.compareToIgnoreCase(part1);
				}
			}
			start_pos0=end_pos0;
			start_pos1=end_pos1;
			numeric=!numeric;
		}
		if (result!=0) {
			return result;
		}
		if (end_pos0<s0.length()) {
			return 1;
		}
		if (end_pos1<s1.length()) {
			return -1;
		}
		return 0;
	}

	public static int compareNames(String f0, String f1) {
		int result;

		result = compareStringsNaturalOrder(f0,f1,false);
		if (result!=0) {
			return result;
		}
		result = compareStringsNaturalOrder(f0,f1,true);
		if (result!=0) {
			return result;
		}
		result = f0.compareToIgnoreCase(f1);
		if (result!=0) {
			return result;
		}
		result = f0.compareTo(f1);
		if (result==0) {
			long lendif = f1.length()-f0.length();
			if (lendif > 0) {
				result=1;
			} else if (lendif < 0) {
				result=-1;
			}
		}
		return result;
	}
}
