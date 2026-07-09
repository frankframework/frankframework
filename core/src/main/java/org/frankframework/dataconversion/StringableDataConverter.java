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
import java.time.temporal.TemporalAccessor;
import java.util.Date;

abstract class StringableDataConverter<T> implements CharacterDataConversionSupport<T> {

	@Override
	public String asString(T data) {
		return data.toString();
	}

	@Override
	public Reader asReader(T data) {
		return new StringReader(asString(data));
	}

	@Override
	public long size(T data) {
		return asByteArray(data).length;
	}

	@Override
	public byte[] asByteArray(T data) {
		return asString(data).getBytes();
	}

	@Override
	public byte[] asByteArray(T data, String encodingCharset) throws IOException {
		return asString(data).getBytes(encodingCharset);
	}

	@Override
	public InputStream asInputStream(T data) {
		return new ByteArrayInputStream(asByteArray(data));
	}

	@Override
	public InputStream asInputStream(T data, String encodingCharset) throws IOException {
		return new ByteArrayInputStream(asByteArray(data, encodingCharset));
	}

	static class StringConverter extends StringableDataConverter<String> { }
	static class BooleanConverter extends StringableDataConverter<Boolean> { }
	static class NumberConverter extends StringableDataConverter<Number> { }
	static class DateConverter extends StringableDataConverter<Date> { }
	static class TemporalAccessorConverter extends StringableDataConverter<TemporalAccessor> { }
	static class EnumConverter extends StringableDataConverter<Enum<?>> {
		@Override
		public String asString(Enum<?> data) {
			return data.name();
		}
	}
}
