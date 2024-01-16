/*
   Copyright 2021 WeAreFrank!

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

public class CaseInsensitiveComparator implements Comparator {

	@Override
	public int compare(Object object1, Object object2) {
		int result = 0;
		if (object1 instanceof String && object2 instanceof String) {
			String string1 = ((String)object1).toLowerCase();
			String string2 = ((String)object2).toLowerCase();
			result = string1.compareTo(string2);
		}
		return result;
	}

}
