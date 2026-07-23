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


import java.io.IOException;
import java.io.InputStream;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.stream.SerializableFileReference;
import org.frankframework.util.ClassUtils;

public class DataConverterFactory {
	// Static singleton definitions of all converters
	static final NullDataConverter nullDataConverter = new NullDataConverter();
	static final StringableDataConverter.EnumConverter enumConverter = new StringableDataConverter.EnumConverter();
	static final StringableDataConverter.BooleanConverter booleanConverter = new StringableDataConverter.BooleanConverter();
	static final StringableDataConverter.NumberConverter numberConverter = new StringableDataConverter.NumberConverter();
	static final StringableDataConverter.StringConverter stringConverter = new StringableDataConverter.StringConverter();
	static final NodeConverter nodeConverter = new NodeConverter();
	static final SerializableFileReferenceConverter serializableFileReferenceConverter = new SerializableFileReferenceConverter();
	static final ThrowingSupplierConverter throwingSupplierConverter = new ThrowingSupplierConverter();
	static final StringableDataConverter.DateConverter dateConverter = new StringableDataConverter.DateConverter();
	static final StringableDataConverter.TemporalAccessorConverter temporalAccessorConverter = new StringableDataConverter.TemporalAccessorConverter();
	static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

	private DataConverterFactory() {
		// Private constructor to avoid instance creation
	}

	/**
	 * Factory method to create a {@literal DataConverter} to wrap the data that is passed
	 * @param data Data to be wrapped, or NULL.
	 * @param charsetSupplier Supplier of a character-set for the data. The supplier may return a NULL value if no charset can be detected. Only used for binary data.
	 * @return Instance implementing the {@literal DataConverter} interface
	 */
	public static DataConverter getConverter(@Nullable Object data, ThrowingSupplier<@Nullable String, IOException> charsetSupplier) {
		return switch (data) {
			case null -> nullDataConverter;
			case Enum<?> anEnum -> new CharacterDataConverter<>(anEnum, enumConverter);
			case Boolean bool -> new CharacterDataConverter<>(bool, booleanConverter);
			case Number number -> new CharacterDataConverter<>(number, numberConverter);
			case String string -> new CharacterDataConverter<>(string, stringConverter);
			case Node node -> new CharacterDataConverter<>(node, nodeConverter);
			case byte[] bytes -> new BinaryDataConverter<>(bytes, byteArrayConverter, charsetSupplier);
			case SerializableFileReference serializableFileReference -> new BinaryDataConverter<>(serializableFileReference, serializableFileReferenceConverter, charsetSupplier);
			case ThrowingSupplier<?, ?> throwingSupplier -> //noinspection unchecked
					new BinaryDataConverter<>((ThrowingSupplier<InputStream, Exception>) throwingSupplier, throwingSupplierConverter, charsetSupplier);
			case Date date -> new CharacterDataConverter<>(date, dateConverter);
			case TemporalAccessor temporalAccessor -> new CharacterDataConverter<>(temporalAccessor, temporalAccessorConverter);
			default -> throw new IllegalArgumentException("Unsupported data type: " + ClassUtils.classNameOf(data));
		};
	}
}
