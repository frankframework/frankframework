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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Cache<K,V> implements Map<K,V> {

	private Map<K,CacheEntry<V>> delegate;
	private int timeToLiveMillis;

	public Cache(int timeToLiveMillis) {
		this.timeToLiveMillis = timeToLiveMillis;
		delegate = new HashMap<>();
	}

	@Override
	public void clear() {
		delegate.clear();
	}

	@Override
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return delegate.containsValue(value);
	}

	@Override
	public Set<Entry<K, V>> entrySet() {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public V get(Object key) {
		CacheEntry<V> entry = delegate.get(key);
		return entry!=null && !entry.isExpired() ? entry.getValue() : null;
	}

	@Override
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	@Override
	public Set<K> keySet() {
		return delegate.keySet();
	}

	@Override
	public V put(K key, V value) {
		delegate.put(key, new CacheEntry<V>(value, timeToLiveMillis));
		return value;
	}

	@Override
	public void putAll(Map<? extends K, ? extends V> map) {
		throw new RuntimeException("NotImplemented");
	}

	@Override
	public V remove(Object key) {
		CacheEntry<V> entry = delegate.remove(key);
		return entry!=null ? entry.getValue() : null;
	}

	@Override
	public int size() {
		return delegate.size();
	}

	@Override
	public Collection<V> values() {
		throw new RuntimeException("NotImplemented");
	}

}
