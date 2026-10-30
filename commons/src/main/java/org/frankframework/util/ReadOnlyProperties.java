/*
   Copyright 2026 WeAreFrank!

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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jspecify.annotations.NonNull;

public class ReadOnlyProperties extends Properties {

	public ReadOnlyProperties(Properties delegate) {
		super(delegate);
	}

	@Override
	public synchronized void putAll(Map<?, ?> t) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized void load(Reader reader) throws IOException {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized void load(InputStream inStream) throws IOException {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object putIfAbsent(Object key, Object value) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized boolean replace(Object key, Object oldValue, Object newValue) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object replace(Object key, Object value) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized void clear() {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object put(Object key, Object value) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object remove(Object key) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public synchronized Object setProperty(String key, String value) {
		throw new UnsupportedOperationException("ReadOnlyProperties is read-only");
	}

	@Override
	public @NonNull Collection<Object> values() {
		return Collections.unmodifiableCollection(super.values());
	}

	@Override
	public @NonNull Set<Object> keySet() {
		return Collections.unmodifiableSet(super.keySet());
	}

	@Override
	public @NonNull Set<Map.Entry<Object, Object>> entrySet() {
		return Collections.unmodifiableSet(super.entrySet());
	}
}
