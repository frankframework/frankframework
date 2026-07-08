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


import java.io.InputStream;
import java.time.temporal.TemporalAccessor;
import java.util.Date;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Node;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.stream.SerializableFileReference;

public class DataConverterFactory {
	public static final NullDataConverter nullDataConverter = new NullDataConverter();
	public static final StringableDataConverter.EnumConverter enumConverter = new StringableDataConverter.EnumConverter();
	public static final StringableDataConverter.BooleanConverter booleanConverter = new StringableDataConverter.BooleanConverter();
	public static final StringableDataConverter.NumberConverter numberConverter = new StringableDataConverter.NumberConverter();
	public static final StringableDataConverter.StringConverter stringConverter = new StringableDataConverter.StringConverter();
	public static final NodeConverter nodeConverter = new NodeConverter();
	public static final SerializableFileReferenceConverter serializableFileReferenceConverter = new SerializableFileReferenceConverter();
	public static final ThrowingSupplierConverter throwingSupplierConverter = new ThrowingSupplierConverter();
	public static final StringableDataConverter.DateConverter dateConverter = new StringableDataConverter.DateConverter();
	public static final StringableDataConverter.TemporalAccessorConverter temporalAccessorConverter = new StringableDataConverter.TemporalAccessorConverter();
	public static final ByteArrayConverter byteArrayConverter = new ByteArrayConverter();

	public static DataConverter getConverter(@Nullable Object data) {
		return switch (data) {
			case null -> new TypedBinaryDataConverter<>(NullMarker.NULL, nullDataConverter);
			case Enum<?> anEnum -> new TypedCharacterDataConverter<>(anEnum, enumConverter);
			case Boolean bool -> new TypedCharacterDataConverter<>(bool, booleanConverter);
			case Number number -> new TypedCharacterDataConverter<>(number, numberConverter);
			case String string -> new TypedCharacterDataConverter<>(string, stringConverter);
			case Node node -> new TypedCharacterDataConverter<>(node, nodeConverter);
			case byte[] bytes -> new TypedBinaryDataConverter<>(bytes, byteArrayConverter);
			case SerializableFileReference serializableFileReference -> new TypedBinaryDataConverter<>(serializableFileReference, serializableFileReferenceConverter);
			case ThrowingSupplier<?, ?> throwingSupplier -> //noinspection unchecked
					new TypedBinaryDataConverter<>((ThrowingSupplier<InputStream, Exception>) throwingSupplier, throwingSupplierConverter);
			case Date date -> new TypedCharacterDataConverter<>(date, dateConverter);
			case TemporalAccessor temporalAccessor -> new TypedCharacterDataConverter<>(temporalAccessor, temporalAccessorConverter);
			default -> throw new IllegalArgumentException("Unsupported data type: " + data.getClass().getName());
		};
	}
}
