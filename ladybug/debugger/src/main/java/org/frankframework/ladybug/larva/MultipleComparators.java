/*
   Copyright 2025 WeAreFrank!

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

public class MultipleComparators<T> implements Comparator<T> {

	private final Comparator<T> comparatorOne;
	private final Comparator<T> comparatorTwo;

	public MultipleComparators(Comparator<T> one, Comparator<T> another) {
		this.comparatorOne = one;
		this.comparatorTwo = another;
	}

	@Override
	public int compare(T one, T another) {
		int comparisonByOne = comparatorOne.compare(one, another);

		if (comparisonByOne == 0) {
			return comparatorTwo.compare(one, another);
		}
		return comparisonByOne;
		
	}
}
