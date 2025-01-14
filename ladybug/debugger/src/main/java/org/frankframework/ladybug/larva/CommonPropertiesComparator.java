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

public class CommonPropertiesComparator implements Comparator<String> {

	@Override
	public int compare(String o1, String o2) {
		return Integer.compare(getRank(o1), getRank(o2));
	}

	private static int getRank(String propertyKey) {
		propertyKey = propertyKey.trim();
		String[] sParts = propertyKey.split("\\.");
		return switch (sParts[0]) {
			case "include" -> 0;
			case "adapter" -> 1;
			case "stub" -> 2;
			default -> {
				if (sParts[0].startsWith("ignoreContentBetweenKeys")) {
					String ignoreIdx = sParts[0].substring("ignoreContentBetweenKeys".length());
					if (ignoreIdx.isEmpty()) {
						yield 3;
					} else {
						yield 3 + 2 * Integer.parseInt(ignoreIdx) + Integer.parseInt(sParts[sParts.length - 1].substring(3));
					}
				} else {
					yield -1;
				}
			}
		};
	}
}
