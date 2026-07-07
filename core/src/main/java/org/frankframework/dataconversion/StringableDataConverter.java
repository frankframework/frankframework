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
package org.frankframework.dataconversion;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

public abstract class StringableDataConverter<T> implements CharacterDataConversionSupport<T> {

	@Override
	public String asString(T data) {
		return data.toString();
	}

	@Override
	public Reader asReader(T data) throws IOException {
		return new StringReader(asString(data));
	}

	@Override
	public long size(T data) {
		return asString(data).getBytes(StandardCharsets.UTF_8).length;
	}

	@Override
	public byte [] asByteArray(T data) throws IOException {
		return asString(data).getBytes();
	}

	@Override
	public byte [] asByteArray(T data, String encodingCharset) throws IOException {
		return asString(data).getBytes(encodingCharset);
	}

	@Override
	public InputStream asInputStream(T data) throws IOException {
		return new ByteArrayInputStream(asByteArray(data));
	}

	@Override
	public InputStream asInputStream(T data, String encodingCharset) throws IOException {
		return new ByteArrayInputStream(asByteArray(data, encodingCharset));
	}

	public static class EnumConverter extends StringableDataConverter<Enum<?>> {
		@Override
		public String asString(Enum<?> data) {
			return data.name();
		}
	}

	public static class StringConverter extends StringableDataConverter<String> { }
	public static class BooleanConverter extends StringableDataConverter<Boolean> { }
	public static class NumberConverter extends StringableDataConverter<Number> { }
}
