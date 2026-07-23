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
import java.io.Serializable;
import java.util.Optional;

import javax.xml.transform.Source;

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import org.frankframework.functional.ThrowingSupplier;
import org.frankframework.util.StreamUtil;

/**
 * Implementation of {@link DataConverter} that wraps binary data of type {@code T}, and an implementation of {@link TypedBinaryDataConverter}
 * for the same type {@code T}.
 *
 * <p>
 *     The interface methods do not pass a character set, which is required by several methods in the wrapped {@link TypedBinaryDataConverter}. In
 *     order to determine the character set, the instance is created with a callback-function to retrieve character set information, the
 *     {@code charsetSupplier} of type {@code ThrowingSupplier<@Nullable String, IOException>}. The return value of the {@code charsetSupplier}
 *     may be {@code NULL} if no charset can be determined for the data.
 * </p>
 *
 * @param <T> Type of the binary data.
 */
final class BinaryDataConverter<T> extends AbstractDataConverter<T> implements DataConverter {
	private final TypedBinaryDataConverter<T> converter;
	private final ThrowingSupplier<@Nullable String, IOException> charsetSupplier;

	BinaryDataConverter(T data, TypedBinaryDataConverter<T> converter, ThrowingSupplier<@Nullable String, IOException> charsetSupplier) {
		super(data);
		this.converter = converter;
		this.charsetSupplier = charsetSupplier;
	}

	private @Nullable String getCharsetOrNull() throws IOException {
		return charsetSupplier.get();
	}

	private String getCharsetOrDefault() throws IOException {
		return Optional.ofNullable(charsetSupplier.get()).orElse(StreamUtil.DEFAULT_INPUT_STREAM_ENCODING);
	}

	@Override
	public boolean isBinary() {
		return true;
	}

	@Override
	public boolean prefersStreaming() {
		return converter.prefersStreaming();
	}

	@Override
	public Serializable asSerializable() throws IOException {
		return converter.asSerializable(data);
	}

	@Override
	public String asString() throws IOException {
		return converter.asString(data, getCharsetOrDefault());
	}

	@Override
	public Reader asReader() throws IOException {
		return converter.asReader(data, getCharsetOrDefault());
	}

	@Override
	public long size() {
		return converter.size(data);
	}

	@Override
	public boolean isEmpty() {
		return converter.isEmpty(data);
	}

	@Override
	public byte[] asByteArray() throws IOException {
		return converter.asByteArray(data);
	}

	@Override
	public InputStream asInputStream() throws IOException {
		return converter.asInputStream(data);
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
		Reader reader = converter.asReader(data, sourceCharset);
		return ReaderInputStream.builder().setReader(reader).setCharset(encodingCharset).get();
	}

	@Override
	public Source asSource() throws IOException, SAXException {
		return converter.asSource(data);
	}

	@Override
	public InputSource asInputSource() throws IOException {
		String charset = getCharsetOrNull();
		if (StringUtils.isEmpty(charset)) {
			return converter.asInputSource(data);
		}
		return converter.asInputSource(data, charset);
	}
}
