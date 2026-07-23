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

import javax.xml.transform.Source;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link DataConverter} that wraps character-based data of type {@code T}, and an implementation of {@link TypedCharacterDataConverter}
 * for the same type {@code T}.
 *
 * @param <T> Type of the character data.
 */
final class CharacterDataConverter<T> extends AbstractDataConverter<T> implements DataConverter {
	private final TypedCharacterDataConverter<T> converter;

	CharacterDataConverter(T data, TypedCharacterDataConverter<T> converter) {
		super(data);
		this.converter = converter;
	}

	@Override
	public boolean isBinary() {
		return false;
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
		return converter.asString(data);
	}

	@Override
	public Reader asReader() throws IOException {
		return converter.asReader(data);
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
		return converter.asInputStream(data, encodingCharset);
	}

	@Override
	public InputSource asInputSource() throws IOException {
		return converter.asInputSource(data);
	}

	@Override
	public Source asSource() throws IOException, SAXException {
		return converter.asSource(data);
	}

	@Override
	public long size() {
		return converter.size(data);
	}

	@Override
	public boolean isEmpty() {
		return converter.isEmpty(data);
	}
}
