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

import org.xml.sax.InputSource;

/**
 * Interface describing the strongly typed operations to represent character-based data as various other types
 * that can be needed for operations in the Frank!Framework.
 * Concrete implementations should specify the concrete type of {@code <T>} upon which they operate.
 *
 * <p>
 *     Character data is any data that has a native {@code toString()} representation and does not need to
 *     have an additional character set specified in order to represent it as a {@link String} or {@link Reader}.
 * </p>
 *
 * @param <T> Type of the data that is operated on.
 */
interface TypedCharacterDataConverter<T> extends TypedConverter<T> {

	String asString(T data) throws IOException;

	Reader asReader(T data) throws IOException;

	InputStream asInputStream(T data, String encodingCharset) throws IOException;

	default InputSource asInputSource(T data) throws IOException {
		return new InputSource(asReader(data));
	}
}
