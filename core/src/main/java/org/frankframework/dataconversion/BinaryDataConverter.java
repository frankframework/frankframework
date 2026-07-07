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

import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.lang3.StringUtils;

import org.jspecify.annotations.Nullable;
import org.xml.sax.InputSource;

public sealed interface BinaryDataConverter extends DataConverter permits TypedBinaryDataConverter {
	default boolean isBinary() {
		return true;
	}

	@Nullable String asString(String encodingCharset) throws IOException;

	Reader asReader(String encodingCharset) throws IOException;

	@Nullable InputSource asInputSource(String charset) throws IOException;

	default InputStream asInputStream(@Nullable String sourceCharset, String encodingCharset) throws IOException {
		if (StringUtils.isEmpty(sourceCharset) || isEmpty()) {
			return asInputStream();
		}
		Reader reader = asReader(sourceCharset);
		return ReaderInputStream.builder().setReader(reader).setCharset(encodingCharset).get();
	}

	default byte @Nullable [] asByteArray(@Nullable String sourceCharset, String encodingCharset) throws IOException {
		if (StringUtils.isEmpty(sourceCharset) || isEmpty()) {
			return asByteArray();
		}
		try (InputStream is = asInputStream(sourceCharset, encodingCharset)) {
			return is.readAllBytes();
		}
	}
}
