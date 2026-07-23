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
import java.io.Reader;

import org.xml.sax.InputSource;

import org.frankframework.stream.Message;
import org.frankframework.util.StreamUtil;

/**
 * Interface describing the strongly typed operations to represent binary data as various other types
 * that can be needed for operations in the Frank!Framework.
 * Concrete implementations should specify the concrete type of {@code <T>} upon which they operate.
 *
 * <p>
 *     Binary data is any data that is natively represented as bytes (in memory or on disk), and needs
 *     to be supplied with a character set in order to be represented as a {@link String} or {@link Reader}.
 *     The character set should then be passed in via the interface.
 * </p>
 *
 * @param <T> Type of the data that is operated on.
 */
interface TypedBinaryDataConverter<T> extends TypedConverter<T> {

	default String asString(T data, String encodingCharset) throws IOException {
		long size = size(data);
		return StreamUtil.readerToString(asReader(data, encodingCharset), null, false, size == Message.MESSAGE_SIZE_UNKNOWN ? 0 : 32 + (int) size);
	}

	default Reader asReader(T data, String encodingCharset) throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(asInputStream(data), encodingCharset);
	}

	default InputSource asInputSource(T data) throws IOException {
		return new InputSource(asInputStream(data));
	}

	default InputSource asInputSource(T data, String charset) throws IOException {
		return new InputSource(asReader(data, charset));
	}
}
