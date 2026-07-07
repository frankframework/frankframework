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
import java.io.Reader;

import org.jspecify.annotations.Nullable;

public final class TypedBinaryDataConverter<T> extends AbstractTypedDataConverter<T> implements BinaryDataConverter {
	private final BinaryDataConversionSupport<T> conversionSupport;


	public TypedBinaryDataConverter(T data, BinaryDataConversionSupport<T> conversionSupport) {
		super(data);
		this.conversionSupport = conversionSupport;
	}

	@Override
	public @Nullable String asString(String encodingCharset) throws IOException {
		return conversionSupport.asString(data, encodingCharset);
	}

	@Override
	public Reader asReader(String encodingCharset) throws IOException {
		return conversionSupport.asReader(data, encodingCharset);
	}

	@Override
	public long size() {
		return conversionSupport.size(data);
	}

	@Override
	public boolean isEmpty() {
		return conversionSupport.isEmpty(data);
	}

	@Override
	public byte @Nullable [] asByteArray() throws IOException {
		return conversionSupport.asByteArray(data);
	}

	@Override
	public InputStream asInputStream() throws IOException {
		return conversionSupport.asInputStream(data);
	}
}
