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

import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Container class that allows retrieving various representations of its contents.
 *
 * <p>
 *     If the underlying data is binary, and a character representation is required, then the container implementation should
 *     have the means to determine the character set of the underlying data.
 *     The character set is not passed in to any of the interface methods.
 * </p>
 */
public interface DataConverter {
	/**
	 * Check if the underlying data is binary, or natively character-based. Not related to whether a character set can be derived
	 * for the data.
	 */
	boolean isBinary();

	/**
	 * Check if the data in the container is empty, such as a String of length 0. This is not the same as {@code NULL} data, but
	 * {@code NULL} is always empty.
	 */
	boolean isEmpty();

	/**
	 * Check if the container contains {@code NULL} data or not.
	 */
	default boolean isNull()  {
		return false;
	}

	/**
	 * Check if the data prefers to be represented in a Streaming format ({@link InputStream} or {@link Reader}. Generally, this
	 * implies the data is not readily available in-memory.
	 */
	boolean prefersStreaming();

	/**
	 * Size of (the binary representation of) the data, if possible to determine. When {@link #isEmpty()} or {@link #isNull()}, this method returns {@code 0}. When
	 * the size of the data cannot be determined, will return {@link org.frankframework.stream.Message#MESSAGE_SIZE_UNKNOWN}.
	 */
	long size();

	/**
	 * Returns the raw object inside this container, or {@code NULL} if {@link #isNull()}.
	 */
	@Nullable Object asRawObject();

	/**
	 * Returns a {@link Serializable} representation of the data, or {@code NULL} if {@link #isNull()}. This may be the same
	 * instance as the original data, if it already implements {@link Serializable} natively.
	 */
	@Nullable Serializable asSerializable() throws IOException;

	/**
	 * Returns a {@link String} representing the data in this container, or {@code NULL} if {@link #isNull()}.
	 */
	@Nullable String asString() throws IOException;

	/**
	 * Returns a {@code byte[]} representing the data in this container, or {@code NULL} if {@link #isNull()}.
	 */
	byte @Nullable [] asByteArray() throws IOException;

	/**
	 * Return data as a {@link Reader}. The {@code Reader} is guaranteed to support {@link Reader#mark(int)} and
	 * {@link Reader#reset()}.
	 *
	 * @return A {@link Reader} backed by the represented data. The Reader might not have any data. Will never return {@code NULL}.
	 * @throws IOException When any error occurs.
	 */
	Reader asReader() throws IOException;

	/**
	 * Return data as an {@link InputStream}. The {@code InputStream} is guaranteed to support {@link InputStream#mark(int)}
	 * and {@link InputStream#reset()}. If the underlying data is character-based, the encoding of the stream is the
	 * {@link org.frankframework.util.StreamUtil#DEFAULT_INPUT_STREAM_ENCODING}.
	 *
	 * @return An {@link InputStream} backed by the represented data. The InputStream might not have any data. Will never return {@code NULL}.
	 * @throws IOException When any error occurs.
	 */
	InputStream asInputStream() throws IOException;

	/**
	 * Return data as an {@link InputStream}, encoded in the specified {@code encodingCharset}. The {@code InputStream}
	 * is guaranteed to support {@link InputStream#mark(int)} and {@link InputStream#reset()}.
	 *
	 * @param encodingCharset The charset into which to encode the data on the {@link InputStream}.
	 * @return An {@link InputStream} backed by the represented data, (re-)encoded in the specified {@code encodingCharset}. The InputStream might not have any data. Will never return {@code NULL}.
	 * @throws IOException When there was an error.
	 */
	InputStream asInputStream(String encodingCharset) throws IOException;

	/**
	 * Return the underlying data as an XML {@link InputSource}.
	 */
	@Nullable InputSource asInputSource() throws IOException;

	/**
	 * Return the underlying data as an XML {@link InputSource}.
	 */
	@Nullable Source asSource() throws IOException, SAXException;
}
