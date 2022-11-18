/*
   Copyright 2022 WeAreFrank!

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

public class CacheEntry<V> {

	private V value;
	private long expiry;

	public CacheEntry() {
		expiry = 0L; // make sure it is expired;
	}

	public CacheEntry(V value, int timeToLiveMillis) {
		this.value = value;
		expiry = System.currentTimeMillis()+timeToLiveMillis;
	}

	public boolean isExpired() {
		return System.currentTimeMillis() > expiry;
	}

	public V getValue() {
		return value;
	}

	public void update(V value, int timeToLiveMillis) {
		this.value = value;
		expiry = System.currentTimeMillis()+timeToLiveMillis;
	}
}
