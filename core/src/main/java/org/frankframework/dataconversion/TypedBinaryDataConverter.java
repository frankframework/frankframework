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
import java.util.Optional;

import javax.xml.transform.Source;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.util.StreamUtil;

public final class TypedBinaryDataConverter<T> extends AbstractTypedDataConverter<T> implements DataConverter {
	private final BinaryDataConversionSupport<T> conversionSupport;
	private final ThrowingSupplier<@Nullable String, IOException> charsetSupplier;

	public TypedBinaryDataConverter(T data, BinaryDataConversionSupport<T> conversionSupport, ThrowingSupplier<@Nullable String, IOException> charsetSupplier) {
		super(data);
		this.conversionSupport = conversionSupport;
		this.charsetSupplier = charsetSupplier;
	}

	@Override
	public boolean isBinary() {
		return true;
	}

	@Override
	public @Nullable String asString() throws IOException {
		return conversionSupport.asString(data, getCharsetOrDefault());
	}

	private @Nullable String getCharsetOrNull() throws IOException {
		return charsetSupplier.get();
	}

	private String getCharsetOrDefault() throws IOException {
		return Optional.ofNullable(charsetSupplier.get()).orElse(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}

	@Override
	public Reader asReader() throws IOException {
		return conversionSupport.asReader(data, getCharsetOrDefault());
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
	public byte @Nullable [] asByteArray(String encodingCharset) throws IOException {
		String sourceCharset = getCharsetOrNull();
		if (StringUtils.isEmpty(sourceCharset) || isEmpty()) {
			return asByteArray();
		}
		try (InputStream is = asInputStream(sourceCharset, encodingCharset)) {
			return is.readAllBytes();
		}
	}

	@Override
	public InputStream asInputStream() throws IOException {
		return conversionSupport.asInputStream(data);
	}

	@Override
	public InputStream asInputStream(String encodingCharset) throws IOException {
		String sourceCharset = getCharsetOrNull();
		if (StringUtils.isEmpty(sourceCharset) || isEmpty()) {
			return asInputStream();
		}
		return asInputStream(encodingCharset, sourceCharset);
	}

	private ReaderInputStream asInputStream(String encodingCharset, String sourceCharset) throws IOException {
		Reader reader = conversionSupport.asReader(data, sourceCharset);
		return ReaderInputStream.builder().setReader(reader).setCharset(encodingCharset).get();
	}

	@Override
	public @Nullable Source asSource() throws IOException, SAXException {
		return conversionSupport.asSource(data);
	}

	@Override
	public @Nullable InputSource asInputSource() throws IOException {
		String charset = getCharsetOrNull();
		if (StringUtils.isEmpty(charset)) {
			return conversionSupport.asInputSource(data);
		}
		return conversionSupport.asInputSource(data, charset);
	}
}
