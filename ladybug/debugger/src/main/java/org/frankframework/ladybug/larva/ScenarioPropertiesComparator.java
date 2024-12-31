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
package org.frankframework.ladybug.larva;

import java.util.Comparator;

public class ScenarioPropertiesComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		return Integer.compare(getRank(o1), getRank(o2));
	}

	private static int getRank(String s) {
		s = s.trim();
		String[] sParts = s.split("\\.");
		switch (sParts[0]) {
			case "scenario":
				return 0;
			case "include":
				return 1;
			default:
				if (sParts[0].startsWith("step")) {
					return 3 + Integer.parseInt(sParts[0].substring(4));
				}
		}
		//substring from second dot (or first if there is only 1);
		int j = s.indexOf(".");
		String s1 = s.substring(Math.max(s.indexOf(".", j + 1) + 1, j + 1));
		if (s1.matches("^param\\d+\\.(name|value)$")) {
			return 2;
		}
		return -1;
	}
}
