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

public interface DataConverter {
	boolean isBinary();
	boolean isEmpty();
	default boolean isNull()  {
		return false;
	}
	boolean prefersStreaming();
	long size();

	@Nullable Object asRawObject();

	@Nullable Serializable asSerializable() throws IOException;

	@Nullable String asString() throws IOException;

	byte @Nullable [] asByteArray() throws IOException;

	Reader asReader() throws IOException;

	InputStream asInputStream() throws IOException;

	InputStream asInputStream(String encodingCharset) throws IOException;

	@Nullable InputSource asInputSource() throws IOException;

	@Nullable Source asSource() throws IOException, SAXException;
}
