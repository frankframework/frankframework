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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.jspecify.annotations.Nullable;

import org.frankframework.util.StreamUtil;

public class ByteArrayConverter implements BinaryDataConversionSupport<byte[]> {
	@Override
	public @Nullable String asString(byte[] data, String encodingCharset) throws IOException {
		return new String(data, encodingCharset);
	}

	@Override
	public Reader asReader(byte[] data, String encodingCharset) throws IOException {
		return StreamUtil.getCharsetDetectingInputStreamReader(asInputStream(data), encodingCharset);
	}

	@Override
	public long size(byte[] data) {
		return data.length;
	}

	@Override
	public byte @Nullable [] asByteArray(byte[] data) throws IOException {
		return data;
	}

	@Override
	public InputStream asInputStream(byte[] data) throws IOException {
		return new ByteArrayInputStream(data);
	}
}
