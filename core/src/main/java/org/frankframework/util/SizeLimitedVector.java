/*
   Copyright 2013 Nationale-Nederlanden, 2022 WeAreFrank!

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

import lombok.Getter;

/**
 * Stores a maximum number of elements in a Vector. If, after the maximum has
 * exceeded, another element is put in the vector the oldest element is removed.
 *
 * @author Johan Verrips
 */
public class SizeLimitedVector<E> extends java.util.Vector<E> {

	private @Getter int maxSize;

	/**
	 * SizeLimitedVector constructor comment.
	 */
	public SizeLimitedVector() {
		this(Integer.MAX_VALUE);
	}

	public SizeLimitedVector(int maxSize) {
		super();
		this.maxSize = maxSize;
	}

	@Override
	public synchronized boolean add(E o) {
		super.add(o);
		if(super.size() > maxSize) {
			super.removeElementAt(0);
		}
		return true;
	}

	/**
	 * sets the Maximum Size to maxSize. If the current size
	 * is greater than the maximum size, the top elements are removed.
	 */
	public void setMaxSize(int maxSize) {
		this.maxSize = maxSize;
		if(this.size() > 0) {
			while(size() > maxSize) removeElementAt(0);
		}
	}
}
