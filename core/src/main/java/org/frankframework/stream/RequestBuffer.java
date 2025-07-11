/*
   Copyright 2025 WeAreFrank!

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
package org.frankframework.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.nio.charset.Charset;

import jakarta.annotation.Nonnull;

/**
 * An interface that provides a uniform and repeatable way to access a request object.
 * <p>
 *     Implementations should allow request objects to be repeatably accessed in requested form and optionally provide
 *     extra information on the request object, such as size.
 * </p>
 */
public interface RequestBuffer extends AutoCloseable {

	/**
	 * Provide access to the underlying request data as InputStream. If the underlying data is not binary,
	 * use {@code UTF-8} to encode it to binary representation.
	 */
	@Nonnull InputStream asInputStream() throws IOException;

	/**
	 * Provide access to the underlying request data as InputStream.
	 * If the underlying data is binary, the provided {@code encodingCharset} may be ignored, otherwise
	 * if the underlying data is character data then use the {@code encodingCharset} to make the binary encoding
	 * of that character data.
	 */
	@Nonnull InputStream asInputStream(@Nonnull Charset encodingCharset) throws IOException;

	/**
	 * Provide access to the underlying request data as Reader.
	 * If the underlying data is binary, assume it is encoded in {@code UTF-8}.
	 */
	@Nonnull Reader asReader() throws IOException;

	/**
	 * Provide access to the underlying request data as Reader.
	 * If the underlying data is binary, use the provided {@code decodingCharset} to decode to characters.
	 * If the underlying data is character data then the {@code decodingCharset} may be ignored.
	 */
	@Nonnull Reader asReader(@Nonnull Charset decodingCharset) throws IOException;

	/**
	 * Convert all data to a serializable format and return this.
	 */
	@Nonnull Serializable asSerializable() throws IOException;

	/**
	 * Return the size of the data, if known. If the full source request has not yet been accessed, return
	 * {@value Message#MESSAGE_SIZE_UNKNOWN}.
	 */
	long size();

	/**
	 * Check if the request contains any data or not. Will try to access the data to see if any is available.
	 */
	boolean isEmpty() throws IOException;

	/**
	 * Check if the underlying data format is binary or character data.
	 */
	boolean isBinary();

	@Override
	void close() throws IOException;
}
